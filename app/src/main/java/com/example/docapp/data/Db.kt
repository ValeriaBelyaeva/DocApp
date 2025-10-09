package com.example.docapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import com.example.docapp.core.newId
import com.example.docapp.core.now
import com.example.docapp.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/* ==================== SQLiteOpenHelper + seed ==================== */

class AppDb(ctx: Context) : SQLiteOpenHelper(ctx, "docapp.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.execSQL("PRAGMA foreign_keys=ON")
    }

    override fun onCreate(db: SQLiteDatabase) {
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
            name TEXT NOT NULL, is_pinned INTEGER NOT NULL, pinned_order INTEGER,
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
            file_name TEXT, uri TEXT NOT NULL, created_at INTEGER NOT NULL,
            FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
        )""")
        db.execSQL("""CREATE TABLE settings(
            id INTEGER PRIMARY KEY CHECK (id=1),
            pin_hash BLOB NOT NULL, pin_salt BLOB NOT NULL, db_key_salt BLOB NOT NULL, version TEXT NOT NULL
        )""")
        // строка настроек по умолчанию
        db.execSQL("INSERT INTO settings(id,pin_hash,pin_salt,db_key_salt,version) VALUES(1, x'', x'', x'', '1.0.0')")
        seedBasics(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}

    private fun seedBasics(db: SQLiteDatabase) {
        val ts = now()
        fun insTpl(name: String, fields: List<String>): String {
            val tplId = newId()
            db.execSQL(
                "INSERT INTO templates(id,name,is_pinned,pinned_order,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                arrayOf(tplId, name, 0, null, ts, ts)
            )
            fields.forEachIndexed { idx, f ->
                db.execSQL(
                    "INSERT INTO template_fields(id,template_id,name,type,ord) VALUES(?,?,?,?,?)",
                    arrayOf(newId(), tplId, f, "text", idx)
                )
            }
            return tplId
        }
        insTpl("ПАСПОРТ", listOf("СЕРИЯ", "НОМЕР"))
        insTpl("КАРТОЧКИ", listOf("БАНК", "НОМЕР"))

        val p1 = newId()
        db.execSQL("INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
            arrayOf(p1, null, "ЛИЧНЫЕ ДАННЫЕ", 0))
        val p2 = newId()
        db.execSQL("INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
            arrayOf(p2, null, "КАРТОЧКИ", 1))
    }
}

/* ==================== DAO interfaces ==================== */

interface TemplateDao {
    suspend fun list(): List<Template>
    suspend fun get(id: String): Template?
    suspend fun listFields(templateId: String): List<TemplateField>
    suspend fun add(name: String, fields: List<String>): String
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
        templateId: String?, folderId: String?, name: String,
        fields: List<Pair<String, String>>, photoUris: List<String>, pdfUri: String?
    ): String

    suspend fun getFull(id: String): DocumentRepository.FullDocument?
    suspend fun update(doc: Document, fields: List<DocumentField>, attachments: List<Attachment>)
    suspend fun delete(id: String)
    suspend fun pin(id: String, pinned: Boolean)
    suspend fun touch(id: String)

    suspend fun move(id: String, folderId: String?)
    suspend fun swapPinned(aId: String, bId: String)
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
        db.readableDatabase.rawQuery("SELECT * FROM templates ORDER BY name", null).use { c ->
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
        db.readableDatabase.rawQuery("SELECT * FROM templates WHERE id=?", arrayOf(id)).use { c ->
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
        db.readableDatabase.rawQuery(
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
        db.writableDatabase.beginTransaction()
        try {
            db.writableDatabase.execSQL(
                "INSERT INTO templates(id,name,is_pinned,pinned_order,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                arrayOf(tplId, name, 0, null, ts, ts)
            )
            fields.forEachIndexed { idx, f ->
                db.writableDatabase.execSQL(
                    "INSERT INTO template_fields(id,template_id,name,type,ord) VALUES(?,?,?,?,?)",
                    arrayOf(newId(), tplId, f, "text", idx)
                )
            }
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
        tplId
    }
}

/* ==================== Folders ==================== */

class FolderDaoSql(private val db: AppDb) : FolderDao {
    private val tree = MutableStateFlow<List<Folder>>(emptyList())
    override fun observeTree(): Flow<List<Folder>> = tree.asStateFlow()

    override fun list(): List<Folder> {
        val res = mutableListOf<Folder>()
        db.readableDatabase.rawQuery("SELECT * FROM folders ORDER BY ord", null).use { c ->
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
        db.writableDatabase.execSQL(
            "INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
            arrayOf(id, parentId, name, ord)
        )
        emitTree()
        id
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.execSQL("DELETE FROM folders WHERE id=?", arrayOf(id))
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
        db.readableDatabase.rawQuery(
            "SELECT * FROM documents WHERE is_pinned=1 ORDER BY pinned_order", null
        ).use { c -> while (c.moveToNext()) pinned.add(c.toDoc()) }
        db.readableDatabase.rawQuery(
            "SELECT * FROM documents WHERE is_pinned=0 ORDER BY last_opened_at DESC", null
        ).use { c -> while (c.moveToNext()) recent.add(c.toDoc()) }
        home.value = DocumentRepository.HomeList(pinned, recent)
    }

    private fun Cursor.toDoc(): Document = Document(
        id = getString(getColumnIndexOrThrow("id")),
        templateId = getStringOrNull("template_id"),
        folderId = getStringOrNull("folder_id"),
        name = getString(getColumnIndexOrThrow("name")),
        isPinned = getInt(getColumnIndexOrThrow("is_pinned")) == 1,
        pinnedOrder = getStringOrNull("pinned_order")?.toInt(),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        lastOpenedAt = getLong(getColumnIndexOrThrow("last_opened_at"))
    )

    override suspend fun create(
        templateId: String?,
        folderId: String?,
        name: String,
        fields: List<Pair<String, String>>,
        photoUris: List<String>,
        pdfUri: String?
    ): String = withContext(Dispatchers.IO) {
        val id = newId()
        val ts = now()
        db.writableDatabase.beginTransaction()
        try {
            db.writableDatabase.execSQL(
                "INSERT INTO documents(id,template_id,folder_id,name,is_pinned,pinned_order,created_at,updated_at,last_opened_at) VALUES(?,?,?,?,?,?,?,?,?)",
                arrayOf(id, templateId, folderId, name, 0, null, ts, ts, ts)
            )
            fields.forEachIndexed { index, (title, value) ->
                db.writableDatabase.execSQL(
                    "INSERT INTO document_fields(id,document_id,name,value,preview,is_secret,ord) VALUES(?,?,?,?,?,?,?)",
                    arrayOf(newId(), id, title, value.encode(), value.take(8), 0, index)
                )
            }
            photoUris.forEach { p ->
                db.writableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,uri,created_at) VALUES(?,?,?,?,?,?)",
                    arrayOf(newId(), id, "photo", null, p, ts)
                )
            }
            pdfUri?.let {
                db.writableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,uri,created_at) VALUES(?,?,?,?,?,?)",
                    arrayOf(newId(), id, "pdf", "document.pdf", it, ts)
                )
            }
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
        emitHome()
        id
    }

    private fun String.encode(): ByteArray = this.toByteArray(Charsets.UTF_8)

    override suspend fun getFull(id: String): DocumentRepository.FullDocument? = withContext(Dispatchers.IO) {
        var doc: Document? = null
        val fields = mutableListOf<DocumentField>()
        val photos = mutableListOf<Attachment>()
        var pdf: Attachment? = null

        db.readableDatabase.rawQuery("SELECT * FROM documents WHERE id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) doc = c.toDoc()
        }
        if (doc == null) return@withContext null

        db.readableDatabase.rawQuery(
            "SELECT * FROM document_fields WHERE document_id=? ORDER BY ord",
            arrayOf(id)
        ).use { c ->
            while (c.moveToNext()) {
                fields.add(
                    DocumentField(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        documentId = id,
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        valueCipher = c.getBlob(c.getColumnIndexOrThrow("value")),
                        preview = c.getStringOrNull("preview"),
                        isSecret = c.getInt(c.getColumnIndexOrThrow("is_secret")) == 1,
                        ord = c.getInt(c.getColumnIndexOrThrow("ord"))
                    )
                )
            }
        }
        db.readableDatabase.rawQuery(
            "SELECT * FROM attachments WHERE document_id=?",
            arrayOf(id)
        ).use { c ->
            while (c.moveToNext()) {
                val a = Attachment(
                    id = c.getString(c.getColumnIndexOrThrow("id")),
                    documentId = id,
                    kind = if (c.getString(c.getColumnIndexOrThrow("kind")) == "photo") AttachmentKind.photo else AttachmentKind.pdf,
                    fileName = c.getStringOrNull("file_name"),
                    uri = Uri.parse(c.getString(c.getColumnIndexOrThrow("uri"))),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                )
                when (a.kind) {
                    AttachmentKind.photo -> photos.add(a)
                    AttachmentKind.pdf -> pdf = a
                }
            }
        }
        DocumentRepository.FullDocument(doc!!, fields, photos, pdf)
    }

    override suspend fun update(doc: Document, fields: List<DocumentField>, attachments: List<Attachment>) = withContext(Dispatchers.IO) {
        val ts = now()
        db.writableDatabase.beginTransaction()
        try {
            db.writableDatabase.execSQL(
                "UPDATE documents SET name=?, updated_at=? WHERE id=?",
                arrayOf(doc.name, ts, doc.id)
            )
            db.writableDatabase.execSQL("DELETE FROM document_fields WHERE document_id=?", arrayOf(doc.id))
            fields.forEach {
                db.writableDatabase.execSQL(
                    "INSERT INTO document_fields(id,document_id,name,value,preview,is_secret,ord) VALUES(?,?,?,?,?,?,?)",
                    arrayOf(it.id, doc.id, it.name, it.valueCipher, it.preview, if (it.isSecret) 1 else 0, it.ord)
                )
            }
            db.writableDatabase.execSQL("DELETE FROM attachments WHERE document_id=?", arrayOf(doc.id))
            attachments.forEach {
                db.writableDatabase.execSQL(
                    "INSERT INTO attachments(id,document_id,kind,file_name,uri,created_at) VALUES(?,?,?,?,?,?)",
                    arrayOf(it.id, doc.id, it.kind.name, it.fileName, it.uri.toString(), ts)
                )
            }
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
        emitHome()
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.beginTransaction()
        try {
            db.writableDatabase.execSQL("DELETE FROM attachments WHERE document_id=?", arrayOf(id))
            db.writableDatabase.execSQL("DELETE FROM document_fields WHERE document_id=?", arrayOf(id))
            db.writableDatabase.execSQL("DELETE FROM documents WHERE id=?", arrayOf(id))
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
        emitHome()
    }

    override suspend fun pin(id: String, pinned: Boolean) = withContext(Dispatchers.IO) {
        if (pinned) {
            val maxOrder = db.readableDatabase.rawQuery(
                "SELECT MAX(pinned_order) FROM documents WHERE is_pinned=1", null
            ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) else 0 }
            db.writableDatabase.execSQL(
                "UPDATE documents SET is_pinned=1, pinned_order=? WHERE id=?",
                arrayOf(maxOrder + 1, id)
            )
        } else {
            db.writableDatabase.execSQL(
                "UPDATE documents SET is_pinned=0, pinned_order=NULL WHERE id=?",
                arrayOf(id)
            )
        }
        emitHome()
    }

    override suspend fun touch(id: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.execSQL(
            "UPDATE documents SET last_opened_at=? WHERE id=?",
            arrayOf(now(), id)
        )
        emitHome()
    }

    override suspend fun move(id: String, folderId: String?) = withContext(Dispatchers.IO) {
        db.writableDatabase.execSQL(
            "UPDATE documents SET folder_id=? WHERE id=?",
            arrayOf(folderId, id)
        )
        emitHome()
    }

    override suspend fun swapPinned(aId: String, bId: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.beginTransaction()
        try {
            ensureSequentialPinnedOrders_NoThrow()
            val orders = mutableMapOf<String, Int>()
            db.readableDatabase.rawQuery(
                "SELECT id, pinned_order FROM documents WHERE is_pinned=1", null
            ).use { c -> while (c.moveToNext()) orders[c.getString(0)] = c.getInt(1) }
            val aOrd = orders[aId] ?: return@withContext
            val bOrd = orders[bId] ?: return@withContext
            db.writableDatabase.execSQL("UPDATE documents SET pinned_order=? WHERE id=?", arrayOf(-1, aId))
            db.writableDatabase.execSQL("UPDATE documents SET pinned_order=? WHERE id=?", arrayOf(aOrd, bId))
            db.writableDatabase.execSQL("UPDATE documents SET pinned_order=? WHERE id=?", arrayOf(bOrd, aId))
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
        emitHome()
    }

    private fun ensureSequentialPinnedOrders_NoThrow() {
        try {
            val ids = mutableListOf<String>()
            db.readableDatabase.rawQuery(
                "SELECT id FROM documents WHERE is_pinned=1 ORDER BY pinned_order", null
            ).use { c -> while (c.moveToNext()) ids.add(c.getString(0)) }
            db.writableDatabase.beginTransaction()
            ids.forEachIndexed { index, id ->
                db.writableDatabase.execSQL(
                    "UPDATE documents SET pinned_order=? WHERE id=?",
                    arrayOf(index + 1, id)
                )
            }
            db.writableDatabase.setTransactionSuccessful()
            db.writableDatabase.endTransaction()
        } catch (_: Exception) { }
    }
}

/* ==================== Settings ==================== */

class SettingsDaoSql(private val db: AppDb) : SettingsDao {

    init { ensureRow() }

    private fun ensureRow() {
        db.readableDatabase.rawQuery("SELECT COUNT(*) FROM settings WHERE id=1", null).use { c ->
            val exists = if (c.moveToFirst()) c.getInt(0) > 0 else false
            if (!exists) {
                val tsDb = db.writableDatabase
                tsDb.execSQL("INSERT INTO settings(id,pin_hash,pin_salt,db_key_salt,version) VALUES(1, x'', x'', x'', '1.0.0')")
            }
        }
    }

    override suspend fun isPinSet(): Boolean = withContext(Dispatchers.IO) {
        db.readableDatabase.rawQuery("SELECT length(pin_hash) FROM settings WHERE id=1", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) > 0 else false }
    }

    override suspend fun get(): Settings = withContext(Dispatchers.IO) {
        db.readableDatabase.rawQuery("SELECT * FROM settings WHERE id=1", null).use { c ->
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
        val w = db.writableDatabase
        w.execSQL("INSERT OR IGNORE INTO settings(id,pin_hash,pin_salt,db_key_salt,version) VALUES(1, x'', x'', x'', '1.0.0')")
        Settings(version = "1.0.0", pinHash = ByteArray(0), pinSalt = ByteArray(0), dbKeySalt = ByteArray(0))
    }

    override suspend fun updatePin(hash: ByteArray) = withContext(Dispatchers.IO) {
        db.writableDatabase.beginTransaction()
        try {
            val cv = ContentValues().apply { put("pin_hash", hash) }
            db.writableDatabase.update("settings", cv, "id=1", null)
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
    }

    override suspend fun clearPin() = withContext(Dispatchers.IO) {
        db.writableDatabase.beginTransaction()
        try {
            val cv = ContentValues().apply { put("pin_hash", ByteArray(0)) } // пустой BLOB, НЕ null
            db.writableDatabase.update("settings", cv, "id=1", null)
            db.writableDatabase.setTransactionSuccessful()
        } finally {
            db.writableDatabase.endTransaction()
        }
    }
}
