package com.tripify.tripify_android.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.parseErrorMessage // AGGIUNTO L'IMPORT PER LA MAGIA!
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookingViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val api = RetrofitClient.createBookingApi(tokenManager)

    private val _uiState = MutableStateFlow<BookingState>(BookingState.Loading)
    val uiState: StateFlow<BookingState> = _uiState

    // 1. Recupera lo storico dei viaggi dell'utente
    fun fetchUserBookings(userId: String) {
        viewModelScope.launch {
            _uiState.value = BookingState.Loading
            try {
                val response = api.getUserBookings(userId)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = BookingState.Success(response.body()!!)
                } else {
                    // MODIFICATO QUI: Estraiamo l'errore pulito dal server
                    val cleanError = response.parseErrorMessage()
                    _uiState.value = BookingState.Error(cleanError)
                }
            } catch (e: Exception) {
                _uiState.value = BookingState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // 2. Invita un amico a un viaggio esistente
    fun inviteFriend(
        bookingId: Long,
        leaderId: String,
        friendId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.inviteFriend(bookingId, leaderId, friendId)

                if (response.isSuccessful) {
                    onSuccess()
                    fetchUserBookings(leaderId)
                } else {
                    // MODIFICATO QUI: Estraiamo l'errore pulito dal server e lo passiamo alla UI
                    val cleanError = response.parseErrorMessage()
                    onError(cleanError)
                }
            } catch (e: Exception) {
                onError("Nessuna connessione: ${e.message}")
            }
        }
    }
}