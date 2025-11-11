package com.example.docapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.io.File
import android.database.sqlite.SQLiteDatabase as SQLCipherDatabase
import com.example.docapp.core.newId
import com.example.docapp.core.now
import com.example.docapp.core.DatabaseEncryption
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext


class AppDb(private val ctx: Context, val passphrase: ByteArray) {
    
    val dbEncryption = DatabaseEncryption(ctx)
    
    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
    
    @Volatile
    private var _encryptedDb: SQLCipherDatabase? = null
    @Volatile
    private var _initializationError: Exception? = null
    
    @Synchronized
    private fun getEncryptedDb(): SQLCipherDatabase {
        if (_encryptedDb != null) {
            return _encryptedDb ?: throw IllegalStateException("Database is null")
        }
        
        if (_initializationError != null) {
            throw RuntimeException("Database initialization failed", _initializationError)
        }
        
        try {
            AppLogger.log("AppDb", "Opening encrypted database...")
            ErrorHandler.showInfo("AppDb: Открываем зашифрованную базу данных...")
            val dbPath = ctx.getDatabasePath("docapp.db").absolutePath
            val dbFile = File(dbPath)
            
            // Проверяем, существует ли файл базы данных
            if (dbFile.exists()) {
                ErrorHandler.showInfo("AppDb: Файл БД существует, проверяем...")
                try {
                    // Пытаемся открыть существующую базу
                    val testDb = SQLCipherDatabase.openDatabase(dbPath, null, SQLCipherDatabase.OPEN_READONLY)
                    testDb.close()
                    ErrorHandler.showInfo("AppDb: Существующая БД корректна")
                } catch (e: Exception) {
                    AppLogger.log("AppDb", "WARNING: Existing database file is corrupted or not encrypted: ${e.message}")
                    ErrorHandler.showWarning("AppDb: Файл БД поврежден, удаляем и создаем заново...")
                    // Удаляем поврежденный файл
                    dbFile.delete()
                    // Также удаляем связанные файлы
                    File("$dbPath-wal").delete()
                    File("$dbPath-shm").delete()
                }
            }
            
            // Создаем или открываем зашифрованную базу
            ErrorHandler.showInfo("AppDb: Создаем/открываем БД с ключом...")
            val db = SQLCipherDatabase.openOrCreateDatabase(dbPath, null)
            AppLogger.log("AppDb", "Database opened/created successfully")
            ErrorHandler.showSuccess("AppDb: База данных открыта/создана успешно")
            
            // Создаем схему базы данных если она не существует
            ErrorHandler.showInfo("AppDb: Создаем таблицы если необходимо...")
            createTablesIfNeeded(db)
            
            // Выполняем миграции
            ErrorHandler.showInfo("AppDb: Выполняем миграции...")
            runMigrations(db)
            
            _encryptedDb = db
            return db
        } catch (e: Exception) {
            AppLogger.log("AppDb", "ERROR: Failed to open encrypted database: ${e.message}")
            ErrorHandler.showCriticalError("Не удалось открыть зашифрованную базу данных", e)
            _initializationError = e
            throw RuntimeException("Cannot open encrypted database", e)
        }
    }
    
    val encryptedWritableDatabase: SQLCipherDatabase
        get() = getEncryptedDb()
    
    val encryptedReadableDatabase: SQLCipherDatabase
        get() = getEncryptedDb()

