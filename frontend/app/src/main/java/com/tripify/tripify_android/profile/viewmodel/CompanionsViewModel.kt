package com.tripify.tripify_android.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.profile.api.ProfileApiService
import com.tripify.tripify_android.profile.model.CompanionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CompanionsViewModel(
    private val apiService: ProfileApiService
) : ViewModel() {

    private val _companions = MutableStateFlow<List<CompanionDto>>(emptyList())
    val companions: StateFlow<List<CompanionDto>> = _companions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCompanion = CompanionDto(
                    firstName = firstName,
                    lastName = lastName,
                    dateOfBirth = dateOfBirth
                )
                val savedCompanion = apiService.addCompanion(newCompanion)
                _companions.value = _companions.value + savedCompanion
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'aggiunta: ${e.message}"
                // AGGIUNGI QUESTE DUE RIGHE PER IL DEBUG:
                println("❌ ERRORE COMPAGNO: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}