package com.example.docapp.domain.interactors
import com.example.docapp.domain.Template
import com.example.docapp.domain.TemplateField
import com.example.docapp.domain.TemplateRepository

/**
 * Interactor class for template operations, providing a clean interface to template repository.
 * Wraps template repository methods with business logic layer.
 * 
 * Works by delegating template operations to the underlying repository, providing
 * a simplified interface for template management.
 * 
 * arguments:
 *     repository - TemplateRepository: The template repository to delegate operations to
 */
class TemplateInteractors(private val repository: TemplateRepository) {
    /**
     * Lists all available templates.
     * 
     * return:
     *     templates - List<Template>: List of all templates
     */
    suspend fun listTemplates(): List<Template> = repository.listTemplates()
    
    /**
     * Retrieves a template by its ID.
     * 
     * arguments:
     *     id - String: The template ID
     * 
     * return:
     *     template - Template?: The template if found, null otherwise
     */
    suspend fun getTemplate(id: String): Template? = repository.getTemplate(id)
    
    /**
     * Lists all fields defined for a template.
     * 
     * arguments:
     *     id - String: The template ID
     * 
     * return:
     *     fields - List<TemplateField>: List of template fields
     */
    suspend fun listTemplateFields(id: String): List<TemplateField> = repository.listFields(id)
    
    /**
     * Creates a new template with the specified name and field names.
     * 
     * arguments:
     *     name - String: The template name
     *     fields - List<String>: List of field names
     * 
     * return:
     *     templateId - String: The ID of the created template
     */
    suspend fun addTemplate(name: String, fields: List<String>): String = repository.addTemplate(name, fields)
    
    /**
     * Updates an existing template and its fields.
     * 
     * arguments:
     *     template - Template: The template with updated properties
     *     fields - List<TemplateField>: Updated list of template fields
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun updateTemplate(template: Template, fields: List<TemplateField>) =
        repository.updateTemplate(template, fields)
    
    /**
     * Deletes a template by its ID.
     * 
     * arguments:
     *     id - String: The template ID to delete
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun deleteTemplate(id: String) = repository.deleteTemplate(id)
    
    /**
     * Sets the pinned status of a template.
     * 
     * arguments:
     *     id - String: The template ID
     *     pinned - Boolean: True to pin, false to unpin
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun pinTemplate(id: String, pinned: Boolean) = repository.pinTemplate(id, pinned)
}
