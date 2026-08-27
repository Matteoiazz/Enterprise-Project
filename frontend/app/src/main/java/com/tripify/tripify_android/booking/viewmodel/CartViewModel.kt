package com.tripify.tripify_android.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.AddToCartRequestDTO
import com.tripify.tripify_android.data.model.BookingResponseDTO
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.data.model.PaymentRequestDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.tripify.tripify_android.data.parseErrorMessage

class CartViewModel(private val tokenManager: TokenManager) : ViewModel() {

    // Creiamo l'istanza dell'API passandole il token per l'autorizzazione
    private val api = RetrofitClient.createBookingApi(tokenManager)

    // Serve solo per salvare in Impostazioni una carta appena inserita al checkout
    // (l'utente lo decide con la checkbox "Salva questo metodo di pagamento").
    private val profileApi = RetrofitClient.createProfileApi(tokenManager)

    // Variabile che contiene lo stato attuale (di default parte in Loading)
    private val _uiState = MutableStateFlow<CartState>(CartState.Loading)
    val uiState: StateFlow<CartState> = _uiState

    // Stato del flusso di pagamento in CheckoutScreen, separato dal carrello:
    // qui il risultato è una Booking pagata, non più un CartDTO.
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    // Metodi di pagamento già salvati in Impostazioni Profilo, mostrati in
    // CheckoutScreen per evitare di dover reinserire una carta ogni volta.
    private val _savedPaymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val savedPaymentMethods: StateFlow<List<PaymentMethodDto>> = _savedPaymentMethods

    // Funzione che verrà chiamata dalla UI per caricare il carrello. Non serve
    // più passare l'userId: il backend lo ricava dal JWT (vedi BookingApi).
    fun fetchCart() {
        viewModelScope.launch {
            _uiState.value = CartState.Loading
            try {
                val response = api.getCart()

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = CartState.Success(response.body()!!)
                } else {
                    _uiState.value = CartState.Error("Errore nel caricamento del carrello: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = CartState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Da agganciare al bottone "Aggiungi" nella UI. roomTypeId/fareClassId/
    // checkIn/checkOut vanno passati solo per camere d'hotel o posti su un volo.
    fun addItemToCart(
        catalogItemId: Long,
        quantity: Int,
        roomTypeId: Long? = null,
        fareClassId: Long? = null,
        checkIn: String? = null,
        checkOut: String? = null,
        onResult: (success: Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = api.addToCart(
                    AddToCartRequestDTO(catalogItemId, quantity, roomTypeId, fareClassId, checkIn, checkOut)
                )

                if (response.isSuccessful) {
                    fetchCart()
                    onResult(true)
                } else {
                    // ALTRA MAGIA! Mostrerà es. "Articolo non trovato nel catalogo!"
                    val cleanError = response.parseErrorMessage()
                    _uiState.value = CartState.Error(cleanError)
                    onResult(false)
                }
            } catch (e: Exception) {
                _uiState.value = CartState.Error("Nessuna connessione: ${e.message}")
                onResult(false)
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                val response = api.clearCart()
                if (response.isSuccessful) {
                    fetchCart()
                } else {
                    _uiState.value = CartState.Error(response.parseErrorMessage())
                }
            } catch (e: Exception) {
                _uiState.value = CartState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Trasforma il carrello corrente in una prenotazione PENDING. Callback
    // esplicite (invece di passare dallo uiState del carrello) perché il
    // risultato è una Booking, non più un CartDTO.
    fun checkout(onSuccess: (bookingId: Long) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.checkout()
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!.id)
                    fetchCart()
                } else {
                    onError(response.parseErrorMessage())
                }
            } catch (e: Exception) {
                onError("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Elenco dei metodi di pagamento già salvati (proxy verso user-auth-service
    // esposto da booking-service): se la chiamata fallisce non blocchiamo il
    // checkout, l'utente può comunque pagare inserendo una carta nuova.
    fun fetchSavedPaymentMethods() {
        viewModelScope.launch {
            try {
                val response = api.getSavedPaymentMethods()
                if (response.isSuccessful) {
                    _savedPaymentMethods.value = response.body().orEmpty()
                }
            } catch (e: Exception) {
                // silenzioso: vedi commento sopra
            }
        }
    }

    // Nucleo comune a entrambi i flussi di pagamento: trasforma il carrello in
    // una Booking PENDING (checkout) poi processa il pagamento su quella Booking.
    // Sono due chiamate distinte lato backend perché il checkout può fallire per
    // motivi diversi dal pagamento (carrello vuoto, hold scaduto).
    private suspend fun checkoutThenPay(buildRequest: (booking: BookingResponseDTO) -> PaymentRequestDTO): BookingResponseDTO? {
        val checkoutResponse = api.checkout()
        if (!checkoutResponse.isSuccessful || checkoutResponse.body() == null) {
            _paymentState.value = PaymentState.Error(checkoutResponse.parseErrorMessage())
            return null
        }
        val booking = checkoutResponse.body()!!

        val paymentResponse = api.processPayment(buildRequest(booking))

        return if (paymentResponse.isSuccessful && paymentResponse.body()?.success == true) {
            _paymentState.value = PaymentState.Success(booking.id)
            booking
        } else {
            val message = paymentResponse.body()?.message ?: paymentResponse.parseErrorMessage()
            _paymentState.value = PaymentState.Error(message)
            null
        }
    }

    // Paga con un metodo già salvato in Impostazioni: solo il suo id viaggia
    // verso il backend, mai un numero di carta (che non viene più chiesto).
    fun payWithSavedMethod(paymentMethodId: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                checkoutThenPay { booking ->
                    PaymentRequestDTO(bookingId = booking.id, amount = booking.totalAmount, paymentMethodId = paymentMethodId)
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Paga con una carta inserita al momento. Se saveCard è true, dopo un
    // pagamento riuscito la carta viene aggiunta anche ai metodi salvati in
    // Impostazioni Profilo: un suo eventuale fallimento non deve far sembrare
    // fallito il pagamento appena andato a buon fine, quindi viene ignorato.
    fun payWithNewCard(cardNumber: String, cardProvider: String, expirationMonthYear: String, saveCard: Boolean) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                val booking = checkoutThenPay { b ->
                    PaymentRequestDTO(bookingId = b.id, amount = b.totalAmount, cardNumber = cardNumber)
                }
                if (booking != null && saveCard) {
                    try {
                        profileApi.addPaymentMethod(
                            PaymentMethodDto(cardProvider = cardProvider, cardNumber = cardNumber, expirationMonthYear = expirationMonthYear)
                        )
                        fetchSavedPaymentMethods()
                    } catch (e: Exception) {
                        // vedi commento sopra: il pagamento è comunque riuscito
                    }
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }
}
