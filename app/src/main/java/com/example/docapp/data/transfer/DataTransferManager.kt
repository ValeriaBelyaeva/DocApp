package com.example.docapp.data.transfer
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.docapp.core.AppLogger
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.newId
import com.example.docapp.data.AppDb
import com.example.docapp.data.SqlDaoFactory
import com.example.docapp.data.db.entities.AttachmentEntity
import com.example.docapp.domain.Document
import com.example.docapp.domain.DocumentField
import com.example.docapp.domain.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.database.sqlite.SQLiteDatabase
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
class DataTransferManager(
    private val context: Context,
    private val db: AppDb,
    private val dao: SqlDaoFactory
) {
    data class ExportResult(val file: File, val documents: Int, val attachments: Int)
    data class ImportResult(val documents: Int, val attachments: Int)
    private data class AttachmentSnapshot(
        val metadata: AttachmentEntity,
        val sourceFile: File,
        val entryName: String
    )
    private data class DocumentSnapshot(
        val document: Document,
        val fields: List<DocumentField>,
        val attachments: List<AttachmentSnapshot>
    )
    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments").also { if (!it.exists()) it.mkdirs() }
    suspend fun exportBackup(password: String?): ExportResult = withContext(Dispatchers.IO) {
        AppLogger.log("DataTransfer", "Starting export...")
        val folders = dao.folders.list()
        val documentIds = dao.documents.getAllDocumentIds()
        val snapshots = mutableListOf<DocumentSnapshot>()
        var totalAttachments = 0
        documentIds.forEach { docId ->
            val full = dao.documents.getFull(docId) ?: return@forEach
            val attachmentEntities = dao.attachments.listByDoc(docId)
            val attachmentSnapshots = attachmentEntities.mapNotNull { entity ->
                val file = File(entity.path)
                if (!file.exists()) {
                    AppLogger.log("DataTransfer", "Attachment file missing: ${entity.path}")
                    null
                } else {
                    val safeName = sanitizeFileName("${entity.id}_${entity.name}")
                    AttachmentSnapshot(entity, file, "attachments/$safeName")
                }
            }
            totalAttachments += attachmentSnapshots.size
            snapshots += DocumentSnapshot(full.doc, full.fields, attachmentSnapshots)
        }
        val payload = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("folders", folders.serializeFolders())
            put("documents", snapshots.serializeDocuments())
        }
        val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val fileName = "docapp-backup-${timestamp()}.zip"
        val backupFile = File(exportDir, fileName)
        val tempJsonFile = File(context.cacheDir, "backup-${System.currentTimeMillis()}.json")
        tempJsonFile.writeText(payload.toString(2), Charsets.UTF_8)
        val isEncrypted = password != null && password.isNotEmpty()
        val passwordChars = if (isEncrypted) password!!.toCharArray() else null
        AppLogger.log("DataTransfer", "Creating ZIP file with encryption: $isEncrypted, password length: ${passwordChars?.size ?: 0}")
        val zipFile = if (isEncrypted && passwordChars != null) {
            AppLogger.log("DataTransfer", "Creating encrypted ZIP with password (length: ${passwordChars.size})")
            Zip4jFile(backupFile, passwordChars)
        } else {
            AppLogger.log("DataTransfer", "Creating unencrypted ZIP")
            Zip4jFile(backupFile)
        }
        val jsonParams = ZipParameters().apply {
            fileNameInZip = "backup.json"
            if (isEncrypted) {
                AppLogger.log("DataTransfer", "Setting AES encryption for backup.json")
                setEncryptFiles(true)
                encryptionMethod = EncryptionMethod.AES
            } else {
                setEncryptFiles(false)
                encryptionMethod = EncryptionMethod.NONE
            }
        }
        zipFile.addFile(tempJsonFile, jsonParams)
        AppLogger.log("DataTransfer", "Added backup.json to ZIP with encryption: ${jsonParams.encryptionMethod}, encryptFiles: ${jsonParams.isEncryptFiles}")
        var attachmentCount = 0
        snapshots.flatMap { it.attachments }.forEach { snap ->
            val fileParams = ZipParameters().apply {
                fileNameInZip = snap.entryName
                if (isEncrypted) {
                    setEncryptFiles(true)
                    encryptionMethod = EncryptionMethod.AES
                } else {
                    setEncryptFiles(false)
                    encryptionMethod = EncryptionMethod.NONE
                }
            }
            zipFile.addFile(snap.sourceFile, fileParams)
            attachmentCount++
        }
        AppLogger.log("DataTransfer", "Added $attachmentCount attachment files to ZIP")
        tempJsonFile.delete()
        AppLogger.log(
            "DataTransfer",
            "Export completed: ${snapshots.size} docs, $totalAttachments files -> ${backupFile.absolutePath}"
        )
        ErrorHandler.showSuccess("Backup saved to ${backupFile.absolutePath}")
        ExportResult(backupFile, snapshots.size, totalAttachments)
    }
    suspend fun importBackup(sourceUri: Uri, password: String?): ImportResult = withContext(Dispatchers.IO) {
        AppLogger.log("DataTransfer", "Importing backup from $sourceUri")
        val tempZip = File(context.cacheDir, "import-${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(tempZip).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Cannot read backup file")
        val zipFile = Zip4jFile(tempZip)
        if (password != null && password.isNotEmpty()) {
            zipFile.setPassword(password.toCharArray())
        }
        val fileHeaders = zipFile.fileHeaders
        val payloadHeader = fileHeaders.find { it.fileName == "backup.json" }
            ?: throw IllegalStateException("Invalid backup: missing backup.json")
        val isEncrypted = payloadHeader.isEncrypted
        if (isEncrypted && (password == null || password.isEmpty())) {
            throw SecurityException("Backup file is password protected")
        }
        val tempJsonFile = File(context.cacheDir, "backup-${System.currentTimeMillis()}.json")
        zipFile.extractFile(payloadHeader, context.cacheDir.absolutePath, tempJsonFile.name)
        val payload = tempJsonFile.readText(Charsets.UTF_8)
        tempJsonFile.delete()
        val json = JSONObject(payload)
        validateVersion(json)
        val folders = json.getJSONArray("folders")
        val documents = json.getJSONArray("documents")
        clearExistingData()
        importFolders(folders)
        var docCount = 0
        var attachmentCount = 0
        val sqlDb = db.encryptedWritableDatabase
        sqlDb.beginTransaction()
        try {
            for (i in 0 until documents.length()) {
                val docJson = documents.getJSONObject(i)
                val docId = docJson.getString("id")
                insertDocument(sqlDb, docJson)
                val fields = docJson.getJSONArray("fields")
                for (fi in 0 until fields.length()) {
                    insertField(sqlDb, docId, fields.getJSONObject(fi))
                }
                    val attachments = docJson.getJSONArray("attachments")
                    for (ai in 0 until attachments.length()) {
                        val attachmentJson = attachments.getJSONObject(ai)
                        val prepared = extractAttachment(zipFile, attachmentJson)
                        insertAttachment(sqlDb, docId, prepared)
                        attachmentCount++
                    }
                docCount++
            }
            sqlDb.setTransactionSuccessful()
        } finally {
            sqlDb.endTransaction()
        }
        dao.documents.emitHome()
        dao.folders.emitTree()
        ErrorHandler.showSuccess("Backup imported: $docCount docs, $attachmentCount files")
        ImportResult(docCount, attachmentCount)
    }
    private fun validateVersion(json: JSONObject) {
        val version = json.optInt("version", 1)
        if (version != 1) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }
    }
    private fun JSONArray.serializeFolders(): JSONArray {
        return this
    }
    private fun List<Folder>.serializeFolders(): JSONArray {
        val array = JSONArray()
        forEach { folder ->
            array.put(
                JSONObject().apply {
                    put("id", folder.id)
                    put("parentId", folder.parentId ?: JSONObject.NULL)
                    put("name", folder.name)
                    put("ord", folder.ord)
                }
            )
        }
        return array
    }
    private fun List<DocumentSnapshot>.serializeDocuments(): JSONArray {
        val array = JSONArray()
        forEach { snapshot ->
            array.put(
                JSONObject().apply {
                    put("id", snapshot.document.id)
                    put("templateId", snapshot.document.templateId ?: JSONObject.NULL)
                    put("folderId", snapshot.document.folderId ?: JSONObject.NULL)
                    put("name", snapshot.document.name)
                    put("description", snapshot.document.description)
                    put("isPinned", snapshot.document.isPinned)
                    put("pinnedOrder", snapshot.document.pinnedOrder ?: JSONObject.NULL)
                    put("createdAt", snapshot.document.createdAt)
                    put("updatedAt", snapshot.document.updatedAt)
                    put("lastOpenedAt", snapshot.document.lastOpenedAt)
                    put("fields", snapshot.fields.serializeFields())
                    put("attachments", snapshot.attachments.serializeAttachments())
                }
            )
        }
        return array
    }
    private fun List<DocumentField>.serializeFields(): JSONArray {
        val array = JSONArray()
        forEach { field ->
            array.put(
                JSONObject().apply {
                    put("id", field.id)
                    put("name", field.name)
                    put("value", field.valueCipher?.toString(Charsets.UTF_8) ?: "")
                    put("preview", field.preview ?: "")
                    put("isSecret", field.isSecret)
                    put("ord", field.ord)
                }
            )
        }
        return array
    }
    private fun List<AttachmentSnapshot>.serializeAttachments(): JSONArray {
        val array = JSONArray()
        forEach { snapshot ->
            array.put(
                JSONObject().apply {
                    put("id", snapshot.metadata.id)
                    put("name", snapshot.metadata.name)
                    put("mime", snapshot.metadata.mime)
                    put("size", snapshot.metadata.size)
                    put("sha256", snapshot.metadata.sha256)
                    put("createdAt", snapshot.metadata.createdAt)
                    put("file", snapshot.entryName)
                }
            )
        }
        return array
    }
    private fun clearExistingData() {
        AppLogger.log("DataTransfer", "Clearing current data before import...")
        val sqlDb = db.encryptedWritableDatabase
        sqlDb.beginTransaction()
        try {
            sqlDb.execSQL("DELETE FROM attachments_new")
            sqlDb.execSQL("DELETE FROM document_fields")
            sqlDb.execSQL("DELETE FROM documents")
            sqlDb.execSQL("DELETE FROM folders")
            sqlDb.execSQL("DELETE FROM attachments")
            sqlDb.setTransactionSuccessful()
        } finally {
            sqlDb.endTransaction()
        }
        attachmentsDir.listFiles()?.forEach { it.delete() }
    }
    private fun importFolders(folders: JSONArray) {
        val sqlDb = db.encryptedWritableDatabase
        sqlDb.beginTransaction()
        try {
            for (i in 0 until folders.length()) {
                val folder = folders.getJSONObject(i)
                sqlDb.execSQL(
                    "INSERT INTO folders(id,parent_id,name,ord) VALUES(?,?,?,?)",
                    arrayOf<Any?>(
                        folder.getString("id"),
                        folder.optString("parentId").takeIf { it.isNotEmpty() },
                        folder.getString("name"),
                        folder.optInt("ord", i)
                    )
                )
            }
            sqlDb.setTransactionSuccessful()
        } finally {
            sqlDb.endTransaction()
        }
    }
    private fun insertDocument(sqlDb: SQLiteDatabase, docJson: JSONObject) {
        val encryptedName = db.encryptDocumentName(docJson.getString("name"))
        val encryptedDescription = db.encryptDocumentName(docJson.optString("description"))
        sqlDb.execSQL(
            """
            INSERT INTO documents(
                id, template_id, folder_id, name, description,
                is_pinned, pinned_order, created_at, updated_at, last_opened_at
            ) VALUES(?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(),
            arrayOf(
                docJson.getString("id"),
                docJson.optString("templateId").takeIf { it.isNotEmpty() },
                docJson.optString("folderId").takeIf { it.isNotEmpty() },
                encryptedName,
                encryptedDescription,
                if (docJson.optBoolean("isPinned")) 1 else 0,
                docJson.opt("pinnedOrder").takeUnless { it == JSONObject.NULL },
                docJson.optLong("createdAt"),
                docJson.optLong("updatedAt"),
                docJson.optLong("lastOpenedAt")
            )
        )
    }
    private fun insertField(sqlDb: SQLiteDatabase, docId: String, field: JSONObject) {
        val value = field.optString("value")
        val encryptedValue = db.encryptFieldValue(value)
        sqlDb.execSQL(
            """
            INSERT INTO document_fields(id,document_id,name,value,preview,is_secret,ord)
            VALUES(?,?,?,?,?,?,?)
            """.trimIndent(),
            arrayOf(
                field.optString("id").takeIf { it.isNotEmpty() } ?: newId(),
                docId,
                field.getString("name"),
                encryptedValue,
                field.optString("preview"),
                if (field.optBoolean("isSecret")) 1 else 0,
                field.optInt("ord")
            )
        )
    }
    private data class PreparedAttachment(
        val id: String,
        val name: String,
        val mime: String,
        val size: Long,
        val sha256: String,
        val path: String,
        val uri: String,
        val createdAt: Long
    )
    private fun extractAttachment(zip: Zip4jFile, attachment: JSONObject): PreparedAttachment {
        val entryName = attachment.getString("file")
        val fileHeader = zip.fileHeaders.find { it.fileName == entryName }
            ?: throw IllegalStateException("Missing attachment file $entryName")
        val targetName = sanitizeFileName("${attachment.getString("id")}_${attachment.getString("name")}")
        val targetFile = File(attachmentsDir, targetName)
        zip.extractFile(fileHeader, attachmentsDir.absolutePath, targetName)
        val sha = computeSha256(targetFile)
        val expectedSha = attachment.getString("sha256")
        if (!expectedSha.equals(sha, ignoreCase = true)) {
            throw IllegalStateException("SHA mismatch for attachment ${attachment.getString("id")}")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )
        return PreparedAttachment(
            id = attachment.getString("id"),
            name = attachment.getString("name"),
            mime = attachment.getString("mime"),
            size = targetFile.length(),
            sha256 = sha,
            path = targetFile.absolutePath,
            uri = uri.toString(),
            createdAt = attachment.optLong("createdAt")
        )
    }
    private fun insertAttachment(
        sqlDb: SQLiteDatabase,
        docId: String,
        prepared: PreparedAttachment
    ) {
        sqlDb.execSQL(
            """
            INSERT INTO attachments_new(id, docId, name, mime, size, sha256, path, uri, createdAt)
            VALUES(?,?,?,?,?,?,?,?,?)
            """.trimIndent(),
            arrayOf(
                prepared.id,
                docId,
                prepared.name,
                prepared.mime,
                prepared.size,
                prepared.sha256,
                prepared.path,
                prepared.uri,
                prepared.createdAt
            )
        )
    }
    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
    private fun timestamp(): String {
        val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return formatter.format(Date())
    }
}
