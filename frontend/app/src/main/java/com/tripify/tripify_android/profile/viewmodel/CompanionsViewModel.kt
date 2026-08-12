package com.tripify.tripify_android.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.profile.api.ProfileApiService
import com.tripify.tripify_android.profile.model.CompanionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompanionsViewModel(
    private val apiService: ProfileApiService
) : ViewModel() {

    private val _companions = MutableStateFlow<List<CompanionDto>>(emptyList())
    val companions: StateFlow<List<CompanionDto>> = _companions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadCompanions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = apiService.getCompanions()
                _companions.value = list
            } catch (e: Exception) {
                _errorMessage.value = "Errore nel caricamento dei compagni: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCompanion(firstName: String, lastName: String, dateOfBirth: String) {
        if (firstName.isBlank() || lastName.isBlank() || dateOfBirth.isBlank()) {
            _errorMessage.value = "Tutti i campi sono obbligatori"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCompanion = CompanionDto(
                    id = null,
                    firstName = firstName,
                    lastName = lastName,
                    dateOfBirth = dateOfBirth
                )
                apiService.addCompanion(newCompanion)
                loadCompanions() // Molto più sicuro ricaricare la lista direttamente dal server
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'aggiunta: ${e.message}"
                println("❌ ERRORE COMPAGNO: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCompanion(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.deleteCompanion(id)
                loadCompanions()
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'eliminazione: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}