package com.example.docapp.data

/**
 * Простая фабрика, собирающая все DAO поверх AppDb.
 * Если у тебя уже есть такой класс внутри Db.kt — этот файл не нужен.
 */
class SqlDaoFactory(private val db: AppDb) {
    val templates: TemplateDao = TemplateDaoSql(db)
    val folders: FolderDao = FolderDaoSql(db)
    val documents: DocumentDao = DocumentDaoSql(db)
    val settings: SettingsDao = SettingsDaoSql(db)
}