    private fun runMigrations(db: SQLCipherDatabase) {
        try {
            // Миграция: добавляем поле display_name в таблицу attachments
            val columns = db.rawQuery("PRAGMA table_info(attachments)", null).use { cursor ->
                generateSequence {
                    if (cursor.moveToNext()) cursor.getString(cursor.getColumnIndexOrThrow("name")) else null
                }.toList()
            }
            
            if (!columns.contains("display_name")) {
                AppLogger.log("AppDb", "Adding display_name column to attachments table")
                ErrorHandler.showInfo("AppDb: Добавляем поле display_name в таблицу attachments")
                db.execSQL("ALTER TABLE attachments ADD COLUMN display_name TEXT")
            }
            
            // Миграция Mx_AddAttachments: создаем новую таблицу для современных вложений
            val newAttachmentsExists = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='attachments_new'",
                null
            ).use { it.count > 0 }
            
            if (!newAttachmentsExists) {
                AppLogger.log("AppDb", "Creating new attachments table for modern attachment system")
                ErrorHandler.showInfo("AppDb: Создаем новую таблицу attachments_new для современной системы вложений")
                db.execSQL("""
                    CREATE TABLE attachments_new(
                        id TEXT PRIMARY KEY,
                        docId TEXT,
                        name TEXT NOT NULL,
                        mime TEXT NOT NULL,
                        size INTEGER NOT NULL,
                        sha256 TEXT NOT NULL,
                        path TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(docId) REFERENCES documents(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX idx_attachments_new_docId ON attachments_new(docId)")
                db.execSQL("CREATE INDEX idx_attachments_new_sha256 ON attachments_new(sha256)")
            }
            
            AppLogger.log("AppDb", "Migrations completed successfully")
        } catch (e: Exception) {
            AppLogger.log("AppDb", "ERROR: Failed to run migrations: ${e.message}")
            ErrorHandler.showWarning("Не удалось выполнить миграции: ${e.message}")
        }
    }

    private fun createTablesIfNeeded(db: SQLCipherDatabase) {
        try {
            // Проверяем, существует ли таблица templates
            val tableExists = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='templates'",
                null
            ).use { it.count > 0 }
            
            if (!tableExists) {
                ErrorHandler.showInfo("AppDb: Создаем таблицы БД...")
                db.execSQL("""CREATE TABLE templates(
                    id TEXT PRIMARY KEY, name TEXT NOT NULL,
                    is_pinned INTEGER NOT NULL, pinned_order INTEGER,
                    created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
                )""")
                db.execSQL("""CREATE TABLE template_fields(
                    id TEXT PRIMARY KEY, template_id TEXT NOT NULL,
                    name TEXT NOT NULL, type TEXT NOT NULL, ord INTEGER NOT NULL,
                    FOREIGN KEY(template_id) REFERENCES templates(id) ON DELETE CASCADE
                )""")
                db.execSQL("""CREATE TABLE folders(
                    id TEXT PRIMARY KEY, parent_id TEXT, name TEXT NOT NULL, ord INTEGER NOT NULL,
                    FOREIGN KEY(parent_id) REFERENCES folders(id) ON DELETE CASCADE
                )""")
                db.execSQL("""CREATE TABLE documents(
                    id TEXT PRIMARY KEY, template_id TEXT, folder_id TEXT,
                    name TEXT NOT NULL, description TEXT, is_pinned INTEGER NOT NULL, pinned_order INTEGER,
                    created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, last_opened_at INTEGER NOT NULL,
                    FOREIGN KEY(template_id) REFERENCES templates(id),
                    FOREIGN KEY(folder_id) REFERENCES folders(id)
                )""")
                db.execSQL("""CREATE TABLE document_fields(
                    id TEXT PRIMARY KEY, document_id TEXT NOT NULL, name TEXT NOT NULL,
                    value BLOB, preview TEXT, is_secret INTEGER NOT NULL, ord INTEGER NOT NULL,
                    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
                )""")
                db.execSQL("""CREATE TABLE attachments(
                    id TEXT PRIMARY KEY, document_id TEXT NOT NULL, kind TEXT NOT NULL,
                    file_name TEXT, display_name TEXT, uri TEXT NOT NULL, created_at INTEGER NOT NULL,
                    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
                )""")
                db.execSQL("""CREATE TABLE settings(
                    id INTEGER PRIMARY KEY CHECK (id=1),
                    pin_hash BLOB NOT NULL, pin_salt BLOB NOT NULL, db_key_salt BLOB NOT NULL, version TEXT NOT NULL
                )""")
                db.execSQL("PRAGMA foreign_keys=ON")
                // строка настроек по умолчанию
                db.execSQL("INSERT INTO settings(id,pin_hash,pin_salt,db_key_salt,version) VALUES(1, x'', x'', x'', '1.0.0')")
                
                // Проверяем, есть ли уже базовые данные
                val templatesCount = db.rawQuery("SELECT COUNT(*) FROM templates", null).use {
                    it.moveToFirst()
                    it.getInt(0)
                }
                
                if (templatesCount == 0) {
                    ErrorHandler.showInfo("AppDb: Заполняем БД базовыми данными...")
                    seedBasics(db)
            } else {
                ErrorHandler.showInfo("AppDb: Базовые данные уже существуют, пропускаем заполнение")
                ErrorHandler.showInfo("AppDb: Найдено шаблонов: $templatesCount")
            }
                
                AppLogger.log("AppDb", "Database tables created successfully")
            } else {
                // Миграция: добавляем колонку description к существующим таблицам documents
                try {
                    val descriptionColumnExists = db.rawQuery("PRAGMA table_info(documents)", null).use { tableInfoCursor ->
                        var exists = false
                        while (tableInfoCursor.moveToNext()) {
                            val columnName = tableInfoCursor.getString(tableInfoCursor.getColumnIndexOrThrow("name"))
                            if (columnName == "description") {
                                exists = true
                                break
                            }
                        }
                        exists
                    }
                    
                    if (!descriptionColumnExists) {
                        ErrorHandler.showInfo("AppDb: Добавляем колонку description к таблице documents...")
                        db.execSQL("ALTER TABLE documents ADD COLUMN description TEXT")
                        ErrorHandler.showInfo("AppDb: Колонка description добавлена успешно")
                    }
                } catch (e: Exception) {
                    ErrorHandler.showInfo("AppDb: Ошибка при миграции: ${e.message}")
                    // Не прерываем выполнение, так как это не критично
                }
            }
        } catch (e: Exception) {
            AppLogger.log("AppDb", "ERROR: Error creating tables: ${e.message}")
            ErrorHandler.showCriticalError("Не удалось создать таблицы базы данных", e)
            throw e
        }
    }

    
    /**
     * Безопасно шифрует значение поля документа
     */
    fun encryptFieldValue(value: String): ByteArray {
        return if (value.isNotEmpty()) {
            dbEncryption.encryptBytes(value.toByteArray(Charsets.UTF_8))
        } else {
            ByteArray(0)
        }
    }
    
    /**
     * Безопасно расшифровывает значение поля документа
     */
    fun decryptFieldValue(encryptedValue: ByteArray): String {
        return if (encryptedValue.isNotEmpty()) {
            val decryptedBytes = dbEncryption.decryptBytes(encryptedValue)
            String(decryptedBytes, Charsets.UTF_8)
        } else {
            ""
        }
    }
    
    /**
     * Безопасно шифрует название документа
     */
    fun encryptDocumentName(name: String): String {
        return if (name.isNotEmpty()) {
            dbEncryption.encryptString(name)
        } else {
            name
        }
    }
    
    /**
     * Безопасно расшифровывает название документа
     */
    fun decryptDocumentName(encryptedName: String): String {
        return if (encryptedName.isNotEmpty()) {
            dbEncryption.decryptString(encryptedName)
        } else {
            encryptedName
        }
    }

    private fun seedBasics(db: SQLCipherDatabase) {
        val ts = now()
        fun insTpl(name: String, fields: List<String>): String {
            val tplId = newId()
            db.execSQL(
                "INSERT INTO templates(id,name,is_pinned,pinned_order,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                arrayOf<Any?>(tplId, name, 0, null, ts, ts)
            )
            fields.forEachIndexed { idx, f ->
                db.execSQL(
                    "INSERT INTO template_fields(id,template_id,name,type,ord) VALUES(?,?,?,?,?)",
                    arrayOf<Any>(newId(), tplId, f, "text", idx)
                )
            }
            return tplId
        }
        insTpl("Паспорт", listOf("Серия", "Номер", "Кем выдан", "Дата выдачи", "Код подразделения"))
        insTpl("Карточка", listOf("Номер", "Годен до", "CVC"))

        val p1 = newId()
        db.execSQL("INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
            arrayOf<Any?>(p1, null, "ЛИЧНЫЕ ДАННЫЕ", 0))
        val p2 = newId()
        db.execSQL("INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
            arrayOf<Any?>(p2, null, "КАРТОЧКИ", 1))
    }

}

/* ==================== DAO interfaces ==================== */

interface TemplateDao {
    suspend fun list(): List<Template>
    suspend fun get(id: String): Template?
    suspend fun listFields(templateId: String): List<TemplateField>
    suspend fun add(name: String, fields: List<String>): String
    suspend fun delete(id: String)
}

interface FolderDao {
    fun observeTree(): Flow<List<Folder>>
    fun list(): List<Folder>
    suspend fun add(name: String, parentId: String?): String
    suspend fun delete(id: String)
    fun emitTree()
}

interface DocumentDao {
    fun observeHome(): Flow<DocumentRepository.HomeList>
    fun emitHome()

    suspend fun create(
        templateId: String?, folderId: String?, name: String, description: String,
        fields: List<Pair<String, String>>, photoUris: List<String>, pdfUris: List<String>
    ): String

    suspend fun createWithNames(
        templateId: String?, folderId: String?, name: String, description: String,
        fields: List<Pair<String, String>>, photoFiles: List<Pair<String, String>>, pdfFiles: List<Pair<String, String>>
    ): String

    suspend fun getFull(id: String): DocumentRepository.FullDocument?
    suspend fun update(doc: Document, fields: List<DocumentField>, attachments: List<Attachment>)
    suspend fun delete(id: String)
    suspend fun pin(id: String, pinned: Boolean)
    suspend fun touch(id: String)

    suspend fun move(id: String, folderId: String?)
    suspend fun swapPinned(aId: String, bId: String)
    suspend fun getAllDocumentIds(): List<String> // Для миграции
    suspend fun getDocumentsInFolder(folderId: String): List<Document>
}

interface SettingsDao {
    suspend fun isPinSet(): Boolean
    suspend fun get(): Settings
    suspend fun updatePin(hash: ByteArray)
    suspend fun clearPin()
}

/* ==================== Helpers ==================== */

private fun Cursor.getStringOrNull(column: String): String? =
    getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) null else getString(it) }

/* ==================== Templates ==================== */

class TemplateDaoSql(private val db: AppDb) : TemplateDao {
    override suspend fun list(): List<Template> = withContext(Dispatchers.IO) {
        val res = mutableListOf<Template>()
        db.encryptedReadableDatabase.rawQuery("SELECT * FROM templates ORDER BY name", null).use { c ->
            while (c.moveToNext()) {
                res.add(
                    Template(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        isPinned = c.getInt(c.getColumnIndexOrThrow("is_pinned")) == 1,
                        pinnedOrder = c.getStringOrNull("pinned_order")?.toInt(),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                        updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"))
                    )
                )
            }
        }
        res
    }

    override suspend fun get(id: String): Template? = withContext(Dispatchers.IO) {
        db.encryptedReadableDatabase.rawQuery("SELECT * FROM templates WHERE id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) {
                return@withContext Template(
                    id = c.getString(c.getColumnIndexOrThrow("id")),
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    isPinned = c.getInt(c.getColumnIndexOrThrow("is_pinned")) == 1,
                    pinnedOrder = c.getStringOrNull("pinned_order")?.toInt(),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                    updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"))
                )
            }
        }
        null
    }

    override suspend fun listFields(templateId: String): List<TemplateField> = withContext(Dispatchers.IO) {
        val res = mutableListOf<TemplateField>()
        db.encryptedReadableDatabase.rawQuery(
            "SELECT * FROM template_fields WHERE template_id=? ORDER BY ord",
            arrayOf(templateId)
        ).use { c ->
            while (c.moveToNext()) {
                res.add(
                    TemplateField(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        templateId = c.getString(c.getColumnIndexOrThrow("template_id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        type = FieldType.text,
                        ord = c.getInt(c.getColumnIndexOrThrow("ord"))
                    )
                )
            }
        }
        res
    }

    override suspend fun add(name: String, fields: List<String>): String = withContext(Dispatchers.IO) {
        val ts = now()
        val tplId = newId()
        db.encryptedWritableDatabase.beginTransaction()
        try {
            db.encryptedWritableDatabase.execSQL(
                "INSERT INTO templates(id,name,is_pinned,pinned_order,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                arrayOf<Any?>(tplId, name, 0, null, ts, ts)
            )
            fields.forEachIndexed { idx, f ->
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO template_fields(id,template_id,name,type,ord) VALUES(?,?,?,?,?)",
                    arrayOf<Any>(newId(), tplId, f, "text", idx)
                )
            }
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
        tplId
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.beginTransaction()
        try {
            // Удаляем поля шаблона
            db.encryptedWritableDatabase.execSQL("DELETE FROM template_fields WHERE template_id=?", arrayOf(id))
            // Удаляем сам шаблон
            db.encryptedWritableDatabase.execSQL("DELETE FROM templates WHERE id=?", arrayOf(id))
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
    }
}

/* ==================== Folders ==================== */

class FolderDaoSql(private val db: AppDb) : FolderDao {
    private val tree = MutableStateFlow<List<Folder>>(emptyList())
    override fun observeTree(): Flow<List<Folder>> = tree.asStateFlow()

    override fun list(): List<Folder> {
        val res = mutableListOf<Folder>()
        db.encryptedReadableDatabase.rawQuery("SELECT * FROM folders ORDER BY ord", null).use { c ->
            while (c.moveToNext()) {
                res.add(
                    Folder(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        parentId = c.getStringOrNull("parent_id"),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        ord = c.getInt(c.getColumnIndexOrThrow("ord"))
                    )
                )
            }
        }
        return res
    }

    override suspend fun add(name: String, parentId: String?): String = withContext(Dispatchers.IO) {
        val id = newId()
        val ord = (list().maxOfOrNull { it.ord } ?: -1) + 1
        db.encryptedWritableDatabase.execSQL(
            "INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
            arrayOf<Any?>(id, parentId, name, ord)
        )
        emitTree()
        id
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.execSQL("DELETE FROM folders WHERE id=?", arrayOf(id))
        emitTree()
    }

    override fun emitTree() {
        tree.value = runCatching { list() }.getOrDefault(emptyList())
    }
}

/* ==================== Documents ==================== */

class DocumentDaoSql(private val db: AppDb) : DocumentDao {
    private val home = MutableStateFlow(DocumentRepository.HomeList(emptyList(), emptyList()))
    override fun observeHome(): Flow<DocumentRepository.HomeList> = home.asStateFlow()

    override fun emitHome() {
        val pinned = mutableListOf<Document>()
        val recent = mutableListOf<Document>()
        db.encryptedReadableDatabase.rawQuery(
            "SELECT * FROM documents WHERE is_pinned=1 ORDER BY pinned_order", null
        ).use { c -> while (c.moveToNext()) pinned.add(c.toDoc()) }
        db.encryptedReadableDatabase.rawQuery(
            "SELECT * FROM documents WHERE is_pinned=0 ORDER BY last_opened_at DESC", null
        ).use { c -> while (c.moveToNext()) recent.add(c.toDoc()) }
        home.value = DocumentRepository.HomeList(pinned, recent)
    }

    private fun Cursor.toDoc(): Document {
        val encryptedName = getString(getColumnIndexOrThrow("name"))
        val decryptedName = db.decryptDocumentName(encryptedName)
        
        val encryptedDescription = getStringOrNull("description") ?: ""
        val decryptedDescription = if (encryptedDescription.isNotEmpty()) {
            db.decryptDocumentName(encryptedDescription)
        } else {
            ""
        }
        
        return Document(
            id = getString(getColumnIndexOrThrow("id")),
            templateId = getStringOrNull("template_id"),
            folderId = getStringOrNull("folder_id"),
            name = decryptedName,
            description = decryptedDescription,
            isPinned = getInt(getColumnIndexOrThrow("is_pinned")) == 1,
            pinnedOrder = getStringOrNull("pinned_order")?.toInt(),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
            lastOpenedAt = getLong(getColumnIndexOrThrow("last_opened_at"))
        )
    }

    override suspend fun create(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoUris: List<String>,
        pdfUris: List<String>
    ): String = withContext(Dispatchers.IO) {
        val id = newId()
        val ts = now()
        db.encryptedWritableDatabase.beginTransaction()
        try {
            val encryptedName = db.encryptDocumentName(name)
            val encryptedDescription = db.encryptDocumentName(description)
            db.encryptedWritableDatabase.execSQL(
                "INSERT INTO documents(id,template_id,folder_id,name,description,is_pinned,pinned_order,created_at,updated_at,last_opened_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(id, templateId, folderId, encryptedName, encryptedDescription, 0, null, ts, ts, ts)
            )
            fields.forEachIndexed { index, (title, value) ->
                val encryptedValue = db.encryptFieldValue(value)
                val preview = if (value.isNotEmpty()) value.take(8) else ""
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO document_fields(id,document_id,name,value,preview,is_secret,ord) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any>(newId(), id, title, encryptedValue, preview, 0, index)
                )
            }
            photoUris.forEach { p ->
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,display_name,uri,created_at) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any?>(newId(), id, "photo", null, null, p, ts)
                )
            }
            pdfUris.forEach { pdfUri ->
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,display_name,uri,created_at) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any?>(newId(), id, "pdfs", "document.pdfs", null, pdfUri, ts)
                )
            }
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
        emitHome()
        id
    }

    override suspend fun createWithNames(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<String, String>>, // URI, displayName
        pdfFiles: List<Pair<String, String>> // URI, displayName
    ): String = withContext(Dispatchers.IO) {
        val id = newId()
        val ts = now()
        db.encryptedWritableDatabase.beginTransaction()
        try {
            val encryptedName = db.encryptDocumentName(name)
            val encryptedDescription = db.encryptDocumentName(description)
            db.encryptedWritableDatabase.execSQL(
                "INSERT INTO documents(id,template_id,folder_id,name,description,is_pinned,pinned_order,created_at,updated_at,last_opened_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(id, templateId, folderId, encryptedName, encryptedDescription, 0, null, ts, ts, ts)
            )
            fields.forEachIndexed { index, (title, value) ->
                val encryptedValue = db.encryptFieldValue(value)
                val preview = value.take(8)
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO document_fields(id,document_id,name,value,preview,is_secret,ord) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any>(newId(), id, title, encryptedValue, preview, 0, index)
                )
            }
            photoFiles.forEachIndexed { index, (uri, displayName) ->
                ErrorHandler.showInfo("DocumentDao: Сохраняем фото $index: $displayName")
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,display_name,uri,created_at) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any?>(newId(), id, "photo", null, displayName, uri, ts)
                )
            }
            pdfFiles.forEachIndexed { index, (uri, displayName) ->
                ErrorHandler.showInfo("DocumentDao: Сохраняем PDF $index: $displayName")
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,display_name,uri,created_at) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any?>(newId(), id, "pdfs", "document.pdfs", displayName, uri, ts)
                )
            }
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
        emitHome()
        id
    }

    private fun String.encode(): ByteArray = this.toByteArray(Charsets.UTF_8)

    override suspend fun getFull(id: String): DocumentRepository.FullDocument? = withContext(Dispatchers.IO) {
        var doc: Document? = null
        val fields = mutableListOf<DocumentField>()
        val photos = mutableListOf<Attachment>()
        val pdfs = mutableListOf<Attachment>()

        db.encryptedReadableDatabase.rawQuery("SELECT * FROM documents WHERE id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) doc = c.toDoc()
        }
        if (doc == null) return@withContext null

        db.encryptedReadableDatabase.rawQuery(
            "SELECT * FROM document_fields WHERE document_id=? ORDER BY ord",
            arrayOf(id)
        ).use { c ->
            while (c.moveToNext()) {
                val encryptedValue = c.getBlob(c.getColumnIndexOrThrow("value"))
                val decryptedValue = if (encryptedValue != null && encryptedValue.isNotEmpty()) {
                    db.dbEncryption.decryptBytes(encryptedValue)
                } else {
                    ByteArray(0)
                }
                
                fields.add(
                    DocumentField(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        documentId = id,
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        valueCipher = decryptedValue,
                        preview = c.getStringOrNull("preview"),
                        isSecret = c.getInt(c.getColumnIndexOrThrow("is_secret")) == 1,
                        ord = c.getInt(c.getColumnIndexOrThrow("ord"))
                    )
                )
            }
        }
        db.encryptedReadableDatabase.rawQuery(
            "SELECT * FROM attachments_new WHERE docId=?",
            arrayOf(id)
        ).use { c ->
            ErrorHandler.showInfo("DocumentDao: Загружаем вложения для документа: $id")
            while (c.moveToNext()) {
                val mime = c.getString(c.getColumnIndexOrThrow("mime"))
                val uriString = c.getString(c.getColumnIndexOrThrow("uri"))
                val name = c.getString(c.getColumnIndexOrThrow("name"))
                
                ErrorHandler.showInfo("DocumentDao: Найдено вложение: $mime, имя: $name")
                
                // Определяем тип файла по MIME типу
                val kind = when {
                    mime.startsWith("image/") -> AttachmentKind.photo
                    mime == "application/pdf" -> AttachmentKind.pdf
                    else -> AttachmentKind.photo // По умолчанию фото
                }
                
                val a = Attachment(
                    id = c.getString(c.getColumnIndexOrThrow("id")),
                    documentId = id,
                    kind = kind,
                    fileName = name,
                    displayName = name,
                    uri = try {
                        Uri.parse(uriString)
                    } catch (e: Exception) {
                        AppLogger.log("Db", "ERROR: Failed to parse URI: $uriString")
                        Uri.parse("file:///invalid")
                    },
                    createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
                )
                when (a.kind) {
                    AttachmentKind.photo -> photos.add(a)
                    AttachmentKind.pdfs -> pdfs.add(a)
                    AttachmentKind.pdf -> pdfs.add(a) // для совместимости со старыми данными
                }
            }
        }
        DocumentRepository.FullDocument(doc ?: throw IllegalStateException("Document not found"), fields, photos, pdfs)
    }

    override suspend fun update(doc: Document, fields: List<DocumentField>, attachments: List<Attachment>) = withContext(Dispatchers.IO) {
        val ts = now()
        db.encryptedWritableDatabase.beginTransaction()
        try {
            val encryptedName = db.encryptDocumentName(doc.name)
            val encryptedDescription = db.encryptDocumentName(doc.description)
            db.encryptedWritableDatabase.execSQL(
                "UPDATE documents SET name=?, description=?, updated_at=? WHERE id=?",
                arrayOf<Any>(encryptedName, encryptedDescription, ts, doc.id)
            )
            db.encryptedWritableDatabase.execSQL("DELETE FROM document_fields WHERE document_id=?", arrayOf(doc.id))
            fields.forEach {
                val encryptedValue = if (it.valueCipher?.isNotEmpty() == true) {
                    db.dbEncryption.encryptBytes(it.valueCipher)
                } else {
                    ByteArray(0)
                }
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO document_fields(id,document_id,name,value,preview,is_secret,ord) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any?>(it.id, doc.id, it.name, encryptedValue, it.preview, if (it.isSecret) 1 else 0, it.ord)
                )
            }
            db.encryptedWritableDatabase.execSQL("DELETE FROM attachments WHERE document_id=?", arrayOf(doc.id))
            attachments.forEach {
                db.encryptedWritableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,display_name,uri,created_at) VALUES(?,?,?,?,?,?,?)",
                    arrayOf<Any?>(
                        it.id,
                        doc.id,
                        it.kind.name,
                        it.fileName,
                        it.displayName,
                        it.uri.toString(),
                        it.createdAt
                    )
                )
            }
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
        emitHome()
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.beginTransaction()
        try {
            db.encryptedWritableDatabase.execSQL("DELETE FROM attachments WHERE document_id=?", arrayOf(id))
            db.encryptedWritableDatabase.execSQL("DELETE FROM document_fields WHERE document_id=?", arrayOf(id))
            db.encryptedWritableDatabase.execSQL("DELETE FROM documents WHERE id=?", arrayOf(id))
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
        emitHome()
    }

    override suspend fun pin(id: String, pinned: Boolean) = withContext(Dispatchers.IO) {
        if (pinned) {
            val maxOrder = db.encryptedReadableDatabase.rawQuery(
                "SELECT MAX(pinned_order) FROM documents WHERE is_pinned=1", null
            ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) else 0 }
            db.encryptedWritableDatabase.execSQL(
                "UPDATE documents SET is_pinned=1, pinned_order=? WHERE id=?",
                arrayOf<Any>(maxOrder + 1, id)
            )
        } else {
            db.encryptedWritableDatabase.execSQL(
                "UPDATE documents SET is_pinned=0, pinned_order=NULL WHERE id=?",
                arrayOf<Any>(id)
            )
        }
        emitHome()
    }

    override suspend fun touch(id: String) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.execSQL(
            "UPDATE documents SET last_opened_at=? WHERE id=?",
            arrayOf<Any>(now(), id)
        )
        emitHome()
    }

    override suspend fun move(id: String, folderId: String?) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.execSQL(
            "UPDATE documents SET folder_id=? WHERE id=?",
            arrayOf<Any?>(folderId, id)
        )
        emitHome()
    }

    override suspend fun swapPinned(aId: String, bId: String) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.beginTransaction()
        try {
            ensureSequentialPinnedOrders_NoThrow()
            val orders = mutableMapOf<String, Int>()
            db.encryptedReadableDatabase.rawQuery(
                "SELECT id, pinned_order FROM documents WHERE is_pinned=1", null
            ).use { c -> while (c.moveToNext()) orders[c.getString(0)] = c.getInt(1) }
            val aOrd = orders[aId] ?: return@withContext
            val bOrd = orders[bId] ?: return@withContext
            db.encryptedWritableDatabase.execSQL("UPDATE documents SET pinned_order=? WHERE id=?", arrayOf<Any>(-1, aId))
            db.encryptedWritableDatabase.execSQL("UPDATE documents SET pinned_order=? WHERE id=?", arrayOf<Any>(aOrd, bId))
            db.encryptedWritableDatabase.execSQL("UPDATE documents SET pinned_order=? WHERE id=?", arrayOf<Any>(bOrd, aId))
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
        emitHome()
    }

    override suspend fun getAllDocumentIds(): List<String> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<String>()
        db.encryptedReadableDatabase.rawQuery("SELECT id FROM documents", null).use { c ->
            while (c.moveToNext()) {
                ids.add(c.getString(0))
            }
        }
        ids
    }

    override suspend fun getDocumentsInFolder(folderId: String): List<Document> = withContext(Dispatchers.IO) {
        val docs = mutableListOf<Document>()
        db.encryptedReadableDatabase.rawQuery(
            "SELECT * FROM documents WHERE folder_id=? ORDER BY created_at DESC", 
            arrayOf(folderId)
        ).use { c -> 
            while (c.moveToNext()) {
                docs.add(c.toDoc())
            }
        }
        docs
    }

    private fun ensureSequentialPinnedOrders_NoThrow() {
        try {
            val ids = mutableListOf<String>()
            db.encryptedReadableDatabase.rawQuery(
                "SELECT id FROM documents WHERE is_pinned=1 ORDER BY pinned_order", null
            ).use { c -> while (c.moveToNext()) ids.add(c.getString(0)) }
            db.encryptedWritableDatabase.beginTransaction()
            ids.forEachIndexed { index, id ->
                db.encryptedWritableDatabase.execSQL(
                    "UPDATE documents SET pinned_order=? WHERE id=?",
                    arrayOf<Any>(index + 1, id)
                )
            }
            db.encryptedWritableDatabase.setTransactionSuccessful()
            db.encryptedWritableDatabase.endTransaction()
        } catch (_: Exception) { }
    }
}

/* ==================== Settings ==================== */

class SettingsDaoSql(private val db: AppDb) : SettingsDao {

    init { ensureRow() }

    private fun ensureRow() {
        db.encryptedReadableDatabase.rawQuery("SELECT COUNT(*) FROM settings WHERE id=1", null).use { c ->
            val exists = if (c.moveToFirst()) c.getInt(0) > 0 else false
            if (!exists) {
                val tsDb = db.encryptedWritableDatabase
                tsDb.execSQL("INSERT INTO settings(id,pin_hash,pin_salt,db_key_salt,version) VALUES(1, x'', x'', x'', '1.0.0')")
            }
        }
    }

    override suspend fun isPinSet(): Boolean = withContext(Dispatchers.IO) {
        db.encryptedReadableDatabase.rawQuery("SELECT length(pin_hash) FROM settings WHERE id=1", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) > 0 else false }
    }

    override suspend fun get(): Settings = withContext(Dispatchers.IO) {
        db.encryptedReadableDatabase.rawQuery("SELECT * FROM settings WHERE id=1", null).use { c ->
            if (c.moveToFirst()) {
                return@withContext Settings(
                    version = c.getString(c.getColumnIndexOrThrow("version")),
                    pinHash = c.getBlob(c.getColumnIndexOrThrow("pin_hash")),
                    pinSalt = c.getBlob(c.getColumnIndexOrThrow("pin_salt")),
                    dbKeySalt = c.getBlob(c.getColumnIndexOrThrow("db_key_salt"))
                )
            }
        }
        // вместо error(...) — создаём строку и возвращаем дефолт
        val w = db.encryptedWritableDatabase
        w.execSQL("INSERT OR IGNORE INTO settings(id,pin_hash,pin_salt,db_key_salt,version) VALUES(1, x'', x'', x'', '1.0.0')")
        Settings(version = "1.0.0", pinHash = ByteArray(0), pinSalt = ByteArray(0), dbKeySalt = ByteArray(0))
    }

    override suspend fun updatePin(hash: ByteArray) = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.beginTransaction()
        try {
            val cv = ContentValues().apply { put("pin_hash", hash) }
            db.encryptedWritableDatabase.update("settings", cv, "id=1", null)
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
    }

    override suspend fun clearPin() = withContext(Dispatchers.IO) {
        db.encryptedWritableDatabase.beginTransaction()
        try {
            val cv = ContentValues().apply { put("pin_hash", ByteArray(0)) } // пустой BLOB, НЕ null
            db.encryptedWritableDatabase.update("settings", cv, "id=1", null)
            db.encryptedWritableDatabase.setTransactionSuccessful()
        } finally {
            db.encryptedWritableDatabase.endTransaction()
        }
    }
}
