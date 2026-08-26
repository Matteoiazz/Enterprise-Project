package com.tripify.tripify_android.itinerary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.data.ItineraryRetrofit
import com.tripify.tripify_android.itinerary.data.UpdateVisibilityRequest
import com.tripify.tripify_android.itinerary.util.extractUserIdFromToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ItineraryFeedState {
    data object Loading : ItineraryFeedState()
    data class Success(val lists: List<FavoriteListDto>) : ItineraryFeedState()
    data class Error(val message: String) : ItineraryFeedState()
}

sealed class ItineraryDetailState {
    data object Loading : ItineraryDetailState()
    data class Success(val list: FavoriteListDto) : ItineraryDetailState()
    data class Error(val message: String) : ItineraryDetailState()
}

class ItineraryViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val api = ItineraryRetrofit.create(tokenManager)
    private val bookingApi = RetrofitClient.createBookingApi(tokenManager)

    private val _feedState = MutableStateFlow<ItineraryFeedState>(ItineraryFeedState.Loading)
    val feedState: StateFlow<ItineraryFeedState> = _feedState.asStateFlow()

    private val _detailState = MutableStateFlow<ItineraryDetailState>(ItineraryDetailState.Loading)
    val detailState: StateFlow<ItineraryDetailState> = _detailState.asStateFlow()

    fun loadFeed(city: String? = null, sort: String = "likes") {
        viewModelScope.launch {
            _feedState.value = ItineraryFeedState.Loading
            try {
                val response = api.getPublicFeed(city?.trim()?.ifBlank { null }, sort)
                _feedState.value = if (response.isSuccessful && response.body() != null) {
                    ItineraryFeedState.Success(response.body()!!)
                } else {
                    ItineraryFeedState.Error("Errore nel caricamento (${response.code()})")
                }
            } catch (e: Exception) {
                _feedState.value = ItineraryFeedState.Error("Nessuna connessione al server")
            }
        }
    }

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _detailState.value = ItineraryDetailState.Loading
            try {
                val response = api.getById(id)
                _detailState.value = if (response.isSuccessful && response.body() != null) {
                    ItineraryDetailState.Success(response.body()!!)
                } else {
                    ItineraryDetailState.Error("Itinerario non trovato")
                }
            } catch (e: Exception) {
                _detailState.value = ItineraryDetailState.Error("Nessuna connessione al server")
            }
        }
    }

    /** Toggle like e ricarica il dettaglio per aggiornare il contatore mostrato. */
    fun toggleLike(id: Long) {
        viewModelScope.launch {
            try {
                val response = api.toggleLike(id)
                if (response.isSuccessful) {
                    loadDetail(id)
                }
            } catch (e: Exception) {
                // like non riuscito: la UI resta invariata, l'utente può riprovare
            }
        }
    }

    fun updateVisibility(id: Long, visibility: String, city: String?, onResult: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.updateVisibility(id, UpdateVisibilityRequest(visibility, city))
                if (response.isSuccessful) {
                    loadDetail(id)
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string() ?: "Requisiti di pubblicazione non soddisfatti")
                }
            } catch (e: Exception) {
                onResult(false, "Nessuna connessione al server")
            }
        }
    }

    /**
     * Aggiunge ogni componente della lista al carrello reale, uno per uno, tramite lo
     * stesso endpoint già usato da CartViewModel — nessuna nuova entity nel carrello.
     */
    fun bookAll(list: FavoriteListDto, onResult: (successCount: Int, total: Int) -> Unit) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            val userId = token?.let { extractUserIdFromToken(it) }
            if (userId == null) {
                onResult(0, list.catalogItemIds.size)
                return@launch
            }
            var successCount = 0
            for (itemId in list.catalogItemIds) {
                try {
                    val response = bookingApi.addToCart(userId, itemId, 1)
                    if (response.isSuccessful) successCount++
                } catch (e: Exception) {
                    // continua con gli altri componenti anche se uno fallisce
                }
            }
            try {
                api.registerBookingAttempt(list.id)
            } catch (e: Exception) {
                // contatore best-effort, non blocca l'esito della prenotazione
            }
            onResult(successCount, list.catalogItemIds.size)
        }
    }
}

class ItineraryViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItineraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItineraryViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
