package com.tripify.tripify_android.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.booking.model.BoardingPassState
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.BookingResponseDTO
import com.tripify.tripify_android.data.model.PassengerRequestDTO
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.data.model.PaymentRequestDTO
import com.tripify.tripify_android.data.model.TravelDocumentDto
import com.tripify.tripify_android.data.parseErrorMessage // AGGIUNTO L'IMPORT PER LA MAGIA!
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BookingViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val api = RetrofitClient.createBookingApi(tokenManager)

    private val _uiState = MutableStateFlow<BookingState>(BookingState.Loading)
    val uiState: StateFlow<BookingState> = _uiState

    private val profileApi = RetrofitClient.createProfileApi(tokenManager)
    private val authApi = RetrofitClient.createApi(tokenManager)

    // Documenti di viaggio già salvati in Impostazioni Profilo, mostrati nella
    // schermata "Aggiungi passeggero" per evitare di doverli reinserire a mano.
    private val _savedTravelDocuments = MutableStateFlow<List<TravelDocumentDto>>(emptyList())
    val savedTravelDocuments: StateFlow<List<TravelDocumentDto>> = _savedTravelDocuments

    fun fetchSavedTravelDocuments() {
        viewModelScope.launch {
            try {
                val documents = profileApi.getTravelDocuments()
                _savedTravelDocuments.value = documents
            } catch (e: Exception) {
            }
        }
    }

    // A differenza di documenti/carte, in Impostazioni c'è un solo numero di
    // telefono per utente: serve solo a precompilare il campo in checkout,
    // non a scegliere tra più valori.
    private val _myPhoneNumber = MutableStateFlow<String?>(null)
    val myPhoneNumber: StateFlow<String?> = _myPhoneNumber

    fun fetchMyPhoneNumber() {
        viewModelScope.launch {
            try {
                val response = authApi.getCurrentUser()
                if (response.isSuccessful) {
                    _myPhoneNumber.value = response.body()?.phone
                }
            } catch (e: Exception) {
                // Il checkout resta possibile inserendo il telefono a mano.
            }
        }
    }

    // Associa un passeggero a una riga di prenotazione (solo il leader può farlo,
    // controllo lato server). Dopo il successo ricarica lo storico per aggiornare
    // il conteggio passeggeri mostrato sulla riga.
    fun addPassenger(
        bookingLineId: Long,
        request: PassengerRequestDTO,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.addPassenger(bookingLineId, request)
                if (response.isSuccessful) {
                    fetchUserBookings()
                    onSuccess()
                } else {
                    onError(response.parseErrorMessage())
                }
            } catch (e: Exception) {
                onError("Nessuna connessione: ${e.message}")
            }
        }
    }

    // 1. Recupera lo storico dei viaggi dell'utente autenticato (non serve più
    // passare l'userId: il backend lo ricava dal JWT). Lo storico ora arriva
    // paginato dal server: qui prendiamo sempre la prima pagina.
    // hideCancelled=true solo per il caricamento "a fresco" della schermata
    // (vedi BookingScreen): appena annullata, una prenotazione resta visibile
    // con la pill "Annullata" finché si sta ancora guardando quella schermata,
    // e sparisce solo alla visita successiva (da qui il default false sui
    // refresh interni dopo cancel/invite/addPassenger, che restano sulla
    // stessa schermata).
    fun fetchUserBookings(hideCancelled: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = BookingState.Loading
            // Senza token non ha senso nemmeno provare la chiamata: il backend
            // risponderebbe 401 e verrebbe mostrato come un errore generico
            // invece di un semplice invito ad accedere.
            if (tokenManager.tokenFlow.first() == null) {
                _uiState.value = BookingState.NotLoggedIn
                return@launch
            }
            try {
                val response = api.getUserBookings()

                if (response.isSuccessful && response.body() != null) {
                    val bookings = response.body()!!.content
                    val visibleBookings = if (hideCancelled) {
                        bookings.filter { it.status != "CANCELLED" }
                    } else {
                        bookings
                    }
                    _uiState.value = BookingState.Success(visibleBookings)
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

    // Il "biglietto" di un viaggio: il QR (o "check-in non ancora aperto") di
    // ogni passeggero, raggruppato per riga. Le righe arrivano già in memoria
    // dalla lista prenotazioni: qui serve solo recuperare i passeggeri di
    // ciascuna, che non viaggiano nella risposta "le mie prenotazioni" per non
    // appesantirla ogni volta (vedi BookingApi.getPassengersForLine).
    private val _boardingPassState = MutableStateFlow<BoardingPassState>(BoardingPassState.Loading)
    val boardingPassState: StateFlow<BoardingPassState> = _boardingPassState

    fun fetchBoardingPass(booking: BookingResponseDTO) {
        viewModelScope.launch {
            _boardingPassState.value = BoardingPassState.Loading
            try {
                val linesWithPassengers = booking.lines.map { line ->
                    val response = api.getPassengersForLine(line.id)
                    line to (if (response.isSuccessful) response.body().orEmpty() else emptyList())
                }
                _boardingPassState.value = BoardingPassState.Success(linesWithPassengers)
            } catch (e: Exception) {
                _boardingPassState.value = BoardingPassState.Error("Nessuna connessione: ${e.message}")
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

    // 4. Riprova il pagamento di una prenotazione rimasta PENDING (es. carta
    // rifiutata al primo tentativo): a differenza del checkout in CartViewModel,
    // qui la Booking esiste già, si chiama solo /payments/process direttamente
    // sul suo id. Se nel frattempo il blocco di una camera/posto è scaduto,
    // il backend rifiuta con un messaggio chiaro (vedi BookingService.confirmPayment)
    // invece di confermare qualcosa che potrebbe essere stato preso da altri.
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private val _savedPaymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val savedPaymentMethods: StateFlow<List<PaymentMethodDto>> = _savedPaymentMethods

    fun fetchSavedPaymentMethods() {
        viewModelScope.launch {
            try {
                val response = api.getSavedPaymentMethods()
                if (response.isSuccessful && response.body() != null) {
                    _savedPaymentMethods.value = response.body()!!
                }
            } catch (e: Exception) {
                // Il pagamento resta possibile inserendo una carta nuova.
            }
        }
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }

    fun retryPaymentWithSavedMethod(bookingId: Long, amount: Double, paymentMethodId: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                val response = api.processPayment(
                    PaymentRequestDTO(bookingId = bookingId, amount = amount, paymentMethodId = paymentMethodId)
                )
                handleRetryPaymentResponse(bookingId, response)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    fun retryPaymentWithNewCard(bookingId: Long, amount: Double, cardNumber: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                val response = api.processPayment(
                    PaymentRequestDTO(bookingId = bookingId, amount = amount, cardNumber = cardNumber)
                )
                handleRetryPaymentResponse(bookingId, response)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    private suspend fun handleRetryPaymentResponse(
        bookingId: Long,
        response: retrofit2.Response<com.tripify.tripify_android.data.model.PaymentResultDTO>
    ) {
        if (response.isSuccessful && response.body()?.success == true) {
            _paymentState.value = PaymentState.Success(bookingId)
            fetchUserBookings()
        } else {
            val message = response.body()?.message ?: response.parseErrorMessage()
            _paymentState.value = PaymentState.Error(message)
        }
    }
}
