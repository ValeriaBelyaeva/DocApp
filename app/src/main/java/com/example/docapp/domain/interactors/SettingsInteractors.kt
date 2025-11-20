package com.example.docapp.domain.interactors
import com.example.docapp.domain.SettingsRepository
class SettingsInteractors(private val repository: SettingsRepository) {
    suspend fun isPinSet() = repository.isPinSet()
    suspend fun verifyPin(pin: String) = repository.verifyPin(pin)
    suspend fun setNewPin(pin: String) = repository.setNewPin(pin)
    suspend fun disablePin() = repository.disablePin()
}
