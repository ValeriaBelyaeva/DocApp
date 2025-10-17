package com.example.docapp.core

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.docapp.data.AppDb
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import android.database.sqlite.SQLiteDatabase
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(val context: Context) {
    
    companion object {
        private const val KEYSTORE_ALIAS = "DocAppMasterKey"
        private const val ENCRYPTED_PREFS_NAME = "secure_prefs"
        private const val DB_KEY_PREF = "db_key_b64"
        private const val DB_SALT_PREF = "db_salt_b64"
        private const val PBKDF2_ITERATIONS = 150_000
        private const val KEY_LENGTH = 256 // бит
        private const val SALT_LENGTH = 32 // байт
    }

    private val masterKey: MasterKey by lazy {
        try {
            AppLogger.log("CryptoManager", "Creating MasterKey...")
            ErrorHandler.showInfo("CryptoManager: Создаем MasterKey...")
            
            // Проверяем доступность Android Keystore
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            ErrorHandler.showSuccess("CryptoManager: MasterKey создан успешно")
            masterKey
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to create MasterKey: ${e.message}")
            ErrorHandler.showCriticalError("Не удалось создать мастер-ключ: ${e.message}", e)
            throw e
        }
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            AppLogger.log("CryptoManager", "Creating EncryptedSharedPreferences...")
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to create EncryptedSharedPreferences: ${e.message}")
            AppLogger.log("CryptoManager", "ERROR: Exception type: ${e.javaClass.simpleName}")
            AppLogger.log("CryptoManager", "ERROR: Stack trace: ${e.stackTraceToString()}")
            
            val errorMessage = when (e) {
                is RuntimeException -> "Критическая ошибка создания зашифрованных настроек: ${e.message}"
                is SecurityException -> "Ошибка безопасности при создании настроек: ${e.message}"
                is IllegalStateException -> "Ошибка состояния системы: ${e.message}"
                else -> "Не удалось создать зашифрованные настройки: ${e.message}"
            }
            
            ErrorHandler.showCriticalError(errorMessage, e)
            throw e
        }
    }

    fun initializeSQLCipher() {
        try {
            // SQLCipher loadLibs больше не нужен для стандартной SQLite
            android.util.Log.d("CryptoManager", "SQLite database ready")
        } catch (e: Exception) {
            android.util.Log.e("CryptoManager", "Failed to initialize database: ${e.message}")
            throw RuntimeException("Database initialization failed", e)
        }
    }

    fun deriveKeyFromPin(pin: String): ByteArray {
        val salt = getOrCreateSalt()
        return deriveKey(pin.toCharArray(), salt)
    }

    fun generateRandomDbKey(): ByteArray {
        val key = ByteArray(KEY_LENGTH / 8)
        SecureRandom().nextBytes(key)
        encryptedPrefs.edit()
            .putString(DB_KEY_PREF, key.encodeToString())
            .apply()
        return key
    }

    fun getExistingDbKey(): ByteArray? {
        return encryptedPrefs.getString(DB_KEY_PREF, null)?.decodeFromString()
    }
    
    fun saveDbKey(key: ByteArray) {
        encryptedPrefs.edit()
            .putString(DB_KEY_PREF, key.encodeToString())
            .apply()
    }
    
    private fun saveDbKey(key: ByteArray, editor: SharedPreferences.Editor) {
        editor.putString(DB_KEY_PREF, key.encodeToString())
    }

    private fun getOrCreateSalt(): ByteArray {
        val saltB64 = encryptedPrefs.getString(DB_SALT_PREF, null)
        return if (saltB64 != null) {
            saltB64.decodeFromString()
        } else {
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            encryptedPrefs.edit()
                .putString(DB_SALT_PREF, salt.encodeToString())
                .apply()
            salt
        }
    }

    private fun deriveKey(pin: CharArray, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        return factory.generateSecret(spec).encoded
    }

    fun sha256Pin(pin: String): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray())
    }

    fun verifyPin(pin: String, storedHash: ByteArray): Boolean {
        val inputHash = sha256Pin(pin)
        return inputHash.contentEquals(storedHash)
    }

    fun verifyPin(pin: String): Boolean {
        ErrorHandler.showInfo("CryptoManager: Проверяем PIN...")
        val storedHash = encryptedPrefs.getString("pin_hash", null)?.decodeFromString()
        val isValid = if (storedHash != null) {
            ErrorHandler.showInfo("CryptoManager: Хеш PIN найден, сравниваем...")
            verifyPin(pin, storedHash)
        } else {
            AppLogger.log("CryptoManager", "verifyPin() - no stored hash found")
            ErrorHandler.showWarning("CryptoManager: Хеш PIN не найден")
            false
        }
        AppLogger.log("CryptoManager", "verifyPin() = $isValid")
        if (isValid) {
            ErrorHandler.showSuccess("CryptoManager: PIN проверен успешно")
        } else {
            ErrorHandler.showWarning("CryptoManager: PIN неверный")
        }
        return isValid
    }

    fun setNewPin(pin: String, newPin: String, currentKey: ByteArray): ByteArray {
        val currentHash = sha256Pin(pin)
        val storedHash = encryptedPrefs.getString("pin_hash", null)?.decodeFromString()
        
        if (storedHash == null || !currentHash.contentEquals(storedHash)) {
            throw SecurityException("Неверный текущий PIN")
        }

        // Сохраняем существующий ключ БД, а не создаем новый
        val newHash = sha256Pin(newPin)
        encryptedPrefs.edit()
            .putString("pin_hash", newHash.encodeToString())
            .putString(DB_KEY_PREF, currentKey.encodeToString()) // Сохраняем существующий ключ БД
            .apply()
            
        AppLogger.log("CryptoManager", "PIN changed but DB key preserved")
        return currentKey
    }

    fun setInitialPin(pin: String): ByteArray {
        AppLogger.log("CryptoManager", "setInitialPin() - setting up new PIN...")
        ErrorHandler.showInfo("CryptoManager: Устанавливаем новый PIN...")
        
        // Если PIN уже установлен, логируем это как предупреждение
        if (isPinSet()) {
            AppLogger.log("CryptoManager", "WARNING: setInitialPin() called but PIN already exists. Overwriting...")
            ErrorHandler.showWarning("CryptoManager: PIN уже существует, перезаписываем...")
        }
        
        ErrorHandler.showInfo("CryptoManager: Создаем хеш PIN...")
        val hash = sha256Pin(pin)
        
        try {
            ErrorHandler.showInfo("CryptoManager: Сохраняем PIN...")
            val editor = encryptedPrefs.edit()
            editor.putString("pin_hash", hash.encodeToString())
            
            // Проверяем, есть ли уже ключ БД
            val existingKey = getExistingDbKey()
            if (existingKey != null) {
                // Если ключ БД уже есть, сохраняем его (не перезаписываем)
                AppLogger.log("CryptoManager", "Existing DB key found, preserving it...")
                ErrorHandler.showInfo("CryptoManager: Сохраняем существующий ключ БД...")
                saveDbKey(existingKey, editor)
            } else {
                // Если ключа БД нет, создаем новый
                AppLogger.log("CryptoManager", "No existing DB key, creating new one...")
                ErrorHandler.showInfo("CryptoManager: Создаем новый ключ БД...")
                val newKey = deriveKeyFromPin(pin)
                saveDbKey(newKey, editor)
            }
            
            // Применяем все изменения атомарно
            if (!editor.commit()) {
                throw RuntimeException("Failed to save PIN and database key")
            }
            
            val finalKey = getExistingDbKey() ?: throw IllegalStateException("Failed to save DB key")
            AppLogger.log("CryptoManager", "PIN hash and database key saved atomically")
            ErrorHandler.showSuccess("CryptoManager: PIN сохранен, ключ БД сохранен")
            return finalKey
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to set initial PIN: ${e.message}")
            ErrorHandler.showError("CryptoManager: Ошибка установки PIN: ${e.message}", e)
            throw e
        }
    }

    fun isPinSet(): Boolean {
        val isSet = encryptedPrefs.getString("pin_hash", null) != null
        AppLogger.log("CryptoManager", "isPinSet() = $isSet")
        return isSet
    }
    
    fun reKey(db: AppDb, newKey: ByteArray) {
        try {
            // Сначала сохраняем ключ в настройках
            saveDbKey(newKey)
            
            // Затем меняем ключ базы данных
            val dbInstance = db.encryptedWritableDatabase
            val hexKey = newKey.toHex()
            // Используем параметризованный запрос для безопасности
            dbInstance.execSQL("PRAGMA rekey = ?", arrayOf(hexKey))
            
            AppLogger.log("CryptoManager", "Database rekeyed successfully")
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to rekey database: ${e.message}")
            // Если что-то пошло не так, пытаемся восстановить старый ключ
            val oldKey = getExistingDbKey()
            if (oldKey != null) {
                try {
                    val oldHexKey = oldKey.toHex()
                    db.encryptedWritableDatabase.execSQL("PRAGMA rekey = ?", arrayOf(oldHexKey))
                    AppLogger.log("CryptoManager", "Restored old database key")
                } catch (restoreException: Exception) {
                    AppLogger.log("CryptoManager", "ERROR: Failed to restore old key: ${restoreException.message}")
                }
            }
            throw e
        }
    }


    fun clearSecurityData() {
        encryptedPrefs.edit().clear().apply()
    }

    private fun ByteArray.encodeToString(): String {
        return android.util.Base64.encodeToString(this, android.util.Base64.DEFAULT)
    }

    private fun String.decodeFromString(): ByteArray {
        return android.util.Base64.decode(this, android.util.Base64.DEFAULT)
    }
    
    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun deriveRuntimeKeysFromPin(pin: String) {
        // Эта функция может использоваться для получения временных ключей
        // на основе PIN для дополнительных операций шифрования
        // Пока не реализована, так как основная функциональность уже есть
    }
}
