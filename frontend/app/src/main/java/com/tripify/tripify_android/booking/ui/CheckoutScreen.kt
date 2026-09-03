package com.tripify.tripify_android.booking.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.component.CardPaymentFormState
import com.tripify.tripify_android.booking.component.CartItemCard
import com.tripify.tripify_android.booking.component.CurrencyPicker
import com.tripify.tripify_android.booking.component.PaymentMethodSection
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.booking.util.convertCartAmount
import com.tripify.tripify_android.booking.util.currencySymbol
import com.tripify.tripify_android.booking.util.isDocumentNumberLengthValid
import com.tripify.tripify_android.booking.util.isTaxCodeChecksumValid
import com.tripify.tripify_android.booking.util.isTaxCodeFormatValid
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.CartItemDTO
import com.tripify.tripify_android.data.model.PassengerRequestDTO
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.data.model.TravelDocumentDto
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

// Stesse liste fisse di Impostazioni > Documenti di viaggio: tipo documento e
// paese si scelgono da un menu anche qui invece di testo libero, così un
// documento inserito qui è nello stesso formato di uno salvato in Impostazioni
// (da cui viene anche precompilato, vedi CheckoutScreen).
private val documentTypeOptions = listOf("Passaporto", "Carta d'Identità", "Patente di Guida")
private val issuingCountryOptions = listOf("ITA", "USA", "GBR", "FRA", "ESP", "DEU")

// Dati di un ospite/partecipante raccolti PRIMA del pagamento (vedi
// CheckoutScreen): oggetto semplice con proprietà mutableStateOf, così le
// modifiche ai singoli campi ricompongono solo la card di quell'ospite,
// tenuto vivo con remember finché si resta sulla schermata di checkout.
private class GuestFieldsState {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var taxCode by mutableStateOf("")
    // Stesso default di Impostazioni > Documenti di viaggio: sono scelti da un
    // menu a opzioni fisse, non testo libero, quindi non sono mai "vuoti".
    var documentType by mutableStateOf(documentTypeOptions.first())
    var documentNumber by mutableStateOf("")
    var documentExpirationDate by mutableStateOf("")
    var issuingCountry by mutableStateOf(issuingCountryOptions.first())

    // Il documento deve restare valido oltre la data odierna: anche una
    // scadenza fissata a oggi non basta, deve essere strettamente successiva
    // (stesso vincolo di @Future lato backend, vedi PassengerRequestDTO).
    private val expirationDateValid: Boolean
        get() = runCatching { LocalDate.parse(documentExpirationDate) }.getOrNull()
            ?.isAfter(LocalDate.now()) == true

    // Ogni errore è null finché il campo è vuoto e non si è ancora tentato di
    // pagare (submitAttempted=false): niente campi rossi appena si apre la
    // schermata. Un campo compilato ma fuori formato mostra subito l'errore.
    fun firstNameError(submitAttempted: Boolean): String? =
        if (submitAttempted && firstName.isBlank()) "Il nome è obbligatorio" else null

    fun lastNameError(submitAttempted: Boolean): String? =
        if (submitAttempted && lastName.isBlank()) "Il cognome è obbligatorio" else null

    fun phoneNumberError(submitAttempted: Boolean): String? = when {
        phoneNumber.isBlank() -> if (submitAttempted) "Il numero di telefono è obbligatorio" else null
        phoneNumber.length != 10 -> "Il numero di telefono deve avere 10 cifre"
        else -> null
    }

    fun taxCodeError(submitAttempted: Boolean): String? = when {
        taxCode.isBlank() -> if (submitAttempted) "Il codice fiscale è obbligatorio" else null
        !isTaxCodeFormatValid(taxCode) -> "Codice fiscale non valido (16 caratteri, es. RSSMRA80A01H501U)"
        !isTaxCodeChecksumValid(taxCode) -> "Codice fiscale non valido: il carattere di controllo non corrisponde"
        else -> null
    }

    fun documentTypeError(submitAttempted: Boolean): String? =
        if (submitAttempted && documentType.isBlank()) "Il tipo di documento è obbligatorio" else null

    fun documentNumberError(submitAttempted: Boolean): String? = when {
        documentNumber.isBlank() -> if (submitAttempted) "Il numero di documento è obbligatorio" else null
        !isDocumentNumberLengthValid(documentNumber) -> "Il numero di documento deve avere tra 5 e 20 caratteri"
        else -> null
    }

