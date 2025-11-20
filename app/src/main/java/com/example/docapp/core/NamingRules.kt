package com.example.docapp.core
import java.util.Locale
object NamingRules {
    enum class NameKind {
        Document,
        Folder,
        Template,
        Field
    }
    fun formatName(input: String, kind: NameKind): String {
        val trimmed = input.trim()
        return when (kind) {
            NameKind.Document -> trimmed.lowercase(Locale.ROOT)
            NameKind.Folder -> trimmed
            NameKind.Template -> trimmed
            NameKind.Field -> trimmed
        }
    }
}
