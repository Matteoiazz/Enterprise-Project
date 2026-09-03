package com.tripify.tripify_android.organizer.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.CreateActivityRequest
import com.tripify.tripify_android.data.model.CreateFlightRequest
import com.tripify.tripify_android.data.model.CreateHotelRequest
import com.tripify.tripify_android.data.model.OrganizerItemDto
import com.tripify.tripify_android.data.model.ReceivedBookingLineDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody


class OrganizerViewModel(tokenManager: TokenManager) : ViewModel() {

    private val catalogApi = RetrofitClient.createCatalogApi(tokenManager)
    private val bookingApi = RetrofitClient.createBookingApi(tokenManager)

    private val _myItems = MutableStateFlow<List<OrganizerItemDto>>(emptyList())
    val myItems: StateFlow<List<OrganizerItemDto>> = _myItems

    private val _receivedBookings = MutableStateFlow<List<ReceivedBookingLineDto>>(emptyList())
    val receivedBookings: StateFlow<List<ReceivedBookingLineDto>> = _receivedBookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Vera per tutta la durata di una scrittura (crea/modifica/elimina/riattiva annuncio):
    // impedisce un doppio tap su "Salva"/"Elimina"/"Riattiva" che altrimenti spedirebbe
    // due richieste identiche prima che la prima risposta torni.
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    fun loadMyItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = catalogApi.getMyItems()
                _myItems.value = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                if (!response.isSuccessful) _errorMessage.value = "Impossibile caricare i tuoi annunci"
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"
            }
            _isLoading.value = false
        }
    }

    fun loadReceivedBookings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bookingApi.getReceivedBookings()
                _receivedBookings.value = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                if (!response.isSuccessful) _errorMessage.value = "Impossibile caricare le prenotazioni ricevute"
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"
            }
            _isLoading.value = false
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun createFlight(request: CreateFlightRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.createFlight(request) }
    fun createActivity(request: CreateActivityRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.createActivity(request) }

    fun updateFlight(id: Int, request: CreateFlightRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateFlight(id, request) }
    fun updateActivity(id: Int, request: CreateActivityRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateActivity(id, request) }

    fun deleteItem(id: Int, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.deleteItem(id) }
    fun reactivateItem(id: Int, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.reactivateItem(id) }


    fun createHotel(request: CreateHotelRequest, imageUris: List<Uri>, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val response = catalogApi.createHotel(request)
                if (!response.isSuccessful) {
                    _errorMessage.value = extractErrorMessage(response.errorBody()?.string(), "Operazione non riuscita")
                    onResult(false); return@launch
                }
                val newId = response.body()?.id
                if (newId != null && imageUris.isNotEmpty()) uploadImages(newId, imageUris, context)
                loadMyItems()
                onResult(true)
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"; onResult(false)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateHotel(id: Int, request: CreateHotelRequest, imageUris: List<Uri>, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val response = catalogApi.updateHotel(id, request)
                if (!response.isSuccessful) {
                    _errorMessage.value = extractErrorMessage(response.errorBody()?.string(), "Operazione non riuscita")
                    onResult(false); return@launch
                }
                if (imageUris.isNotEmpty()) uploadImages(id, imageUris, context)
                loadMyItems()
                onResult(true)
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"; onResult(false)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteHotelImage(id: Int, imageUrl: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = catalogApi.deleteItemImage(id, imageUrl)
                if (!response.isSuccessful) _errorMessage.value = "Impossibile rimuovere la foto"
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"; onResult(false)
            }
        }
    }

    private suspend fun uploadImages(itemId: Int, uris: List<Uri>, context: Context) {
        val parts = uris.mapIndexedNotNull { index, uri ->
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@mapIndexedNotNull null
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", "photo_$index.${mime.substringAfter('/', "jpg")}", body)
            } catch (e: Exception) {
                null
            }
        }
        if (parts.isEmpty()) return
        try {
            val response = catalogApi.uploadItemImages(itemId, parts)
            if (!response.isSuccessful) _errorMessage.value = "Annuncio salvato, ma alcune foto non sono state caricate"
        } catch (e: Exception) {
            _errorMessage.value = "Annuncio salvato, ma le foto non sono state caricate"
        }
    }

    /** Estrae il campo "message" del corpo di errore JSON (vedi ApiError sul backend), con un fallback leggibile. */
    private fun extractErrorMessage(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            org.json.JSONObject(raw).optString("message").ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }
    }

    private fun launchWrite(onResult: (Boolean) -> Unit, call: suspend () -> retrofit2.Response<*>) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val response = call()
                if (response.isSuccessful) {
                    loadMyItems()
                    onResult(true)
                } else {
                    _errorMessage.value = extractErrorMessage(response.errorBody()?.string(), "Operazione non riuscita")
                    onResult(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"
                onResult(false)
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}

class OrganizerViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrganizerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrganizerViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
