package com.example.docapp.domain
import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing application settings, particularly PIN code settings.
 * Provides methods to check, verify, set, and disable PIN codes.
 * 
 * Works by delegating to underlying data storage to persist and retrieve PIN settings securely.
 */
interface SettingsRepository {
    /**
     * Checks if a PIN code has been set for the application.
     * 
     * return:
     *     isSet - Boolean: True if PIN is set, false otherwise
     */
    suspend fun isPinSet(): Boolean
    
    /**
     * Verifies a PIN code against the stored PIN hash.
     * 
     * arguments:
     *     pin - String: The PIN code to verify
     * 
     * return:
     *     isValid - Boolean: True if PIN is correct, false otherwise
     */
    suspend fun verifyPin(pin: String): Boolean
    
    /**
     * Sets a new PIN code for the application.
     * 
     * arguments:
     *     pin - String: The new PIN code to set
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun setNewPin(pin: String)
    
    /**
     * Disables PIN code protection by removing the stored PIN hash.
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun disablePin()
}

/**
 * Repository interface for managing folder hierarchy and organization.
 * Provides methods to observe folder tree, create, delete, and list folders.
 * 
 * Works by managing a hierarchical folder structure where folders can have parent folders,
 * allowing nested organization of documents.
 */
interface FolderRepository {
    /**
     * Observes the folder tree structure as a reactive stream.
     * 
     * return:
     *     folders - Flow<List<Folder>>: Flow emitting the complete folder tree whenever it changes
     */
    fun observeTree(): Flow<List<Folder>>
    
    /**
     * Creates a new folder with the specified name and optional parent folder.
     * 
     * arguments:
     *     name - String: The name of the folder to create
     *     parentId - String?: Optional ID of the parent folder, null for root level
     * 
     * return:
     *     folderId - String: The ID of the newly created folder
     */
    suspend fun addFolder(name: String, parentId: String?): String
    
    /**
     * Deletes a folder by its ID.
     * 
     * arguments:
     *     id - String: The ID of the folder to delete
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun deleteFolder(id: String)
    
    /**
     * Lists all folders in the system.
     * 
     * return:
     *     folders - List<Folder>: List of all folders
     */
    suspend fun listAll(): List<Folder>
}

/**
 * Repository interface for managing document templates.
 * Provides methods to list, get, create, update, delete, and pin templates.
 * 
 * Works by managing template definitions that can be used to create documents with predefined fields.
 */
interface TemplateRepository {
    /**
     * Lists all available templates.
     * 
     * return:
     *     templates - List<Template>: List of all templates
     */
    suspend fun listTemplates(): List<Template>
    
    /**
     * Retrieves a template by its ID.
     * 
     * arguments:
     *     id - String: The ID of the template to retrieve
     * 
     * return:
     *     template - Template?: The template if found, null otherwise
     */
    suspend fun getTemplate(id: String): Template?
    
    /**
     * Lists all fields defined for a template.
     * 
     * arguments:
     *     templateId - String: The ID of the template
     * 
     * return:
     *     fields - List<TemplateField>: List of fields defined in the template
     */
    suspend fun listFields(templateId: String): List<TemplateField>
    
    /**
     * Creates a new template with the specified name and field names.
     * 
     * arguments:
     *     name - String: The name of the template
     *     fields - List<String>: List of field names to include in the template
     * 
     * return:
     *     templateId - String: The ID of the newly created template
     */
    suspend fun addTemplate(name: String, fields: List<String>): String
    
    /**
     * Updates an existing template and its fields.
     * 
     * arguments:
     *     template - Template: The template object with updated properties
     *     fields - List<TemplateField>: Updated list of template fields
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun updateTemplate(template: Template, fields: List<TemplateField>)
    
    /**
     * Deletes a template by its ID.
     * 
     * arguments:
     *     id - String: The ID of the template to delete
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun deleteTemplate(id: String)
    
    /**
     * Sets the pinned status of a template.
     * 
     * arguments:
     *     id - String: The ID of the template
     *     pinned - Boolean: True to pin the template, false to unpin
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun pinTemplate(id: String, pinned: Boolean)
}

/**
 * Repository interface for managing documents and their content.
 * Provides methods to create, read, update, delete documents, and manage their organization.
 * 
 * Works by managing documents with custom fields, attachments (photos and PDFs), and folder organization.
 * Supports pinned documents and tracks recently opened documents.
 */
interface DocumentRepository {
    /**
     * Data class representing the home screen document list with pinned and recent documents.
     * 
     * arguments:
     *     pinned - List<Document>: List of pinned documents to display first
     *     recent - List<Document>: List of recently opened documents
     */
    data class HomeList(val pinned: List<Document>, val recent: List<Document>)
    
