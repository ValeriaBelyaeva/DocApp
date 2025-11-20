package com.example.docapp.data
import com.example.docapp.data.db.dao.AttachmentDao
import com.example.docapp.data.db.dao.AttachmentDaoSql
/**
 * Factory class that creates and provides access to all DAO implementations.
 * Centralizes DAO creation and provides a single access point for all data access objects.
 * 
 * Works by creating instances of all DAO implementations using the provided database instance,
 * exposing them as properties for easy access.
 * 
 * arguments:
 *     db - AppDb: The database instance to use for creating DAOs
 */
class SqlDaoFactory(private val db: AppDb) {
    val templates: TemplateDao = TemplateDaoSql(db)
    val folders: FolderDao = FolderDaoSql(db)
    val documents: DocumentDao = DocumentDaoSql(db)
    val settings: SettingsDao = SettingsDaoSql(db)
    val attachments: AttachmentDao = AttachmentDaoSql(db.encryptedWritableDatabase)
}
