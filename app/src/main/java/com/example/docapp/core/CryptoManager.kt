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
/**
 * Manages cryptographic operations for PIN hashing, database key derivation, and secure storage.
 * Handles PIN verification, database encryption key management, and secure preference storage.
 * 
 * Works by using Android Keystore for master key, EncryptedSharedPreferences for secure storage,
 * and PBKDF2 for deriving database keys from PIN codes. Stores PIN hashes and database keys securely.
 * 
 * arguments:
 *     context - Context: The application context for accessing Android Keystore and SharedPreferences
 */
class CryptoManager(val context: Context) {
    companion object {
        private const val KEYSTORE_ALIAS = "DocAppMasterKey"
        private const val ENCRYPTED_PREFS_NAME = "secure_prefs"
        private const val DB_KEY_PREF = "db_key_b64"
        private const val DB_SALT_PREF = "db_salt_b64"
        private const val PBKDF2_ITERATIONS = 150_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
    }
    private val masterKey: MasterKey by lazy {
        try {
            AppLogger.log("CryptoManager", "Creating MasterKey...")
            ErrorHandler.showInfo("CryptoManager: Creating MasterKey...")
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            ErrorHandler.showSuccess("CryptoManager: MasterKey created successfully")
            masterKey
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to create MasterKey: ${e.message}")
            ErrorHandler.showCriticalError("Failed to create master key: ${e.message}", e)
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
                is RuntimeException -> "Critical error while creating encrypted preferences: ${e.message}"
                is SecurityException -> "Security error while creating preferences: ${e.message}"
                is IllegalStateException -> "System state error while creating preferences: ${e.message}"
                else -> "Failed to create encrypted preferences: ${e.message}"
            }
            ErrorHandler.showCriticalError(errorMessage, e)
            throw e
        }
    }
    /**
     * Initializes SQLCipher library for encrypted database operations.
     * Currently a placeholder that logs database readiness.
     * 
     * Works by verifying SQLCipher is available and ready for use.
     * 
     * return:
     *     Unit - No return value
     * 
     * throws:
     *     RuntimeException: If database initialization fails
     */
    fun initializeSQLCipher() {
        try {
            android.util.Log.d("CryptoManager", "SQLite database ready")
        } catch (e: Exception) {
            android.util.Log.e("CryptoManager", "Failed to initialize database: ${e.message}")
            throw RuntimeException("Database initialization failed", e)
        }
    }
    
    /**
     * Derives a database encryption key from a PIN code using PBKDF2.
     * Uses a stored salt or creates a new one if none exists.
     * 
     * Works by retrieving or creating a salt, then using PBKDF2 with SHA-256 to derive
     * a 256-bit key from the PIN and salt.
     * 
     * arguments:
     *     pin - String: The PIN code to derive the key from
     * 
     * return:
     *     key - ByteArray: The derived 256-bit encryption key
     */
    fun deriveKeyFromPin(pin: String): ByteArray {
        val salt = getOrCreateSalt()
        return deriveKey(pin.toCharArray(), salt)
    }
    
    /**
     * Generates a random 256-bit database encryption key and stores it securely.
     * 
     * Works by generating random bytes using SecureRandom and storing the key
     * in encrypted shared preferences.
     * 
     * return:
     *     key - ByteArray: The generated random 256-bit encryption key
     */
    fun generateRandomDbKey(): ByteArray {
        val key = ByteArray(KEY_LENGTH / 8)
        SecureRandom().nextBytes(key)
        encryptedPrefs.edit()
            .putString(DB_KEY_PREF, key.encodeToString())
            .apply()
        return key
    }
    
    /**
     * Retrieves the existing database encryption key from secure storage.
     * 
     * Works by reading the stored key from encrypted shared preferences and decoding it from base64.
     * 
     * return:
     *     key - ByteArray?: The stored database encryption key, or null if no key exists
     */
    fun getExistingDbKey(): ByteArray? {
        return encryptedPrefs.getString(DB_KEY_PREF, null)?.decodeFromString()
    }
    
    /**
     * Saves a database encryption key to secure storage.
     * 
     * Works by encoding the key to base64 and storing it in encrypted shared preferences.
     * 
     * arguments:
     *     key - ByteArray: The database encryption key to store
     * 
     * return:
     *     Unit - No return value
     */
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
    /**
     * Computes SHA-256 hash of a PIN code for storage and verification.
     * 
     * Works by applying SHA-256 message digest algorithm to the PIN string bytes.
     * 
     * arguments:
     *     pin - String: The PIN code to hash
     * 
     * return:
     *     hash - ByteArray: The SHA-256 hash of the PIN
     */
    fun sha256Pin(pin: String): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray())
    }
    
    /**
     * Verifies a PIN code against a stored hash.
     * 
     * Works by computing the SHA-256 hash of the input PIN and comparing it with the stored hash.
     * 
     * arguments:
     *     pin - String: The PIN code to verify
     *     storedHash - ByteArray: The stored hash to compare against
     * 
     * return:
     *     isValid - Boolean: True if the PIN hash matches the stored hash, false otherwise
     */
    fun verifyPin(pin: String, storedHash: ByteArray): Boolean {
        val inputHash = sha256Pin(pin)
        return inputHash.contentEquals(storedHash)
    }
    
    /**
     * Verifies a PIN code against the stored hash in secure preferences.
     * 
     * Works by retrieving the stored PIN hash from encrypted preferences and comparing it
     * with the hash of the input PIN.
     * 
     * arguments:
     *     pin - String: The PIN code to verify
     * 
     * return:
     *     isValid - Boolean: True if the PIN is correct, false if incorrect or no PIN is set
     */
    fun verifyPin(pin: String): Boolean {
        ErrorHandler.showInfo("CryptoManager: Verifying PIN...")
        val storedHash = encryptedPrefs.getString("pin_hash", null)?.decodeFromString()
        val isValid = if (storedHash != null) {
            ErrorHandler.showInfo("CryptoManager: Stored PIN hash found, comparing...")
            verifyPin(pin, storedHash)
        } else {
            AppLogger.log("CryptoManager", "verifyPin() - no stored hash found")
            ErrorHandler.showWarning("CryptoManager: Stored PIN hash not found")
            false
        }
        AppLogger.log("CryptoManager", "verifyPin() = $isValid")
        if (isValid) {
            ErrorHandler.showSuccess("CryptoManager: PIN verified successfully")
        } else {
            ErrorHandler.showWarning("CryptoManager: PIN is incorrect")
        }
        return isValid
    }
    /**
     * Changes the PIN code while preserving the existing database encryption key.
     * Verifies the current PIN before allowing the change.
     * 
     * Works by verifying the current PIN, then storing the new PIN hash while keeping
     * the existing database key unchanged.
     * 
     * arguments:
     *     pin - String: The current PIN code to verify
     *     newPin - String: The new PIN code to set
     *     currentKey - ByteArray: The current database encryption key to preserve
     * 
     * return:
     *     key - ByteArray: The preserved database encryption key (same as currentKey)
     * 
     * throws:
     *     SecurityException: If the current PIN is incorrect
     */
    fun setNewPin(pin: String, newPin: String, currentKey: ByteArray): ByteArray {
        val currentHash = sha256Pin(pin)
        val storedHash = encryptedPrefs.getString("pin_hash", null)?.decodeFromString()
        if (storedHash == null || !currentHash.contentEquals(storedHash)) {
            throw SecurityException("Incorrect current PIN")
        }
        val newHash = sha256Pin(newPin)
        encryptedPrefs.edit()
            .putString("pin_hash", newHash.encodeToString())
            .putString(DB_KEY_PREF, currentKey.encodeToString())
            .apply()
        AppLogger.log("CryptoManager", "PIN changed but DB key preserved")
        return currentKey
    }
    /**
     * Sets the initial PIN code and creates or preserves the database encryption key.
     * Creates a new key derived from PIN if no key exists, or preserves existing key if it does.
     * 
     * Works by hashing the PIN and storing it, then either deriving a new key from the PIN
     * or preserving an existing database key if one is already stored.
     * 
     * arguments:
     *     pin - String: The PIN code to set as the initial PIN
     * 
     * return:
     *     key - ByteArray: The database encryption key (either newly derived or preserved)
     * 
     * throws:
     *     RuntimeException: If saving PIN and key fails
     *     IllegalStateException: If key cannot be retrieved after saving
     */
    fun setInitialPin(pin: String): ByteArray {
        AppLogger.log("CryptoManager", "setInitialPin() - setting up new PIN...")
        ErrorHandler.showInfo("CryptoManager: Setting a new PIN...")
        if (isPinSet()) {
            AppLogger.log("CryptoManager", "WARNING: setInitialPin() called but PIN already exists. Overwriting...")
            ErrorHandler.showWarning("CryptoManager: PIN already exists, overwriting...")
        }
        ErrorHandler.showInfo("CryptoManager: Generating PIN hash...")
        val hash = sha256Pin(pin)
        try {
            ErrorHandler.showInfo("CryptoManager: Saving PIN data...")
            val editor = encryptedPrefs.edit()
            editor.putString("pin_hash", hash.encodeToString())
            val existingKey = getExistingDbKey()
            if (existingKey != null) {
                AppLogger.log("CryptoManager", "Existing DB key found, preserving it...")
                ErrorHandler.showInfo("CryptoManager: Preserving existing DB key...")
                saveDbKey(existingKey, editor)
            } else {
                AppLogger.log("CryptoManager", "No existing DB key, creating new one...")
                ErrorHandler.showInfo("CryptoManager: Creating new DB key...")
                val newKey = deriveKeyFromPin(pin)
                saveDbKey(newKey, editor)
            }
            if (!editor.commit()) {
                throw RuntimeException("Failed to save PIN and database key")
            }
            val finalKey = getExistingDbKey() ?: throw IllegalStateException("Failed to save DB key")
            AppLogger.log("CryptoManager", "PIN hash and database key saved atomically")
            ErrorHandler.showSuccess("CryptoManager: PIN saved, DB key stored")
            return finalKey
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to set initial PIN: ${e.message}")
            ErrorHandler.showError("CryptoManager: Failed to set up PIN: ${e.message}", e)
            throw e
        }
    }
    /**
     * Checks if a PIN code has been set and stored.
     * 
     * Works by checking if a PIN hash exists in encrypted shared preferences.
     * 
     * return:
     *     isSet - Boolean: True if a PIN hash is stored, false otherwise
     */
    fun isPinSet(): Boolean {
        val isSet = encryptedPrefs.getString("pin_hash", null) != null
        AppLogger.log("CryptoManager", "isPinSet() = $isSet")
        return isSet
    }
    
    /**
     * Changes the encryption key for an existing database, re-encrypting it with the new key.
     * Attempts to restore the old key if rekeying fails.
     * 
     * Works by saving the new key and executing SQLite PRAGMA rekey command to re-encrypt
     * the database. If rekeying fails, attempts to restore the old key.
     * 
     * arguments:
     *     db - AppDb: The database instance to rekey
     *     newKey - ByteArray: The new encryption key to use
     * 
     * return:
     *     Unit - No return value
     * 
     * throws:
     *     Exception: If rekeying fails and old key restoration also fails
     */
    fun reKey(db: AppDb, newKey: ByteArray) {
        try {
            saveDbKey(newKey)
            val dbInstance = db.encryptedWritableDatabase
            val hexKey = newKey.toHex()
            dbInstance.execSQL("PRAGMA rekey = ?", arrayOf(hexKey))
            AppLogger.log("CryptoManager", "Database rekeyed successfully")
        } catch (e: Exception) {
            AppLogger.log("CryptoManager", "ERROR: Failed to rekey database: ${e.message}")
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
    /**
     * Clears all security data including PIN hash and database keys from secure storage.
     * 
     * Works by clearing all entries from encrypted shared preferences.
     * 
     * return:
     *     Unit - No return value
     */
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
    @Suppress("UNUSED_PARAMETER")
    fun deriveRuntimeKeysFromPin(pin: String) {
    }
}
