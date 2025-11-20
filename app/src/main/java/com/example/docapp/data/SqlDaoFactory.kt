package com.example.docapp.data
import com.example.docapp.data.db.dao.AttachmentDao
import com.example.docapp.data.db.dao.AttachmentDaoSql
class SqlDaoFactory(private val db: AppDb) {
    val templates: TemplateDao = TemplateDaoSql(db)
    val folders: FolderDao = FolderDaoSql(db)
    val documents: DocumentDao = DocumentDaoSql(db)
    val settings: SettingsDao = SettingsDaoSql(db)
    val attachments: AttachmentDao = AttachmentDaoSql(db.encryptedWritableDatabase)
}
