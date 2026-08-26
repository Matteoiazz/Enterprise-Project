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

    // 1. Recupera lo storico dei viaggi dell'utente autenticato (non serve più
    // passare l'userId: il backend lo ricava dal JWT). Lo storico ora arriva
    // paginato dal server: qui prendiamo sempre la prima pagina.
    fun fetchUserBookings() {
        viewModelScope.launch {
            _uiState.value = BookingState.Loading
            try {
                val response = api.getUserBookings()

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = BookingState.Success(response.body()!!.content)
                } else {
                    val cleanError = response.parseErrorMessage()
                    _uiState.value = BookingState.Error(cleanError)
                }
            } catch (e: Exception) {
                _uiState.value = BookingState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // 2. Invita un amico a un viaggio esistente. leaderId non serve più
    // passarlo: solo il vero proprietario del token puo' invitare comunque
    // (il backend lo verifica sempre lato server tramite il JWT).
    fun inviteFriend(
        bookingId: Long,
        friendId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.inviteFriend(bookingId, friendId)

                if (response.isSuccessful) {
                    onSuccess()
                    fetchUserBookings()
                } else {
                    val cleanError = response.parseErrorMessage()
                    onError(cleanError)
                }
            } catch (e: Exception) {
                onError("Nessuna connessione: ${e.message}")
            }
        }
    }

    // 3. Annulla una prenotazione (solo il Leader); se era già confermata il
    // backend avvia anche il rimborso.
    fun cancelBooking(
        bookingId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.cancelBooking(bookingId)

                if (response.isSuccessful) {
                    onSuccess()
                    fetchUserBookings()
                } else {
                    onError(response.parseErrorMessage())
                }
            } catch (e: Exception) {
                onError("Nessuna connessione: ${e.message}")
            }
        }
    }
}
