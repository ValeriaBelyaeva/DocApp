package com.example.docapp.domain.interactors

import com.example.docapp.domain.Template
import com.example.docapp.domain.TemplateField
import com.example.docapp.domain.TemplateRepository

class TemplateInteractors(private val repository: TemplateRepository) {
    suspend fun listTemplates(): List<Template> = repository.listTemplates()

    suspend fun getTemplate(id: String): Template? = repository.getTemplate(id)

    suspend fun listTemplateFields(id: String): List<TemplateField> = repository.listFields(id)

    suspend fun addTemplate(name: String, fields: List<String>): String = repository.addTemplate(name, fields)

    suspend fun updateTemplate(template: Template, fields: List<TemplateField>) =
        repository.updateTemplate(template, fields)

    suspend fun deleteTemplate(id: String) = repository.deleteTemplate(id)

    suspend fun pinTemplate(id: String, pinned: Boolean) = repository.pinTemplate(id, pinned)
}

