package com.example.docapp.core
object DataValidator {
    fun validateFolderName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Folder name cannot be empty")
        }
        if (trimmed.length > 50) {
            return ValidationResult.Error("Folder name cannot be longer than 50 characters")
        }
        val normalized = trimmed.uppercase()
        return ValidationResult.Success(normalized)
    }
    fun validateTemplateName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Template name cannot be empty")
        }
        if (trimmed.length > 50) {
            return ValidationResult.Error("Template name cannot exceed 50 characters")
        }
        val normalized = trimmed.uppercase()
        return ValidationResult.Success(normalized)
    }
    fun validateFieldName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Field name cannot be empty")
        }
        if (trimmed.length > 30) {
            return ValidationResult.Error("Field name cannot exceed 30 characters")
        }
        val normalized = trimmed.uppercase()
        return ValidationResult.Success(normalized)
    }
    fun validateDocumentName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Document name cannot be empty")
        }
        if (trimmed.length > 100) {
            return ValidationResult.Error("Document name cannot exceed 100 characters")
        }
        val normalized = trimmed.uppercase()
        return ValidationResult.Success(normalized)
    }
    fun validateFieldValue(value: String): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Error("Field value cannot be empty")
        }
        if (trimmed.length > 200) {
            return ValidationResult.Error("Field value cannot exceed 200 characters")
        }
        val normalized = trimmed.uppercase()
        return ValidationResult.Success(normalized)
    }
    private fun containsOnlyValidChars(text: String): Boolean {
        return text.all { char ->
            char.isLetterOrDigit() || char.isWhitespace() || char == '-' || char == '_'
        }
    }
}
sealed class ValidationResult {
    data class Success(val data: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    fun getValue(): String? = if (this is Success) data else null
    fun getError(): String? = if (this is Error) message else null
}
