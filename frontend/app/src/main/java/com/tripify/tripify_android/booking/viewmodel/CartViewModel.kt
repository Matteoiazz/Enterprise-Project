package com.tripify.tripify_android.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.AddToCartRequestDTO
import com.tripify.tripify_android.data.model.BookingResponseDTO
import com.tripify.tripify_android.data.model.CheckoutRequestDTO
import com.tripify.tripify_android.data.model.PassengerRequestDTO
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.data.model.PaymentRequestDTO
import com.tripify.tripify_android.data.model.TravelDocumentDto
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

    // Articoli del carrello scelti per il prossimo checkout: di default tutti,
    // l'utente può togliere la spunta a quelli che non vuole prenotare ora.
    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds

    fun toggleItemSelection(itemId: Long) {
        _selectedItemIds.value = if (itemId in _selectedItemIds.value) {
            _selectedItemIds.value - itemId
        } else {
            _selectedItemIds.value + itemId
        }
    }

    // Funzione che verrà chiamata dalla UI per caricare il carrello. Non serve
    // più passare l'userId: il backend lo ricava dal JWT (vedi BookingApi).
    fun fetchCart() {
        viewModelScope.launch {
            _uiState.value = CartState.Loading
            try {
                val response = api.getCart()

                if (response.isSuccessful && response.body() != null) {
                    val cart = response.body()!!
                    _uiState.value = CartState.Success(cart)
                    // Ogni volta che il carrello viene ricaricato si riparte con tutto
                    // selezionato: più semplice e prevedibile che provare a ricordare
                    // le deselezioni tra un fetch e l'altro (es. dopo aver rimosso un articolo).
                    _selectedItemIds.value = cart.items.map { it.id }.toSet()
                } else {
                    _uiState.value = CartState.Error("Errore nel caricamento del carrello: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = CartState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Rimuove un singolo articolo dal carrello (rilascia il suo eventuale hold lato server).
    fun removeItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val response = api.removeCartItem(itemId)
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

    // Elenco dei metodi di pagamento già salvati (proxy verso user-auth-service
    // esposto da booking-service): se la chiamata fallisce non blocchiamo il
    // checkout, l'utente può comunque pagare inserendo una carta nuova.
    fun fetchSavedPaymentMethods() {
        viewModelScope.launch {
            try {
                // profileApi restituisce direttamente la lista (non un Response<List>)
                val methods = profileApi.getPaymentMethods()
                _savedPaymentMethods.value = methods
            } catch (e: Exception) {
                android.util.Log.w("CartViewModel", "fetchSavedPaymentMethods fallita", e)
            }
        }
    }

    // Nucleo comune a entrambi i flussi di pagamento: trasforma il carrello in
    // una Booking PENDING (checkout) poi processa il pagamento su quella Booking.
    // Sono due chiamate distinte lato backend perché il checkout può fallire per
    // motivi diversi dal pagamento (carrello vuoto, hold scaduto).
    // guestsByCartItemId: dati degli ospiti raccolti in CheckoutScreen PRIMA di
    // pagare, inviati SUBITO DOPO un pagamento riuscito (servono gli id delle
    // BookingLine appena create, che non esistono prima). L'abbinamento tra
    // articolo del carrello e riga della Booking NON può essere per posizione:
    // se un articolo scade (hold purge) tra il caricamento del carrello mostrato
    // in CheckoutScreen e questa chiamata, il checkout lato server produce meno
    // righe di quelle previste e uno zip per indice sposterebbe gli ospiti sulla
    // riga sbagliata. Si abbina invece per "firma" (stesso catalogItemId/
    // roomTypeId/fareClassId/checkIn/checkOut), consumando ogni riga una sola
    // volta: un articolo senza più una riga corrispondente perde solo i suoi
    // ospiti (recuperabili dopo da "Le mie prenotazioni"), non li assegna a un
    // altro articolo.
    // Stessa chiave riusata finché non arriva un pagamento riuscito (vedi
    // resetPaymentState): se la risposta del checkout si perde per un
    // problema di rete e l'utente ritenta, il backend riconosce lo stesso
    // tentativo e restituisce la Booking già creata invece di un "carrello
    // vuoto" (il primo tentativo l'ha già svuotato). Aiuta anche un pagamento
    // rifiutato e ritentato: la Booking PENDING viene riusata invece di
    // fallire con carrello vuoto.
    private var checkoutIdempotencyKey: String? = null

    private suspend fun checkoutThenPay(
        guestsByCartItemId: Map<Long, List<PassengerRequestDTO>> = emptyMap(),
        buildRequest: (booking: BookingResponseDTO) -> PaymentRequestDTO
    ): BookingResponseDTO? {
        val selectedIds = _selectedItemIds.value
        val orderedSelectedItems = (_uiState.value as? CartState.Success)?.cart?.items
            ?.filter { it.id in selectedIds }.orEmpty()

        val idempotencyKey = checkoutIdempotencyKey
            ?: java.util.UUID.randomUUID().toString().also { checkoutIdempotencyKey = it }

        val checkoutResponse = api.checkout(idempotencyKey, CheckoutRequestDTO(cartItemIds = selectedIds.toList()))
        if (!checkoutResponse.isSuccessful || checkoutResponse.body() == null) {
            _paymentState.value = PaymentState.Error(checkoutResponse.parseErrorMessage())
            return null
        }
        val booking = checkoutResponse.body()!!

        val paymentResponse = api.processPayment(buildRequest(booking))

        return if (paymentResponse.isSuccessful && paymentResponse.body()?.success == true) {
            _paymentState.value = PaymentState.Success(booking.id)
            checkoutIdempotencyKey = null

            val remainingLines = booking.lines.toMutableList()
            orderedSelectedItems.forEach { cartItem ->
                val lineIndex = remainingLines.indexOfFirst { line ->
                    line.catalogItemId == cartItem.catalogItemId &&
                        line.roomTypeId == cartItem.roomTypeId &&
                        line.fareClassId == cartItem.fareClassId &&
                        line.checkIn == cartItem.checkIn &&
                        line.checkOut == cartItem.checkOut
                }
                if (lineIndex == -1) return@forEach
                val line = remainingLines.removeAt(lineIndex)

                guestsByCartItemId[cartItem.id].orEmpty().forEach { guest ->
                    try {
                        api.addPassenger(line.id, guest)
                    } catch (e: Exception) {
                        // Il pagamento è comunque riuscito: un ospite non registrato si
                        // può sempre aggiungere dopo da "Le mie prenotazioni".
                        android.util.Log.w("CartViewModel", "addPassenger fallita per la riga ${line.id}", e)
                    }
                }
            }

            booking
        } else {
            val message = paymentResponse.body()?.message ?: paymentResponse.parseErrorMessage()
            _paymentState.value = PaymentState.Error(message)
            null
        }
    }

    // Paga con un metodo già salvato in Impostazioni: solo il suo id viaggia
    // verso il backend, mai un numero di carta (che non viene più chiesto).
    // documentToSave: vedi payWithNewCard, si applica indipendentemente da
    // quale metodo di pagamento si sta usando.
    fun payWithSavedMethod(
        paymentMethodId: String,
        guestsByCartItemId: Map<Long, List<PassengerRequestDTO>> = emptyMap(),
        documentToSave: TravelDocumentDto? = null
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                val booking = checkoutThenPay(guestsByCartItemId) { booking ->
                    PaymentRequestDTO(bookingId = booking.id, amount = booking.totalAmount, paymentMethodId = paymentMethodId)
                }
                saveNewDocumentIfNeeded(booking, documentToSave)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Paga con una carta inserita al momento. Se saveCard è true, dopo un
    // pagamento riuscito la carta viene aggiunta anche ai metodi salvati in
    // Impostazioni Profilo: un suo eventuale fallimento non deve far sembrare
    // fallito il pagamento appena andato a buon fine, quindi viene ignorato.
    // documentToSave: stesso criterio, ma per il documento del primo ospite -
    // non null solo se l'utente ha scelto "Nuovo documento" e spuntato di salvarlo.
    fun payWithNewCard(
        cardNumber: String,
        cardProvider: String,
        expirationMonthYear: String,
        saveCard: Boolean,
        guestsByCartItemId: Map<Long, List<PassengerRequestDTO>> = emptyMap(),
        documentToSave: TravelDocumentDto? = null
    ) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            try {
                val booking = checkoutThenPay(guestsByCartItemId) { b ->
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
                saveNewDocumentIfNeeded(booking, documentToSave)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }

    // Stesso criterio di saveCard: un fallimento nel salvare il documento non
    // deve far sembrare fallito un pagamento già andato a buon fine.
    private suspend fun saveNewDocumentIfNeeded(booking: BookingResponseDTO?, documentToSave: TravelDocumentDto?) {
        if (booking == null || documentToSave == null) return
        try {
            profileApi.addTravelDocument(documentToSave)
        } catch (e: Exception) {
            android.util.Log.w("CartViewModel", "addTravelDocument fallita", e)
        }
    }

    // Chiamato all'ingresso in CheckoutScreen: azzera anche la chiave di
    // idempotenza, così una nuova visita alla schermata parte come un
    // tentativo nuovo invece di riusare quella di un checkout abbandonato.
    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
        checkoutIdempotencyKey = null
    }
}
