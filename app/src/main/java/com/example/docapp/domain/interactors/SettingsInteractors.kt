package com.example.docapp.domain.interactors
import com.example.docapp.domain.SettingsRepository

/**
 * Interactor class for settings operations, providing a clean interface to settings repository.
 * Wraps settings repository methods with business logic layer.
 * 
 * Works by delegating settings operations to the underlying repository, providing
 * a simplified interface for PIN code management.
 * 
 * arguments:
 *     repository - SettingsRepository: The settings repository to delegate operations to
 */
class SettingsInteractors(private val repository: SettingsRepository) {
    /**
     * Checks if a PIN code has been set.
     * 
     * return:
     *     isSet - Boolean: True if PIN is set, false otherwise
     */
    suspend fun isPinSet() = repository.isPinSet()
    
    /**
     * Verifies a PIN code against the stored PIN hash.
     * 
     * arguments:
     *     pin - String: The PIN code to verify
     * 
     * return:
     *     isValid - Boolean: True if PIN is correct, false otherwise
     */
    suspend fun verifyPin(pin: String) = repository.verifyPin(pin)
    
    /**
     * Sets a new PIN code for the application.
     * 
     * arguments:
     *     pin - String: The new PIN code to set
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun setNewPin(pin: String) = repository.setNewPin(pin)
    
    /**
     * Disables PIN code protection by removing the stored PIN hash.
     * 
     * return:
     *     Unit - No return value
     */
    suspend fun disablePin() = repository.disablePin()
}
