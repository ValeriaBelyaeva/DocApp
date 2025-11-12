package com.example.docapp.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.example.docapp.core.AppLogger
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher

class AttachmentCrypto(private val context: Context) {
    
    companion object {
        private const val KEY_ALIAS = "attachment_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        
        // Flag to enable encryption (can be toggled via settings)
        var encryptionEnabled: Boolean = false
            private set
    }
    
    private var masterKey: MasterKey? = null
    
    init {
        initializeMasterKey()
    }
    
    fun enableEncryption() {
        encryptionEnabled = true
        AppLogger.log("AttachmentCrypto", "Encryption enabled")
    }
    
    fun disableEncryption() {
        encryptionEnabled = false
        AppLogger.log("AttachmentCrypto", "Encryption disabled")
    }
    
    private fun initializeMasterKey() {
        try {
            if (encryptionEnabled) {
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(ENCRYPTION_BLOCK_MODE)
                    .setEncryptionPaddings(ENCRYPTION_PADDING)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                
                masterKey = MasterKey.Builder(context)
                    .setKeyGenParameterSpec(keyGenParameterSpec)
                    .build()
                
                AppLogger.log("AttachmentCrypto", "Master key initialized")
            }
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to initialize master key: ${e.message}")
            throw RuntimeException("Cannot initialize encryption", e)
        }
    }
    
    fun createEncryptedFile(file: File): EncryptedFile? {
        return try {
            if (!encryptionEnabled || masterKey == null) {
                return null
            }
            
            EncryptedFile.Builder(
                context,
                file,
                masterKey!!,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to create encrypted file: ${e.message}")
            null
        }
    }
    
    fun writeToEncryptedFile(encryptedFile: EncryptedFile, inputStream: InputStream): Boolean {
        return try {
            encryptedFile.openFileOutput().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            true
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to write encrypted file: ${e.message}")
            false
        }
    }
    
    fun readFromEncryptedFile(encryptedFile: EncryptedFile): InputStream? {
        return try {
            encryptedFile.openFileInput()
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to read encrypted file: ${e.message}")
            null
        }
    }
    
    fun isEncrypted(@Suppress("UNUSED_PARAMETER") file: File): Boolean {
        return try {
            if (!encryptionEnabled) return false
            
            // Check whether the file is encrypted
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to check encryption status: ${e.message}")
            false
        }
    }
    
    fun deleteMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_ALIAS)
            masterKey = null
            AppLogger.log("AttachmentCrypto", "Master key deleted")
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to delete master key: ${e.message}")
        }
    }
    
    fun getEncryptionStatus(): EncryptionStatus {
        return try {
            if (!encryptionEnabled) {
                return EncryptionStatus.DISABLED
            }
            
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            if (keyStore.containsAlias(KEY_ALIAS)) {
                EncryptionStatus.ENABLED_AND_READY
            } else {
                EncryptionStatus.ENABLED_NOT_READY
            }
        } catch (e: Exception) {
            AppLogger.log("AttachmentCrypto", "ERROR: Failed to get encryption status: ${e.message}")
            EncryptionStatus.ERROR
        }
    }
    
    enum class EncryptionStatus {
        DISABLED,
        ENABLED_AND_READY,
        ENABLED_NOT_READY,
        ERROR
    }
}