    /**
     * Data class representing a complete document with all its associated data.
     * 
     * arguments:
     *     doc - Document: The document entity with basic properties
     *     fields - List<DocumentField>: Custom fields associated with the document
     *     photos - List<Attachment>: Photo attachments for the document
     *     pdfs - List<Attachment>: PDF attachments for the document
     */
    data class FullDocument(
        val doc: Document,
        val fields: List<DocumentField>,
        val photos: List<Attachment>,
        val pdfs: List<Attachment>
    )
    /**
     * Observes the home screen document list as a reactive stream.
     * 
     * return:
     *     homeList - Flow<HomeList>: Flow emitting pinned and recent documents whenever they change
     */
    fun observeHome(): Flow<HomeList>
    
    /**
     * Creates a new document with the specified properties and attachments.
     * 
     * arguments:
     *     templateId - String?: Optional ID of template used to create the document, null if no template
     *     folderId - String?: Optional ID of folder to place document in, null for root folder
     *     name - String: The name of the document
     *     description - String: The description of the document
     *     fields - List<Pair<String, String>>: List of field name-value pairs for custom fields
     *     photoUris - List<String>: List of photo attachment URIs as strings
     *     pdfUris - List<String>: List of PDF attachment URIs as strings
     * 
     * return:
     *     docId - String: The ID of the newly created document
     */
    suspend fun createDocument(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoUris: List<String>,
        pdfUris: List<String>
    ): String
    
    /**
     * Creates a new document with attachments that have custom file names.
     * 
     * arguments:
     *     templateId - String?: Optional ID of template used to create the document, null if no template
     *     folderId - String?: Optional ID of folder to place document in, null for root folder
     *     name - String: The name of the document
     *     description - String: The description of the document
     *     fields - List<Pair<String, String>>: List of field name-value pairs for custom fields
     *     photoFiles - List<Pair<Uri, String>>: List of photo attachments with URIs and custom file names
     *     pdfFiles - List<Pair<Uri, String>>: List of PDF attachments with URIs and custom file names
     * 
     * return:
     *     docId - String: The ID of the newly created document
     */
    suspend fun createDocumentWithNames(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<Uri, String>>,
        pdfFiles: List<Pair<Uri, String>>
    ): String
    
    /**
     * Retrieves a complete document with all its fields and attachments.
     * 
     * arguments:
     *     id - String: The ID of the document to retrieve
     * 
     * return:
     *     fullDocument - FullDocument?: The complete document if found, null otherwise
     */
    suspend fun getDocument(id: String): FullDocument?
    
    /**
     * Updates an existing document with new properties, fields, and attachments.
     * 
     * arguments:
     *     doc - Document: The document entity with updated properties
     *     fields - List<DocumentField>: Updated list of document fields
     *     attachments - List<Attachment>: Updated list of attachments
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun updateDocument(doc: Document, fields: List<DocumentField>, attachments: List<Attachment>)
    
    /**
     * Deletes a document by its ID.
     * 
     * arguments:
     *     id - String: The ID of the document to delete
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun deleteDocument(id: String)
    
    /**
     * Sets the pinned status of a document.
     * 
     * arguments:
     *     id - String: The ID of the document
     *     pinned - Boolean: True to pin the document, false to unpin
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun pinDocument(id: String, pinned: Boolean)
    
    /**
     * Updates the last opened timestamp of a document to mark it as recently accessed.
     * 
     * arguments:
     *     id - String: The ID of the document
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun touchOpened(id: String)
    
    /**
     * Moves a document to a different folder.
     * 
     * arguments:
     *     id - String: The ID of the document to move
     *     folderId - String?: The ID of the target folder, null to move to root folder
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun moveToFolder(id: String, folderId: String?)
    
    /**
     * Swaps the pinned order of two documents.
     * 
     * arguments:
     *     aId - String: The ID of the first document
     *     bId - String: The ID of the second document
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun swapPinned(aId: String, bId: String)
    
    /**
     * Retrieves all documents in a specific folder.
     * 
     * arguments:
     *     folderId - String: The ID of the folder
     * 
     * return:
     *     documents - List<Document>: List of documents in the folder
     */
    suspend fun getDocumentsInFolder(folderId: String): List<Document>
}

/**
 * Container interface that provides access to all repository interfaces.
 * Aggregates all domain repositories into a single access point.
 * 
 * Works by exposing properties for each repository type, allowing centralized access to all data operations.
 */
interface Repositories {
    val settings: SettingsRepository
    val folders: FolderRepository
    val templates: TemplateRepository
    val documents: DocumentRepository
    val attachments: com.example.docapp.domain.repo.AttachmentRepository
}
