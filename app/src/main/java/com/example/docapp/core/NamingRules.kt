package com.example.docapp.core

import java.util.Locale

/**
 * Centralized name normalization rules for different name kinds.
 */
object NamingRules {
    enum class NameKind {
        Document,
        Folder,
        Template,
        Field
    }

    /**
     * Formats name according to business rules for the provided kind.
     * Keep logic centralized to avoid duplication across screens.
     */
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


