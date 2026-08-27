package com.tripify.tripify_android.organizer.viewmodel

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

/**
 * Zona organizzatore: annunci posseduti (creazione/modifica/cancellazione) e
 * prenotazioni ricevute su quegli annunci. Nessun controllo di ruolo qui: se
 * il token non ha ROLE_ORGANIZER, il backend rifiuta le chiamate di scrittura
 * con 403 (vedi CatalogController) e getMyItems/getReceivedBookings tornano
 * semplicemente vuoti per chi non ha ancora annunci.
 */
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
    fun createHotel(request: CreateHotelRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.createHotel(request) }
    fun createActivity(request: CreateActivityRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.createActivity(request) }

    fun updateFlight(id: Int, request: CreateFlightRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateFlight(id, request) }
    fun updateHotel(id: Int, request: CreateHotelRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateHotel(id, request) }
    fun updateActivity(id: Int, request: CreateActivityRequest, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.updateActivity(id, request) }

    fun deleteItem(id: Int, onResult: (Boolean) -> Unit) = launchWrite(onResult) { catalogApi.deleteItem(id) }

    private fun launchWrite(onResult: (Boolean) -> Unit, call: suspend () -> retrofit2.Response<*>) {
        viewModelScope.launch {
            try {
                val response = call()
                if (response.isSuccessful) {
                    loadMyItems()
                    onResult(true)
                } else {
                    _errorMessage.value = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Operazione non riuscita"
                    onResult(false)
                }
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
