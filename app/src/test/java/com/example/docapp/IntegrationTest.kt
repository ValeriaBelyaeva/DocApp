package com.example.docapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.docapp.core.AppProtection
import com.example.docapp.core.SecurityChecker
import com.example.docapp.core.SecurityLevel
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Интеграционные тесты для проверки работы всех компонентов вместе
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IntegrationTest {
    
    private lateinit var context: Context
    private lateinit var appProtection: AppProtection
    private lateinit var securityChecker: SecurityChecker
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appProtection = AppProtection(context)
        securityChecker = SecurityChecker(context)
    }
    
    @Test
    fun testAppProtectionCreation() {
        assertNotNull("AppProtection должен быть создан", appProtection)
    }
    
    @Test
    fun testSecurityCheckerIntegration() {
        assertNotNull("SecurityChecker должен быть создан", securityChecker)
        
        val report = securityChecker.performSecurityCheck()
        assertNotNull("Отчет безопасности должен быть создан", report)
    }
    
    @Test
    fun testAppProtectionSecurityLevel() {
        val securityLevel = appProtection.getCurrentSecurityLevel()
        assertNotNull("Уровень безопасности должен быть определен", securityLevel)
        
        // В тестовой среде уровень должен быть безопасным или низким риском
        assertTrue("Уровень безопасности в тестах должен быть приемлемым",
            securityLevel == SecurityLevel.SAFE || 
            securityLevel == SecurityLevel.LOW_RISK)
    }
    
    @Test
    fun testAppIntegrityVerification() {
        val isIntact = appProtection.verifyAppIntegrity()
        assertTrue("Целостность приложения должна быть проверена", isIntact)
    }
    
    @Test
    fun testApkIntegrityVerification() {
        val isIntact = appProtection.verifyApkIntegrity()
        assertTrue("Целостность APK должна быть проверена", isIntact)
    }
    
    @Test
    fun testEnvironmentSafetyCheck() {
        val isSafe = appProtection.checkEnvironmentSafety()
        assertNotNull("Проверка безопасности среды должна работать", isSafe)
    }
    
    @Test
    fun testSafeDataHandling() {
        val canHandle = appProtection.canSafelyHandleSensitiveData()
        assertNotNull("Проверка возможности безопасной работы с данными должна работать", canHandle)
        
        // В тестовой среде обычно можно безопасно работать с данными
        assertTrue("В тестовой среде должно быть безопасно работать с данными", canHandle)
    }
    
    @Test
    fun testProtectionLifecycle() {
        // Тестируем запуск и остановку защиты
        assertDoesNotThrow("Запуск защиты не должен вызывать исключений") {
            appProtection.startProtection()
        }
        
        assertDoesNotThrow("Остановка защиты не должна вызывать исключений") {
            appProtection.stopProtection()
        }
    }
    
    @Test
    fun testSecurityReportCompleteness() {
        val report = securityChecker.performSecurityCheck()
        
        // Проверяем, что все поля отчета заполнены
        assertNotNull("isEmulator должно быть определено", report.isEmulator)
        assertNotNull("isRooted должно быть определено", report.isRooted)
        assertNotNull("isOfficialStore должно быть определено", report.isOfficialStore)
        assertNotNull("isDebugging должно быть определено", report.isDebugging)
        assertNotNull("hasModificationTools должно быть определено", report.hasModificationTools)
        assertNotNull("hasSuspiciousApps должно быть определено", report.hasSuspiciousApps)
        assertNotNull("securityLevel должно быть определено", report.securityLevel)
    }
    
    @Test
    fun testSecurityLevelConsistency() {
        val report1 = securityChecker.performSecurityCheck()
        val report2 = securityChecker.performSecurityCheck()
        
        // В тестовой среде результаты должны быть консистентными
        assertEquals("Уровень безопасности должен быть консистентным", 
            report1.securityLevel, report2.securityLevel)
    }
}
