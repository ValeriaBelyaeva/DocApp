package com.example.docapp.core

import android.content.Context
import java.security.MessageDigest

interface CryptoManager {
    fun deriveRuntimeKeysFromPin(pin: String): Result<Unit>
    fun rotateKeys(oldPin: String, newPin: String): Result<Unit>
    fun encryptField(plain: ByteArray): ByteArray
    fun decryptField(cipher: ByteArray): ByteArray
    fun sha256(bytes: ByteArray): ByteArray
}

class CryptoManagerImpl(ctx: Context) : CryptoManager {
    override fun deriveRuntimeKeysFromPin(pin: String) = Result.success(Unit)
    override fun rotateKeys(oldPin: String, newPin: String) = Result.success(Unit)
    override fun encryptField(plain: ByteArray) = plain // TODO AES-GCM
    override fun decryptField(cipher: ByteArray) = cipher
    override fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)
}
