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
import com.tripify.tripify_android.data.parseApiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody


class OrganizerViewModel(tokenManager: TokenManager) : ViewModel() {

    private val catalogApi = RetrofitClient.createCatalogApi(tokenManager)
    private val bookingApi = RetrofitClient.createBookingApi(tokenManager)

    private val _myItems = MutableStateFlow<List<OrganizerItemDto>>(emptyList())
    val myItems: StateFlow<List<OrganizerItemDto>> = _myItems.asStateFlow()

    private val _receivedBookings = MutableStateFlow<List<ReceivedBookingLineDto>>(emptyList())
    val receivedBookings: StateFlow<List<ReceivedBookingLineDto>> = _receivedBookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadMyItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = catalogApi.getMyItems()
                _myItems.value = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                if (!response.isSuccessful) {
                    _errorMessage.value = response.parseApiError("Impossibile caricare i tuoi annunci")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadReceivedBookings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bookingApi.getReceivedBookings()
                _receivedBookings.value = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                if (!response.isSuccessful) {
                    _errorMessage.value = response.parseApiError("Impossibile caricare le prenotazioni ricevute")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun createFlight(request: CreateFlightRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.createFlight(request) }
    fun createActivity(request: CreateActivityRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.createActivity(request) }

    fun updateFlight(id: Int, request: CreateFlightRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateFlight(id, request) }
    fun updateActivity(id: Int, request: CreateActivityRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateActivity(id, request) }

    fun deleteItem(id: Int, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.deleteItem(id) }

    fun createHotel(request: CreateHotelRequest, imageUris: List<Uri>, context: Context, onResult: (Boolean) -> Unit) =
        saveHotel(id = null, request = request, imageUris = imageUris, context = context, onResult = onResult)

    fun updateHotel(id: Int, request: CreateHotelRequest, imageUris: List<Uri>, context: Context, onResult: (Boolean) -> Unit) =
        saveHotel(id = id, request = request, imageUris = imageUris, context = context, onResult = onResult)

    // Creazione e modifica hotel: stesso flusso (salva l'annuncio, poi carica le
    // eventuali foto). Differenza: in creazione l'id lo restituisce il backend.
    private fun saveHotel(id: Int?, request: CreateHotelRequest, imageUris: List<Uri>, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = if (id == null) catalogApi.createHotel(request) else catalogApi.updateHotel(id, request)
                if (!response.isSuccessful) {
                    _errorMessage.value = response.parseApiError("Operazione non riuscita")
                    onResult(false); return@launch
                }
                val itemId = id ?: response.body()?.id
                if (itemId != null && imageUris.isNotEmpty()) uploadImages(itemId, imageUris, context)
                loadMyItems()
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"; onResult(false)
            }
        }
    }

    fun deleteHotelImage(id: Int, imageUrl: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = catalogApi.deleteItemImage(id, imageUrl)
                if (!response.isSuccessful) _errorMessage.value = "Impossibile rimuovere la foto"
                onResult(response.isSuccessful)
            } catch (e: CancellationException) {
                throw e
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
                // "image/jpeg" -> "jpg", "image/png" -> "png", altro/strano -> "jpg"
                val ext = mime.substringAfterLast('/', "jpg").takeWhile { it.isLetterOrDigit() }.ifBlank { "jpg" }
                val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", "photo_$index.$ext", body)
            } catch (e: Exception) {
                null
            }
        }
        if (parts.isEmpty()) return
        try {
            val response = catalogApi.uploadItemImages(itemId, parts)
            if (!response.isSuccessful) _errorMessage.value = "Annuncio salvato, ma alcune foto non sono state caricate"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _errorMessage.value = "Annuncio salvato, ma le foto non sono state caricate"
        }
    }

    private fun launchWrite(onResult: (Boolean) -> Unit, call: suspend () -> retrofit2.Response<*>) {
        viewModelScope.launch {
            try {
                val response = call()
                if (response.isSuccessful) {
                    loadMyItems()
                    onResult(true)
                } else {
                    _errorMessage.value = response.parseApiError("Operazione non riuscita")
                    onResult(false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "Nessuna connessione al server"
                onResult(false)
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
