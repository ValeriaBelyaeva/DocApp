package com.example.docapp.domain.interactors
import com.example.docapp.domain.FolderRepository

/**
 * Interactor class for folder operations, providing a clean interface to folder repository.
 * Wraps folder repository methods with business logic layer.
 * 
 * Works by delegating folder operations to the underlying repository, providing
 * a simplified interface for folder management.
 * 
 * arguments:
 *     repository - FolderRepository: The folder repository to delegate operations to
 */
class FolderInteractors(private val repository: FolderRepository) {
    /**
     * Observes the folder tree structure as a reactive stream.
     * 
     * return:
     *     folders - Flow<List<Folder>>: Flow emitting the folder tree whenever it changes
     */
    fun observeTree() = repository.observeTree()
    
    /**
     * Creates a new folder with the specified name and optional parent folder.
     * 
     * arguments:
     *     name - String: The folder name
     *     parentId - String?: Optional parent folder ID, null for root level
     * 
     * return:
     *     folderId - String: The ID of the created folder
     */
    suspend fun add(name: String, parentId: String?) = repository.addFolder(name, parentId)
    
    /**
     * Deletes a folder by its ID.
     * 
     * arguments:
     *     id - String: The folder ID to delete
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun delete(id: String) = repository.deleteFolder(id)
    
    /**
     * Lists all folders in the system.
     * 
     * return:
     *     folders - List<Folder>: List of all folders
     */
    suspend fun listAll() = repository.listAll()
}
