package com.example.docapp.core
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
class DatabaseEncryption(private val context: Context) {
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val KEY_SIZE = 256
        private const val DB_ENCRYPTION_KEY = "db_encryption_key"
        private const val DB_IV_KEY = "db_iv_key"
    }
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context, "db_encryption_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyB64 = encryptedPrefs.getString(DB_ENCRYPTION_KEY, null)
        return if (keyB64 != null) {
            val keyBytes = Base64.decode(keyB64, Base64.DEFAULT)
            SecretKeySpec(keyBytes, ALGORITHM)
        } else {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(KEY_SIZE)
            val key = keyGenerator.generateKey()
            encryptedPrefs.edit()
                .putString(DB_ENCRYPTION_KEY, Base64.encodeToString(key.encoded, Base64.DEFAULT))
                .apply()
            key
        }
    }
    fun encryptString(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        try {
            val key = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val encryptedData = iv + ciphertext
            return Base64.encodeToString(encryptedData, Base64.DEFAULT)
        } catch (e: Exception) {
            return plaintext
        }
    }
    fun decryptString(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        try {
            val key = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val encryptedData = Base64.decode(encryptedText, Base64.DEFAULT)
            val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            return encryptedText
        }
    }
    fun encryptBytes(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        try {
            val key = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            val ciphertext = cipher.doFinal(data)
            return iv + ciphertext
        } catch (e: Exception) {
            return data
        }
    }
    fun decryptBytes(encryptedData: ByteArray): ByteArray {
        if (encryptedData.isEmpty()) return encryptedData
        try {
            val key = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            if (encryptedData.size <= GCM_IV_LENGTH) {
                return encryptedData
            }
            val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            return cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            return encryptedData
        }
    }
}