    fun documentExpirationDateError(submitAttempted: Boolean): String? = when {
        documentExpirationDate.isBlank() -> if (submitAttempted) "Seleziona una data di scadenza" else null
        !expirationDateValid -> "Documento scaduto! Impossibile effettuare la prenotazione con questo documento."
        else -> null
    }

    fun issuingCountryError(submitAttempted: Boolean): String? = when {
        issuingCountry.isBlank() -> if (submitAttempted) "Il paese di rilascio è obbligatorio" else null
        issuingCountry.length != 3 -> "Deve essere un codice ISO a 3 lettere (es. ITA)"
        else -> null
    }

    val isValid: Boolean
        get() = firstNameError(true) == null && lastNameError(true) == null &&
            phoneNumberError(true) == null && taxCodeError(true) == null &&
            documentTypeError(true) == null && documentNumberError(true) == null &&
            documentExpirationDateError(true) == null && issuingCountryError(true) == null

    fun toRequest() = PassengerRequestDTO(
        firstName = firstName,
        lastName = lastName,
        phoneNumber = phoneNumber,
        taxCode = taxCode,
        documentType = documentType,
        documentNumber = documentNumber,
        documentExpirationDate = documentExpirationDate,
        issuingCountry = issuingCountry
    )
}

// Riepilogo ordine + pagamento simulato (vedi PaymentService lato booking-service:
// non è un vero PSP, basta una card "plausibile" lunga almeno 12 cifre). Scadenza,
// CVV e intestatario sono validati solo lato client: il mock non li richiede, ma un
// form di pagamento senza questi controlli non sembra affidabile a un utente vero.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CartViewModel,
    catalogViewModel: CatalogViewModel,
    bookingViewModel: BookingViewModel,
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: (bookingId: Long) -> Unit = {}
) {
    val cartState by viewModel.uiState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val savedMethods by viewModel.savedPaymentMethods.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val savedTravelDocuments by bookingViewModel.savedTravelDocuments.collectAsState()
    val myPhoneNumber by bookingViewModel.myPhoneNumber.collectAsState()

    // Stessa valuta di CartScreen (storage condiviso), modificabile anche
    // qui. Converte solo la cifra mostrata: l'importo pagato resta sempre
    // booking.totalAmount calcolato dal server.
    val displayCurrency by rememberCatalogCurrency()
    val currencyContext = LocalContext.current
    val currencyTokenManager = remember { TokenManager(currencyContext) }
    val currencyScope = rememberCoroutineScope()

    // Stato del metodo di pagamento (salvato o carta nuova) e la sua
    // validazione: condiviso con RetryPaymentScreen, vedi CardPaymentForm.kt.
    val paymentFormState = remember { CardPaymentFormState() }

    // Diventa true al primo tentativo di pagamento con dati non validi: da lì in
    // poi ogni campo obbligatorio ancora vuoto si segnala in rosso. I campi
    // compilati ma fuori formato (es. carta troppo corta) si segnalano subito,
    // senza aspettare un tentativo di pagamento.
    var submitAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchCart()
        viewModel.fetchSavedPaymentMethods()
        viewModel.resetPaymentState()
        bookingViewModel.fetchSavedTravelDocuments()
        bookingViewModel.fetchMyPhoneNumber()
    }

    LaunchedEffect(paymentState) {
        val state = paymentState
        if (state is PaymentState.Success) {
            // Se è stato salvato un nuovo documento la lista qui è ancora
            // quella di prima: la aggiorniamo per coerenza anche se si sta
            // per lasciare la schermata.
            bookingViewModel.fetchSavedTravelDocuments()
            onPaymentSuccess(state.bookingId)
        }
    }

    val cart = (cartState as? CartState.Success)?.cart
    // Solo gli articoli scelti in CartScreen vengono mostrati/pagati qui: gli
    // altri restano nel carrello (vedi CartViewModel.selectedItemIds).
    val selectedItems = cart?.items?.filter { it.id in selectedItemIds } ?: emptyList()
    // Ogni articolo si converte dalla sua valuta originale a quella scelta:
    // sommare i priceAtAdded grezzi avrebbe senso solo se fossero tutti
    // nella stessa valuta.
    val selectedTotal = selectedItems.sumOf {
        convertCartAmount(it.priceAtAdded, it.currency, displayCurrency) * it.quantity
    }

    // Un set di campi ospite per ogni "posto" acquistato (quantity) su ogni
    // articolo selezionato, tenuti vivi per tutta la sessione di checkout;
    // si ricreano solo se cambia la selezione fatta in CartScreen.
    val guestsByItemId = remember(selectedItemIds) {
        selectedItems.associate { item -> item.id to List(item.quantity) { GuestFieldsState() } }
    }

    // Documento per precompilare il primo ospite di ogni articolo: uno
    // salvato (selettore sotto, stesso pattern del metodo di pagamento)
    // oppure "Nuovo documento" (selectedDocumentId=null), con la checkbox
    // per salvarlo.
    var selectedDocumentId by remember { mutableStateOf<String?>(null) }
    // Come CardPaymentFormState.hasAutoSelected: la preselezione scatta una
    // sola volta, poi la scelta dell'utente non viene più sovrascritta.
    var hasAutoSelectedDocument by remember { mutableStateOf(false) }
    var saveNewDocument by remember { mutableStateOf(false) }

    fun applyDocumentToFirstGuests(document: TravelDocumentDto) {
        guestsByItemId.values.forEach { guests ->
            val firstGuest = guests.firstOrNull() ?: return@forEach
            firstGuest.documentType = document.documentType
            firstGuest.documentNumber = document.documentNumber
            firstGuest.documentExpirationDate = document.expirationDate
            firstGuest.issuingCountry = document.issuingCountry
        }
    }

    LaunchedEffect(savedTravelDocuments) {
        if (!hasAutoSelectedDocument && savedTravelDocuments.isNotEmpty()) {
            selectedDocumentId = savedTravelDocuments.first().id
            hasAutoSelectedDocument = true
        }
    }

    // Precompila solo il primo ospite di ciascun articolo: gli altri sono
    // persone diverse, il documento va inserito a mano. Il guard su
    // documentNumber evita di sovrascrivere dati già digitati prima che la
    // lista arrivasse dal server.
    LaunchedEffect(selectedDocumentId, savedTravelDocuments, selectedItemIds) {
        val chosenDocument = savedTravelDocuments.firstOrNull { it.id == selectedDocumentId } ?: return@LaunchedEffect
        guestsByItemId.values.forEach { guests ->
            val firstGuest = guests.firstOrNull() ?: return@forEach
            if (firstGuest.documentNumber.isBlank()) {
                firstGuest.documentType = chosenDocument.documentType
                firstGuest.documentNumber = chosenDocument.documentNumber
                firstGuest.documentExpirationDate = chosenDocument.expirationDate
                firstGuest.issuingCountry = chosenDocument.issuingCountry
            }
        }
    }

    // Il telefono in Impostazioni è testo libero, nessun formato imposto:
    // autocompiliamo solo se restano esattamente 10 cifre dopo aver tolto
    // spazi/trattini, altrimenti rischieremmo di troncare male un numero con
    // prefisso internazionale e lasciamo il campo vuoto.
    LaunchedEffect(myPhoneNumber, selectedItemIds) {
        val digitsOnly = myPhoneNumber?.filter { it.isDigit() }
        if (digitsOnly?.length != 10) return@LaunchedEffect
        guestsByItemId.values.forEach { guests ->
            val firstGuest = guests.firstOrNull() ?: return@forEach
            if (firstGuest.phoneNumber.isBlank()) {
                firstGuest.phoneNumber = digitsOnly
            }
        }
    }

    val allGuestsValid = guestsByItemId.values.all { guests -> guests.all { it.isValid } }
    val formValid = paymentFormState.isValid && allGuestsValid

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pagamento", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        },
        bottomBar = {
            if (selectedItems.isNotEmpty()) {
                Surface(color = CatalogColors.Surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        CurrencyPicker(
                            selected = displayCurrency,
                            onSelect = { currency -> currencyScope.launch { currencyTokenManager.setCurrency(currency) } }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale da pagare (${selectedItems.size} articoli)", style = CatalogType.Body, color = CatalogColors.InkMuted)
                            Text(
                                text = "${currencySymbol(displayCurrency)} ${"%.2f".format(selectedTotal)}",
                                style = CatalogType.PriceLarge,
                                color = CatalogColors.AccentDark
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (!formValid) {
                                    // Niente chiamata di rete: si accende solo la segnalazione
                                    // rossa sui campi ancora vuoti o fuori formato.
                                    submitAttempted = true
                                } else {
                                    val guestsRequest = guestsByItemId.mapValues { (_, guests) -> guests.map { it.toRequest() } }
                                    // Non null solo se scelto "Nuovo documento" e spuntato di
                                    // salvarlo: si prende il primo ospite del primo articolo,
                                    // stessa convenzione di sopra.
                                    val documentToSave = if (selectedDocumentId == null && saveNewDocument) {
                                        selectedItems.firstOrNull()
                                            ?.let { guestsByItemId[it.id]?.firstOrNull() }
                                            ?.let { guest ->
                                                TravelDocumentDto(
                                                    documentType = guest.documentType,
                                                    documentNumber = guest.documentNumber,
                                                    expirationDate = guest.documentExpirationDate,
                                                    issuingCountry = guest.issuingCountry
                                                )
                                            }
                                    } else null
                                    val savedId = paymentFormState.selectedSavedMethodId
                                    if (savedId != null) {
                                        viewModel.payWithSavedMethod(savedId, guestsRequest, documentToSave)
                                    } else {
                                        viewModel.payWithNewCard(
                                            cardNumber = paymentFormState.cardNumber,
                                            cardProvider = paymentFormState.cardProvider,
                                            expirationMonthYear = paymentFormState.expirationMonthYear(),
                                            saveCard = paymentFormState.saveNewCard,
                                            guestsByCartItemId = guestsRequest,
                                            documentToSave = documentToSave
                                        )
                                    }
                                }
                            },
                            enabled = paymentState !is PaymentState.Processing,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Field
                        ) {
                            if (paymentState is PaymentState.Processing) {
                                CircularProgressIndicator(
                                    color = CatalogColors.Surface,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Paga ${currencySymbol(displayCurrency)} ${"%.2f".format(selectedTotal)}", style = CatalogType.Button)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (cart == null || selectedItems.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (cartState is CartState.Loading) {
                    CircularProgressIndicator(color = CatalogColors.AccentDark)
                } else {
                    Text("Nessun articolo selezionato per il pagamento", style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                item {
                    Text(
                        text = "Riepilogo ordine",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                items(selectedItems, key = { it.id }) { item ->
                    CartItemCard(item = item, catalogViewModel = catalogViewModel, selected = true, showControls = false)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dati degli ospiti",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Servono per registrare chi viaggia su ogni prenotazione.",
                        style = CatalogType.Caption,
                        color = CatalogColors.InkMuted,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Text(
                        text = "Documento da usare",
                        style = CatalogType.Caption,
                        color = CatalogColors.InkMuted,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    savedTravelDocuments.forEach { document ->
                        SavedDocumentRow(
                            document = document,
                            selected = selectedDocumentId == document.id,
                            onClick = {
                                selectedDocumentId = document.id
                                applyDocumentToFirstGuests(document)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    NewDocumentOptionRow(
                        selected = selectedDocumentId == null,
                        onClick = { selectedDocumentId = null }
                    )

                    if (selectedDocumentId == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                .clickable { saveNewDocument = !saveNewDocument }
                        ) {
                            Checkbox(
                                checked = saveNewDocument,
                                onCheckedChange = { saveNewDocument = it },
                                colors = CheckboxDefaults.colors(checkedColor = CatalogColors.AccentDark)
                            )
                            Text(
                                "Salva questo documento per i prossimi acquisti",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                selectedItems.forEach { cartItem ->
                    item(key = "guests-${cartItem.id}") {
                        GuestFieldsForItem(
                            item = cartItem,
                            catalogViewModel = catalogViewModel,
                            guests = guestsByItemId[cartItem.id].orEmpty(),
                            submitAttempted = submitAttempted
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Metodo di pagamento",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    PaymentMethodSection(
                        state = paymentFormState,
                        savedMethods = savedMethods,
                        submitAttempted = submitAttempted
                    )

                    val paymentError = paymentState
                    if (paymentError is PaymentState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = paymentError.message,
                            style = CatalogType.Body,
                            color = CatalogColors.Alert,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Stesso stile di SavedPaymentMethodRow (radio button, non chip come in
// AddPassengersScreen), per coerenza con l'altro selettore in questa schermata.
@Composable
private fun SavedDocumentRow(
    document: TravelDocumentDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CatalogShapes.Field,
        color = if (selected) CatalogColors.AccentSoft else CatalogColors.Surface,
        border = BorderStroke(1.dp, if (selected) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "${document.documentType} · ${document.documentNumber}",
                    style = CatalogType.BodyStrong,
                    color = CatalogColors.Ink
                )
                Text("Scadenza ${document.expirationDate}", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            }
        }
    }
}

// Stesso stile visivo di NewCardOptionRow (PaymentMethodSection).
@Composable
private fun NewDocumentOptionRow(selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CatalogShapes.Field,
        color = if (selected) CatalogColors.AccentSoft else CatalogColors.Surface,
        border = BorderStroke(1.dp, if (selected) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = CatalogColors.Accent)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Nuovo documento", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
        }
    }
}

// Un ospite per ogni "posto" acquistato (item.quantity) sull'articolo, con il
// titolo dell'articolo risolto dal catalogo come nel resto del carrello.
@Composable
private fun GuestFieldsForItem(
    item: CartItemDTO,
    catalogViewModel: CatalogViewModel,
    guests: List<GuestFieldsState>,
    submitAttempted: Boolean
) {
    var resolvedTitle by remember(item.catalogItemId) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.catalogItemId) {
        resolvedTitle = catalogViewModel.getOrFetchItem(item.catalogItemId.toInt())?.title
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = resolvedTitle ?: "Articolo #${item.catalogItemId}",
            style = CatalogType.BodyStrong,
            color = CatalogColors.Ink
        )
        Spacer(modifier = Modifier.height(8.dp))
        guests.forEachIndexed { index, guest ->
            GuestFieldsCard(index = index + 1, state = guest, submitAttempted = submitAttempted)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestFieldsCard(index: Int, state: GuestFieldsState, submitAttempted: Boolean) {
    val context = LocalContext.current
    val openDatePicker = {
        val today = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day -> state.documentExpirationDate = "%04d-%02d-%02d".format(year, month + 1, day) },
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val firstNameError = state.firstNameError(submitAttempted)
    val lastNameError = state.lastNameError(submitAttempted)
    val phoneNumberError = state.phoneNumberError(submitAttempted)
    val taxCodeError = state.taxCodeError(submitAttempted)
    val documentTypeError = state.documentTypeError(submitAttempted)
    val issuingCountryError = state.issuingCountryError(submitAttempted)
    val documentNumberError = state.documentNumberError(submitAttempted)
    val documentExpirationDateError = state.documentExpirationDateError(submitAttempted)

    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Ospite $index", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.firstName, onValueChange = { state.firstName = it },
                    label = { Text("Nome") },
                    isError = firstNameError != null,
                    supportingText = firstNameError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.lastName, onValueChange = { state.lastName = it },
                    label = { Text("Cognome") },
                    isError = lastNameError != null,
                    supportingText = lastNameError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = { value -> state.phoneNumber = value.filter { it.isDigit() }.take(10) },
                label = { Text("Telefono") },
                isError = phoneNumberError != null,
                supportingText = phoneNumberError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.taxCode,
                onValueChange = { value -> state.taxCode = value.filter { it.isLetterOrDigit() }.uppercase().take(16) },
                label = { Text("Codice fiscale") },
                isError = taxCodeError != null,
                supportingText = taxCodeError?.let { { Text(it) } },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Stesso menu a opzioni fisse di Impostazioni > Documenti di
                // viaggio, non più testo libero (vedi documentTypeOptions).
                var documentTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = documentTypeExpanded,
                    onExpandedChange = { documentTypeExpanded = !documentTypeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.documentType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo documento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = documentTypeExpanded) },
                        isError = documentTypeError != null,
                        supportingText = documentTypeError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = documentTypeExpanded,
                        onDismissRequest = { documentTypeExpanded = false }
                    ) {
                        documentTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    state.documentType = option
                                    documentTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                var issuingCountryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = issuingCountryExpanded,
                    onExpandedChange = { issuingCountryExpanded = !issuingCountryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.issuingCountry,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paese") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = issuingCountryExpanded) },
                        isError = issuingCountryError != null,
                        supportingText = issuingCountryError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = issuingCountryExpanded,
                        onDismissRequest = { issuingCountryExpanded = false }
                    ) {
                        issuingCountryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    state.issuingCountry = option
                                    issuingCountryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.documentNumber,
                onValueChange = { value -> state.documentNumber = value.filter { it.isLetterOrDigit() }.uppercase().take(20) },
                label = { Text("Numero documento") },
                isError = documentNumberError != null,
                supportingText = documentNumberError?.let { { Text(it) } },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.documentExpirationDate, onValueChange = {},
                label = { Text("Scadenza documento") }, placeholder = { Text("Seleziona una data") },
                isError = documentExpirationDateError != null,
                supportingText = documentExpirationDateError?.let { { Text(it) } },
                readOnly = true, singleLine = true,
                trailingIcon = {
                    IconButton(onClick = openDatePicker) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Scegli data")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
