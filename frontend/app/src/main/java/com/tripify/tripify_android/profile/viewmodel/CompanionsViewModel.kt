package com.tripify.tripify_android.profile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.profile.api.ProfileApiService
import com.tripify.tripify_android.profile.model.CompanionDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class CompanionsViewModel(
    private val apiService: ProfileApiService
) : ViewModel() {

    private val _companions = MutableStateFlow<List<CompanionDto>>(emptyList())
    val companions: StateFlow<List<CompanionDto>> = _companions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var loadJob: Job? = null

    fun loadCompanions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _companions.value = apiService.getCompanions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Impossibile caricare i compagni di viaggio. Controlla la connessione e riprova."
                Log.e("CompanionsVM", "Load Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCompanion(firstName: String, lastName: String, dateOfBirth: String) {
        _errorMessage.value = null

        if (firstName.isBlank() || lastName.isBlank() || dateOfBirth.isBlank()) {
            _errorMessage.value = "Tutti i campi sono obbligatori."
            return
        }

        try {
            val birthDate = LocalDate.parse(dateOfBirth, dateFormatter)
            val today = LocalDate.now()

            if (birthDate.isAfter(today)) {
                _errorMessage.value = "La data di nascita non può essere nel futuro."
                return
            }

            val age = Period.between(birthDate, today).years
            if (age < 18) {
                _errorMessage.value = "Il compagno di viaggio deve essere maggiorenne (almeno 18 anni)."
                return
            }
        } catch (e: DateTimeParseException) {
            _errorMessage.value = "Formato data non valido. Usa AAAA-MM-GG."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCompanion = CompanionDto(
                    id = null,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    dateOfBirth = dateOfBirth
                )
                apiService.addCompanion(newCompanion)
                loadCompanions()
            } catch (e: HttpException) {
                val serverMessage = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
                _errorMessage.value = if (!serverMessage.isNullOrBlank()) serverMessage else "Errore durante il salvataggio."
                Log.e("CompanionsVM", "Add Error", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Errore di rete durante il salvataggio. Riprova."
                Log.e("CompanionsVM", "Add Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCompanion(id: String) {
        _errorMessage.value = null
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.deleteCompanion(id)
                loadCompanions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'eliminazione. Riprova."
                Log.e("CompanionsVM", "Delete Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
