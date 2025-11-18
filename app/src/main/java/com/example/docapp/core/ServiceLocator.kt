package com.example.docapp.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.docapp.data.AppDb
import com.example.docapp.data.RepositoriesImpl
import com.example.docapp.data.SqlDaoFactory
import com.example.docapp.data.transfer.DataTransferManager
import com.example.docapp.domain.Repositories
import com.example.docapp.domain.interactors.DomainInteractors
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
    lateinit var domain: DomainInteractors
        private set
    lateinit var useCases: UseCases
        private set
    lateinit var dao: SqlDaoFactory
        private set
    lateinit var dataTransfer: DataTransferManager
        private set
    
    private lateinit var appContext: Context
    private var sqlCipherInitialized = false

    fun init(appCtx: Context) {
        AppLogger.log("ServiceLocator", "Initializing ServiceLocator...")
        appContext = appCtx
        
        // Initialize debugging utilities
        UriDebugger.init(appCtx)
        
        // Initialize SQLite (SQLCipher is no longer used)
        try {
            if (!sqlCipherInitialized) {
                ErrorHandler.showInfo("ServiceLocator: Initializing SQLite...")
                synchronized(this) {
                    if (!sqlCipherInitialized) {
                        // Standard SQLite does not require loadLibs
                        sqlCipherInitialized = true
                        AppLogger.log("ServiceLocator", "SQLite database ready")
                        ErrorHandler.showSuccess("SQLite database is ready")
                    }
                }
            } else {
                AppLogger.log("ServiceLocator", "SQLite already initialized, skipping")
            }
        } catch (e: Exception) {
            AppLogger.log("ServiceLocator", "ERROR: Failed to load SQLCipher libraries: ${e.message}")
            ErrorHandler.showCriticalError("Failed to load encryption libraries: ${e.message}", e)
            throw e
        }
        
        // Initialize cryptography
        crypto = CryptoManager(appCtx)
        AppLogger.log("ServiceLocator", "CryptoManager initialized")
        
        // Do NOT create the database here! It is created the first time the PIN is entered
        AppLogger.log("ServiceLocator", "ServiceLocator initialized, database will be created on first PIN entry")
    }
    
    /**
     * Initializes the database with a key derived from the PIN.
     * Called on the first login or when validating an existing PIN.
     */
    fun initializeWithPin(pin: String, isNewPin: Boolean = false) {
        AppLogger.log("ServiceLocator", "Initializing with PIN... (isNewPin: $isNewPin)")
        ErrorHandler.showInfo("ServiceLocator: Initializing with PIN (isNewPin: $isNewPin)")
        
        try {
            val dbKey = if (crypto.isPinSet() && !isNewPin) {
                // PIN already set and this is not a new PIN: verify PIN first, then fetch the DB key
                AppLogger.log("ServiceLocator", "PIN is set, verifying PIN first...")
                ErrorHandler.showInfo("ServiceLocator: PIN is set, verifying PIN...")
                val isPinValid = crypto.verifyPin(pin)
                if (!isPinValid) {
                    AppLogger.log("ServiceLocator", "ERROR: Invalid PIN provided")
                    ErrorHandler.showCriticalError("Incorrect PIN code")
                    throw SecurityException("Invalid PIN")
                }
                
                ErrorHandler.showInfo("ServiceLocator: PIN verified, retrieving DB key...")
                val existingKey = crypto.getExistingDbKey()
                if (existingKey == null) {
                    AppLogger.log("ServiceLocator", "ERROR: PIN is set but DB key is missing")
                    ErrorHandler.showCriticalError("Security error: database key is missing")
                    throw IllegalStateException("DB key is missing, but PIN is set")
                }
                ErrorHandler.showInfo("ServiceLocator: PIN verified successfully")
                existingKey
            } else if (!crypto.isPinSet() && isNewPin) {
                // PIN not set and this is a new PIN: create a new key from the PIN
                AppLogger.log("ServiceLocator", "PIN not set and new PIN, creating new key from PIN...")
                ErrorHandler.showInfo("ServiceLocator: PIN is not set, creating new key...")
                val newKey = crypto.setInitialPin(pin)
                ErrorHandler.showInfo("ServiceLocator: PIN has been set")
                newKey
            } else if (crypto.isPinSet() && isNewPin) {
                // PIN set but a new PIN requested: verify current PIN before updating
                AppLogger.log("ServiceLocator", "PIN is set but new PIN requested, verifying current PIN first...")
                ErrorHandler.showInfo("ServiceLocator: PIN is set, validating before updating...")
                val isPinValid = crypto.verifyPin(pin)
                if (!isPinValid) {
                    AppLogger.log("ServiceLocator", "ERROR: Invalid current PIN provided for new PIN setup")
                    ErrorHandler.showCriticalError("Incorrect current PIN code")
                    throw SecurityException("Invalid current PIN")
                }
                
                // Update the PIN while preserving the existing DB key
                AppLogger.log("ServiceLocator", "Current PIN verified, setting new PIN but keeping DB key...")
                ErrorHandler.showInfo("ServiceLocator: Current PIN verified, updating...")
                crypto.getExistingDbKey() ?: throw IllegalStateException("Current DB key is missing")
                // Overwrite the stored PIN while keeping the DB key intact
                val newKey = crypto.setInitialPin(pin) // setInitialPin keeps the existing key if present
                ErrorHandler.showInfo("ServiceLocator: New PIN saved")
                newKey
            } else {
                // Unexpected branch
                AppLogger.log("ServiceLocator", "ERROR: Unexpected PIN state - isPinSet: ${crypto.isPinSet()}, isNewPin: $isNewPin")
                ErrorHandler.showCriticalError("Unexpected PIN state")
                throw IllegalStateException("Unexpected PIN state")
            }
            
            // Create the database only if it has not been initialized yet, or when setting a new PIN
            if (!::db.isInitialized || isNewPin) {
                if (::db.isInitialized && isNewPin) {
                    AppLogger.log("ServiceLocator", "New PIN detected, recreating database...")
                    ErrorHandler.showInfo("ServiceLocator: New PIN detected, recreating DB...")
                    // Close the previous database instance
                    try {
                        db.encryptedWritableDatabase.close()
                        ErrorHandler.showInfo("ServiceLocator: Previous database closed")
                    } catch (e: Exception) {
                        AppLogger.log("ServiceLocator", "Warning: Failed to close old database: ${e.message}")
                        ErrorHandler.showWarning("Failed to close previous database: ${e.message}")
                    }
                }
                
                AppLogger.log("ServiceLocator", "Creating database...")
                ErrorHandler.showInfo("ServiceLocator: Creating database...")
                db = AppDb(appContext, dbKey)
                ErrorHandler.showInfo("ServiceLocator: Database created, initializing components...")
                dao = SqlDaoFactory(db)
                // Initialize compatibility layer for legacy code paths
                files = AttachmentStoreImpl(appContext)
                repos = RepositoriesImpl(dao, crypto, files, appContext)
                domain = DomainInteractors(repos, files, dao.documents)
                useCases = UseCases(repos, files, dao.documents)
                dataTransfer = DataTransferManager(appContext, db, dao)
                AppLogger.log("ServiceLocator", "Database and components initialized successfully")
                ErrorHandler.showSuccess("ServiceLocator: Database and components initialized")
            } else {
                AppLogger.log("ServiceLocator", "Database already exists, PIN verification completed")
                ErrorHandler.showInfo("ServiceLocator: Database already exists, PIN verification completed")
            }
        } catch (e: Exception) {
            AppLogger.log("ServiceLocator", "ERROR: Failed to initialize with PIN: ${e.message}")
            AppLogger.log("ServiceLocator", "ERROR: Exception type: ${e.javaClass.simpleName}")
            AppLogger.log("ServiceLocator", "ERROR: Stack trace: ${e.stackTraceToString()}")
            
            val errorMessage = when (e) {
                is RuntimeException -> "Critical initialization error: ${e.message}"
                is SecurityException -> "Security error: ${e.message}"
                is IllegalStateException -> "System state error: ${e.message}"
                else -> "Failed to initialize database: ${e.message}"
            }
            
            ErrorHandler.showCriticalError(errorMessage, e)
            throw e
        }
    }
}
