package com.example.docapp.domain.interactors
import com.example.docapp.domain.DocumentRepository

/**
 * Interactor class for document operations, providing a clean interface to document repository.
 * Wraps document repository methods with business logic layer.
 * 
 * Works by delegating document operations to the underlying repository, providing
 * a simplified interface for document creation, retrieval, updates, and management.
 * 
 * arguments:
 *     repository - DocumentRepository: The document repository to delegate operations to
 */
class DocumentInteractors(
    private val repository: DocumentRepository
) {
    /**
     * Creates a new document with the specified properties and attachments.
     * 
     * arguments:
     *     templateId - String?: Optional template ID, null if no template
     *     folderId - String?: Optional folder ID, null for root folder
     *     name - String: Document name
     *     description - String: Document description
     *     fields - List<Pair<String, String>>: Custom field name-value pairs
     *     photoUris - List<String>: Photo attachment URIs
     *     pdfUris - List<String>: PDF attachment URIs
     * 
     * return:
     *     docId - String: The ID of the created document
     */
    suspend fun create(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoUris: List<String>,
        pdfUris: List<String>
    ) = repository.createDocument(templateId, folderId, name, description, fields, photoUris, pdfUris)
    
    /**
     * Creates a new document with attachments that have custom file names.
     * 
     * arguments:
     *     templateId - String?: Optional template ID, null if no template
     *     folderId - String?: Optional folder ID, null for root folder
     *     name - String: Document name
     *     description - String: Document description
     *     fields - List<Pair<String, String>>: Custom field name-value pairs
     *     photoFiles - List<Pair<Uri, String>>: Photo attachments with URIs and custom names
     *     pdfFiles - List<Pair<Uri, String>>: PDF attachments with URIs and custom names
     * 
     * return:
     *     docId - String: The ID of the created document
     */
    suspend fun createWithNames(
        templateId: String?,
        folderId: String?,
        name: String,
        description: String,
        fields: List<Pair<String, String>>,
        photoFiles: List<Pair<android.net.Uri, String>>,
        pdfFiles: List<Pair<android.net.Uri, String>>
    ) = repository.createDocumentWithNames(templateId, folderId, name, description, fields, photoFiles, pdfFiles)
    
    /**
     * Retrieves a complete document by its ID.
     * 
     * arguments:
     *     id - String: The document ID
     * 
     * return:
     *     fullDocument - DocumentRepository.FullDocument?: The complete document if found, null otherwise
     */
    suspend fun get(id: String) = repository.getDocument(id)
    
    /**
     * Updates an existing document with new properties, fields, and attachments.
     * 
     * arguments:
     *     fullDocument - DocumentRepository.FullDocument: The document with updated properties
     *     preparedAttachments - List<Attachment>: Updated list of attachments
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun update(fullDocument: DocumentRepository.FullDocument, preparedAttachments: List<com.example.docapp.domain.Attachment>) {
        repository.updateDocument(fullDocument.doc, fullDocument.fields, preparedAttachments)
    }
    
    /**
     * Deletes a document by its ID.
     * 
     * arguments:
     *     id - String: The document ID to delete
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun delete(id: String) = repository.deleteDocument(id)
    
    /**
     * Sets the pinned status of a document.
     * 
     * arguments:
     *     id - String: The document ID
     *     pinned - Boolean: True to pin, false to unpin
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun pin(id: String, pinned: Boolean) = repository.pinDocument(id, pinned)
    
    /**
     * Updates the last opened timestamp of a document.
     * 
     * arguments:
     *     id - String: The document ID
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun touchOpened(id: String) = repository.touchOpened(id)
    
    /**
     * Moves a document to a different folder.
     * 
     * arguments:
     *     id - String: The document ID to move
     *     folderId - String?: The target folder ID, null for root folder
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun moveToFolder(id: String, folderId: String?) = repository.moveToFolder(id, folderId)
    
    /**
     * Swaps the pinned order of two documents.
     * 
     * arguments:
     *     aId - String: First document ID
     *     bId - String: Second document ID
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun swapPinned(aId: String, bId: String) = repository.swapPinned(aId, bId)
    
    /**
     * Retrieves all documents in a specific folder.
     * 
     * arguments:
     *     folderId - String: The folder ID
     * 
     * return:
     *     documents - List<Document>: List of documents in the folder
     */
    suspend fun getDocumentsInFolder(folderId: String) = repository.getDocumentsInFolder(folderId)
    
    /**
     * Observes the home screen document list as a reactive stream.
     * 
     * return:
     *     homeList - Flow<DocumentRepository.HomeList>: Flow emitting pinned and recent documents
     */
    fun observeHome() = repository.observeHome()
}
