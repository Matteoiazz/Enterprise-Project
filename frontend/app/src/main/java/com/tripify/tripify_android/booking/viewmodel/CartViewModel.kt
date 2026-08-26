package com.tripify.tripify_android.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.AddToCartRequestDTO
import com.tripify.tripify_android.data.model.PaymentRequestDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.tripify.tripify_android.data.parseErrorMessage

class CartViewModel(private val tokenManager: TokenManager) : ViewModel() {

    // Creiamo l'istanza dell'API passandole il token per l'autorizzazione
    private val api = RetrofitClient.createBookingApi(tokenManager)

    // Variabile che contiene lo stato attuale (di default parte in Loading)
    private val _uiState = MutableStateFlow<CartState>(CartState.Loading)
    val uiState: StateFlow<CartState> = _uiState

    // Stato del flusso di pagamento in CheckoutScreen, separato dal carrello:
    // qui il risultato è una Booking pagata, non più un CartDTO.
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

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
        checkOut: String? = null
    ) {
        viewModelScope.launch {
            try {
                val response = api.addToCart(
                    AddToCartRequestDTO(catalogItemId, quantity, roomTypeId, fareClassId, checkIn, checkOut)
                )

                if (response.isSuccessful) {
                    fetchCart()
                } else {
                    // ALTRA MAGIA! Mostrerà es. "Articolo non trovato nel catalogo!"
                    val cleanError = response.parseErrorMessage()
                    _uiState.value = CartState.Error(cleanError)
                }
            } catch (e: Exception) {
                _uiState.value = CartState.Error("Nessuna connessione: ${e.message}")
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

    // Flusso completo di pagamento usato da CheckoutScreen: prima trasforma il
    // carrello in una Booking PENDING (checkout), poi processa il pagamento su
    // quella Booking. Sono due chiamate distinte lato backend perché il
    // checkout può fallire per motivi diversi dal pagamento (carrello vuoto,
    // hold scaduto), ma per la UI del carrello è un'unica azione "Paga".
    fun payCart(cardNumber: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                val checkoutResponse = api.checkout()
                if (!checkoutResponse.isSuccessful || checkoutResponse.body() == null) {
                    _paymentState.value = PaymentState.Error(checkoutResponse.parseErrorMessage())
                    return@launch
                }
                val booking = checkoutResponse.body()!!

                val paymentResponse = api.processPayment(
                    PaymentRequestDTO(booking.id, cardNumber, booking.totalAmount)
                )

                if (paymentResponse.isSuccessful && paymentResponse.body()?.success == true) {
                    _paymentState.value = PaymentState.Success(booking.id)
                } else {
                    val message = paymentResponse.body()?.message ?: paymentResponse.parseErrorMessage()
                    _paymentState.value = PaymentState.Error(message)
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
