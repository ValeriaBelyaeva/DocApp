package com.example.docapp
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.docapp.core.DatabaseEncryption
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EncryptionTest {
    private lateinit var context: Context
    private lateinit var encryption: DatabaseEncryption
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        encryption = DatabaseEncryption(context)
    }
    @Test
    fun testEncryptionCreation() {
        assertNotNull("DatabaseEncryption должен быть создан", encryption)
    }
    @Test
    fun testStringEncryptionDecryption() {
        val originalText = "Тестовый текст для шифрования"
        val encrypted = encryption.encryptString(originalText)
        assertNotNull("Зашифрованный текст не должен быть null", encrypted)
        assertNotEquals("Зашифрованный текст должен отличаться от оригинала", originalText, encrypted)
        val decrypted = encryption.decryptString(encrypted)
        assertNotNull("Расшифрованный текст не должен быть null", decrypted)
        assertEquals("Расшифрованный текст должен совпадать с оригиналом", originalText, decrypted)
    }
    @Test
    fun testEmptyStringEncryption() {
        val emptyString = ""
        val encrypted = encryption.encryptString(emptyString)
        assertEquals("Пустая строка должна остаться пустой", emptyString, encrypted)
        val decrypted = encryption.decryptString(encrypted)
        assertEquals("Расшифрованная пустая строка должна остаться пустой", emptyString, decrypted)
    }
    @Test
    fun testBytesEncryptionDecryption() {
        val originalBytes = "Тестовые байты".toByteArray(Charsets.UTF_8)
        val encrypted = encryption.encryptBytes(originalBytes)
        assertNotNull("Зашифрованные байты не должны быть null", encrypted)
        val decrypted = encryption.decryptBytes(encrypted)
        assertNotNull("Расшифрованные байты не должны быть null", decrypted)
        assertArrayEquals("Расшифрованные байты должны совпадать с оригиналом", originalBytes, decrypted)
    }
    @Test
    fun testMultipleEncryptionsProduceDifferentResults() {
        val text = "Одинаковый текст"
        val encrypted1 = encryption.encryptString(text)
        val encrypted2 = encryption.encryptString(text)
        assertNotEquals("Два шифрования одного текста должны давать разные результаты",
            encrypted1, encrypted2)
        val decrypted1 = encryption.decryptString(encrypted1)
        val decrypted2 = encryption.decryptString(encrypted2)
        assertEquals("Расшифровка должна давать одинаковый результат", decrypted1, decrypted2)
        assertEquals("Расшифрованный текст должен совпадать с оригиналом", text, decrypted1)
    }
    @Test
    fun testSpecialCharactersEncryption() {
        val specialText = "Специальные символы: !@#$%^&*()_+-=[]{}|;':\",./<>?`~"
        val encrypted = encryption.encryptString(specialText)
        val decrypted = encryption.decryptString(encrypted)
        assertEquals("Специальные символы должны корректно шифроваться", specialText, decrypted)
    }
    @Test
    fun testUnicodeEncryption() {
        val unicodeText = "Unicode текст: 🚀🔒💾📱⭐"
        val encrypted = encryption.encryptString(unicodeText)
        val decrypted = encryption.decryptString(encrypted)
        assertEquals("Unicode символы должны корректно шифроваться", unicodeText, decrypted)
    }
    @Test
    fun testLongTextEncryption() {
        val longText = "Очень длинный текст ".repeat(1000)
        val encrypted = encryption.encryptString(longText)
        val decrypted = encryption.decryptString(encrypted)
        assertEquals("Длинный текст должен корректно шифроваться", longText, decrypted)
    }
}
