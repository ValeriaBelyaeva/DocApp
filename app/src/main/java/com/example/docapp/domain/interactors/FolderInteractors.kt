package com.example.docapp.domain.interactors
import com.example.docapp.domain.FolderRepository
class FolderInteractors(private val repository: FolderRepository) {
    fun observeTree() = repository.observeTree()
    suspend fun add(name: String, parentId: String?) = repository.addFolder(name, parentId)
    suspend fun delete(id: String) = repository.deleteFolder(id)
    suspend fun listAll() = repository.listAll()
}
