package com.example.docapp.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.docapp.data.AppDb
import com.example.docapp.data.RepositoriesImpl
import com.example.docapp.data.SqlDaoFactory
import com.example.docapp.domain.Repositories
import com.example.docapp.domain.usecases.UseCases

object ServiceLocator {
    lateinit var db: AppDb
        private set
    lateinit var crypto: CryptoManager
        private set
    lateinit var files: AttachmentStore
        private set
    lateinit var repos: Repositories
        private set
    lateinit var useCases: UseCases
        private set
    lateinit var dao: SqlDaoFactory
        private set
    
    private lateinit var appContext: Context
    private var sqlCipherInitialized = false

    fun init(appCtx: Context) {
        AppLogger.log("ServiceLocator", "Initializing ServiceLocator...")
        appContext = appCtx
        
        // Инициализируем дебаг системы
        UriDebugger.init(appCtx)
        
        // Инициализируем SQLite (SQLCipher больше не используется)
        try {
            if (!sqlCipherInitialized) {
                ErrorHandler.showInfo("ServiceLocator: Инициализируем SQLite...")
                synchronized(this) {
                    if (!sqlCipherInitialized) {
                        // Стандартная SQLite не требует loadLibs
                        sqlCipherInitialized = true
                        AppLogger.log("ServiceLocator", "SQLite database ready")
                        ErrorHandler.showSuccess("База данных SQLite готова")
                    }
                }
            } else {
                AppLogger.log("ServiceLocator", "SQLite already initialized, skipping")
            }
        } catch (e: Exception) {
            AppLogger.log("ServiceLocator", "ERROR: Failed to load SQLCipher libraries: ${e.message}")
            ErrorHandler.showCriticalError("Не удалось загрузить библиотеки шифрования: ${e.message}", e)
            throw e
        }
        
        // Инициализируем криптографию
        crypto = CryptoManager(appCtx)
        AppLogger.log("ServiceLocator", "CryptoManager initialized")
        
        // НЕ создаем базу данных здесь! База создается только при первом входе с PIN
        AppLogger.log("ServiceLocator", "ServiceLocator initialized, database will be created on first PIN entry")
    }
    
