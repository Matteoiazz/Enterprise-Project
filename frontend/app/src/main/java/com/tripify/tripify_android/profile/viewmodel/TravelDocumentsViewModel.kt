package com.tripify.tripify_android.profile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.model.TravelDocumentDto
import com.tripify.tripify_android.profile.api.ProfileApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TravelDocumentsViewModel(private val apiService: ProfileApiService) : ViewModel() {

    private val _documents = MutableStateFlow<List<TravelDocumentDto>>(emptyList())
    val documents: StateFlow<List<TravelDocumentDto>> = _documents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    fun loadDocuments() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _documents.value = apiService.getTravelDocuments()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Impossibile caricare i documenti. Controlla la connessione e riprova."
                Log.e("TravelDocsVM", "Error loading docs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addDocument(
        type: String, number: String, expiration: String, country: String, onSuccess: () -> Unit
    ) {
        _errorMessage.value = null

        if (type.isBlank() || number.isBlank() || expiration.isBlank() || country.isBlank()) {
            _errorMessage.value = "Tutti i campi sono obbligatori"
            return
        }

        try {
            val expDate = LocalDate.parse(expiration, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            if (expDate.isBefore(LocalDate.now())) {
                _errorMessage.value = "Impossibile salvare: Il documento è scaduto."
                return
            }
        } catch (e: DateTimeParseException) {
            _errorMessage.value = "Formato data non valido."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newDoc = TravelDocumentDto(
                    id = null,
                    documentType = type,
                    documentNumber = number,
                    expirationDate = expiration,
                    issuingCountry = country
                )

                apiService.addTravelDocument(newDoc)

                loadDocuments()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                val serverMessage = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
                _errorMessage.value = if (!serverMessage.isNullOrBlank()) serverMessage else "Errore durante il salvataggio."
                Log.e("TravelDocsVM", "Error adding doc", e)
            } catch (e: Exception) {
                _errorMessage.value = "Errore di rete durante il salvataggio. Riprova."
                Log.e("TravelDocsVM", "Error adding doc", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDocument(id: String) {
        _errorMessage.value = null
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.deleteTravelDocument(id)
                loadDocuments()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'eliminazione. Riprova."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class TravelDocumentsViewModelFactory(private val apiService: ProfileApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TravelDocumentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TravelDocumentsViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}