    /**
     * Инициализирует базу данных с ключом, выведенным из PIN
     * Вызывается при первом входе или для проверки существующего PIN
     */
    fun initializeWithPin(pin: String, isNewPin: Boolean = false) {
        AppLogger.log("ServiceLocator", "Initializing with PIN... (isNewPin: $isNewPin)")
        ErrorHandler.showInfo("ServiceLocator: Инициализация с PIN (isNewPin: $isNewPin)")
        
        try {
            val dbKey = if (crypto.isPinSet() && !isNewPin) {
                // PIN уже установлен и это не новый PIN - СНАЧАЛА ПРОВЕРЯЕМ ПИН, затем получаем ключ
                AppLogger.log("ServiceLocator", "PIN is set, verifying PIN first...")
                ErrorHandler.showInfo("ServiceLocator: PIN установлен, проверяем PIN...")
                val isPinValid = crypto.verifyPin(pin)
                if (!isPinValid) {
                    AppLogger.log("ServiceLocator", "ERROR: Invalid PIN provided")
                    ErrorHandler.showCriticalError("Неверный PIN-код")
                    throw SecurityException("Invalid PIN")
                }
                
                ErrorHandler.showInfo("ServiceLocator: PIN проверен, получаем ключ БД...")
                val existingKey = crypto.getExistingDbKey()
                if (existingKey == null) {
                    AppLogger.log("ServiceLocator", "ERROR: PIN is set but DB key is missing")
                    ErrorHandler.showCriticalError("Ошибка безопасности: отсутствует ключ базы данных")
                    throw IllegalStateException("DB key is missing, but PIN is set")
                }
                ErrorHandler.showInfo("ServiceLocator: PIN проверен успешно")
                existingKey
            } else if (!crypto.isPinSet() && isNewPin) {
                // PIN не установлен И это новый PIN - создаем новый ключ из PIN
                AppLogger.log("ServiceLocator", "PIN not set and new PIN, creating new key from PIN...")
                ErrorHandler.showInfo("ServiceLocator: PIN не установлен, создаем новый ключ...")
                val newKey = crypto.setInitialPin(pin)
                ErrorHandler.showInfo("ServiceLocator: PIN установлен")
                newKey
            } else if (crypto.isPinSet() && isNewPin) {
                // PIN уже установлен, но это новый PIN - проверяем PIN перед установкой нового
                AppLogger.log("ServiceLocator", "PIN is set but new PIN requested, verifying current PIN first...")
                ErrorHandler.showInfo("ServiceLocator: PIN установлен, но запрошен новый PIN...")
                val isPinValid = crypto.verifyPin(pin)
                if (!isPinValid) {
                    AppLogger.log("ServiceLocator", "ERROR: Invalid current PIN provided for new PIN setup")
                    ErrorHandler.showCriticalError("Неверный текущий PIN-код")
                    throw SecurityException("Invalid current PIN")
                }
                
                // Устанавливаем новый PIN, но сохраняем существующий ключ БД
                AppLogger.log("ServiceLocator", "Current PIN verified, setting new PIN but keeping DB key...")
                ErrorHandler.showInfo("ServiceLocator: Текущий PIN проверен, устанавливаем новый...")
                val currentKey = crypto.getExistingDbKey() ?: throw IllegalStateException("Current DB key is missing")
                // Просто перезаписываем PIN, сохраняя существующий ключ БД
                val newKey = crypto.setInitialPin(pin) // Используем setInitialPin, но он сохранит существующий ключ
                ErrorHandler.showInfo("ServiceLocator: Новый PIN установлен")
                newKey
            } else {
                // Неожиданная ситуация
                AppLogger.log("ServiceLocator", "ERROR: Unexpected PIN state - isPinSet: ${crypto.isPinSet()}, isNewPin: $isNewPin")
                ErrorHandler.showCriticalError("Неожиданная ошибка состояния PIN")
                throw IllegalStateException("Unexpected PIN state")
            }
            
            // Создаем базу данных только если она еще не создана ИЛИ это новый PIN
            if (!::db.isInitialized || isNewPin) {
                if (::db.isInitialized && isNewPin) {
                    AppLogger.log("ServiceLocator", "New PIN detected, recreating database...")
                    ErrorHandler.showInfo("ServiceLocator: Новый PIN обнаружен, пересоздаем БД...")
                    // Закрываем старую базу данных
                    try {
                        db.encryptedWritableDatabase.close()
                        ErrorHandler.showInfo("ServiceLocator: Старая БД закрыта")
                    } catch (e: Exception) {
                        AppLogger.log("ServiceLocator", "Warning: Failed to close old database: ${e.message}")
                        ErrorHandler.showWarning("Не удалось закрыть старую БД: ${e.message}")
                    }
                }
                
                AppLogger.log("ServiceLocator", "Creating database...")
                ErrorHandler.showInfo("ServiceLocator: Создаем базу данных...")
                db = AppDb(appContext, dbKey)
                ErrorHandler.showInfo("ServiceLocator: БД создана, инициализируем компоненты...")
                dao = SqlDaoFactory(db)
                // Инициализируем совместимость со старым кодом
                files = AttachmentStoreImpl(appContext)
                repos = RepositoriesImpl(dao, crypto, files, appContext)
                useCases = UseCases(repos, files, dao.documents)
                AppLogger.log("ServiceLocator", "Database and components initialized successfully")
                ErrorHandler.showSuccess("ServiceLocator: База данных и компоненты инициализированы")
            } else {
                AppLogger.log("ServiceLocator", "Database already exists, PIN verification completed")
                ErrorHandler.showInfo("ServiceLocator: БД уже существует, проверка PIN завершена")
            }
        } catch (e: Exception) {
            AppLogger.log("ServiceLocator", "ERROR: Failed to initialize with PIN: ${e.message}")
            AppLogger.log("ServiceLocator", "ERROR: Exception type: ${e.javaClass.simpleName}")
            AppLogger.log("ServiceLocator", "ERROR: Stack trace: ${e.stackTraceToString()}")
            
            val errorMessage = when (e) {
                is RuntimeException -> "Критическая ошибка инициализации: ${e.message}"
                is SecurityException -> "Ошибка безопасности: ${e.message}"
                is IllegalStateException -> "Ошибка состояния системы: ${e.message}"
                else -> "Не удалось инициализировать базу данных: ${e.message}"
            }
            
            ErrorHandler.showCriticalError(errorMessage, e)
            throw e
        }
    }
}
