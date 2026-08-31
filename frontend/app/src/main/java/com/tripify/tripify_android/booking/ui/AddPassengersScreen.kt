package com.tripify.tripify_android.booking.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.util.isDocumentNumberLengthValid
import com.tripify.tripify_android.booking.util.isTaxCodeChecksumValid
import com.tripify.tripify_android.booking.util.isTaxCodeFormatValid
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.BookingLineDTO
import com.tripify.tripify_android.data.model.PassengerRequestDTO
import com.tripify.tripify_android.data.model.TravelDocumentDto
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

// Schermata raggiungibile solo dal Leader di un viaggio (vedi BookingCard):
// permette di associare un passeggero a una riga già pagata, riusando un
// documento già salvato in Impostazioni Profilo per non doverlo reinserire.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPassengersScreen(
    viewModel: BookingViewModel,
    catalogViewModel: CatalogViewModel,
    bookingId: Long,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedDocuments by viewModel.savedTravelDocuments.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var lineForNewPassenger by remember { mutableStateOf<BookingLineDTO?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchSavedTravelDocuments()
        if (uiState !is BookingState.Success) {
            viewModel.fetchUserBookings()
        }
    }

    val booking = (uiState as? BookingState.Success)?.bookings?.find { it.id == bookingId }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Aggiungi passeggeri", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        }
    ) { innerPadding ->
        if (booking == null) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CatalogColors.AccentDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(booking.lines, key = { it.id }) { line ->
                    BookingLineRow(
                        line = line,
                        catalogViewModel = catalogViewModel,
                        onAddPassengerClick = { lineForNewPassenger = line }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    val targetLine = lineForNewPassenger
    if (targetLine != null) {
        AddPassengerDialog(
            savedDocuments = savedDocuments,
            onDismiss = { lineForNewPassenger = null },
            onConfirm = { request ->
                viewModel.addPassenger(
                    bookingLineId = targetLine.id,
                    request = request,
                    onSuccess = { lineForNewPassenger = null },
                    onError = { message ->
                        lineForNewPassenger = null
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            }
        )
    }
}

@Composable
private fun BookingLineRow(line: BookingLineDTO, catalogViewModel: CatalogViewModel, onAddPassengerClick: () -> Unit) {
    var resolved by remember(line.catalogItemId) { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(line.catalogItemId) {
        resolved = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())
    }

    val maxPassengers = line.quantity
    val isFull = maxPassengers != null && line.passengerCount >= maxPassengers

    Surface(
        shape = CatalogShapes.Card,
        color = CatalogColors.Surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Stessa riga foto+nome usata in CartItemCard/BookingCard: qui non
            // deve comparire nessun identificativo della prenotazione, solo cosa
            // si sta prenotando.
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = resolved?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = resolved?.title ?: "Articolo #${line.catalogItemId}",
                    style = CatalogType.CardTitle,
                    color = CatalogColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Passeggeri: ${line.passengerCount}${maxPassengers?.let { "/$it" } ?: ""}",
                style = CatalogType.Body,
                color = CatalogColors.InkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddPassengerClick,
                enabled = !isFull,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                shape = CatalogShapes.Field
            ) {
                Text(if (isFull) "Completo" else "Aggiungi passeggero", style = CatalogType.Button)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPassengerDialog(
    savedDocuments: List<TravelDocumentDto>,
    onDismiss: () -> Unit,
    onConfirm: (PassengerRequestDTO) -> Unit
) {
    val context = LocalContext.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var taxCode by remember { mutableStateOf("") }
    var documentType by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var issuingCountry by remember { mutableStateOf("") }

    // Diventa true al primo tentativo di salvataggio con dati non validi: da lì
    // in poi i campi obbligatori ancora vuoti si segnalano in rosso. Un campo
    // già compilato ma fuori formato (data scaduta, paese troppo corto) si
    // segnala subito, senza aspettare un tentativo di salvataggio.
    var submitAttempted by remember { mutableStateOf(false) }

    // Il documento deve restare valido oltre la data odierna, stesso vincolo
    // di @Future lato backend (vedi PassengerRequestDTO): anche una scadenza
    // fissata a oggi non basta.
    val expirationDateValid = runCatching { LocalDate.parse(expirationDate) }.getOrNull()
        ?.isAfter(LocalDate.now()) == true

    val firstNameError = if (submitAttempted && firstName.isBlank()) "Il nome è obbligatorio" else null
    val lastNameError = if (submitAttempted && lastName.isBlank()) "Il cognome è obbligatorio" else null
    val phoneNumberError = when {
        phoneNumber.isBlank() -> if (submitAttempted) "Il numero di telefono è obbligatorio" else null
        phoneNumber.length != 10 -> "Il numero di telefono deve avere 10 cifre"
        else -> null
    }
    val taxCodeError = when {
        taxCode.isBlank() -> if (submitAttempted) "Il codice fiscale è obbligatorio" else null
        !isTaxCodeFormatValid(taxCode) -> "Codice fiscale non valido (16 caratteri, es. RSSMRA80A01H501U)"
        !isTaxCodeChecksumValid(taxCode) -> "Codice fiscale non valido: il carattere di controllo non corrisponde"
        else -> null
    }
    val documentTypeError = if (submitAttempted && documentType.isBlank()) "Il tipo di documento è obbligatorio" else null
    val documentNumberError = when {
        documentNumber.isBlank() -> if (submitAttempted) "Il numero di documento è obbligatorio" else null
        !isDocumentNumberLengthValid(documentNumber) -> "Il numero di documento deve avere tra 5 e 20 caratteri"
        else -> null
    }
    val expirationDateError = when {
        expirationDate.isBlank() -> if (submitAttempted) "Seleziona una data di scadenza" else null
        !expirationDateValid -> "Documento scaduto! Impossibile effettuare la prenotazione con questo documento."
        else -> null
    }
    val issuingCountryError = when {
        issuingCountry.isBlank() -> if (submitAttempted) "Il paese di rilascio è obbligatorio" else null
        issuingCountry.length != 3 -> "Deve essere un codice ISO a 3 lettere (es. ITA)"
        else -> null
    }

    val formValid = firstName.isNotBlank() && lastName.isNotBlank() && phoneNumber.length == 10 && taxCode.isNotBlank() &&
        documentType.isNotBlank() && documentNumber.isNotBlank() && expirationDateValid && issuingCountry.length == 3

    fun fillFromSaved(document: TravelDocumentDto) {
        documentType = document.documentType
        documentNumber = document.documentNumber
        expirationDate = document.expirationDate
        issuingCountry = document.issuingCountry
    }

    val openDatePicker = {
        val today = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day -> expirationDate = "%04d-%02d-%02d".format(year, month + 1, day) },
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo passeggero") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = firstName, onValueChange = { firstName = it },
                    label = { Text("Nome") },
                    isError = firstNameError != null,
                    supportingText = firstNameError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lastName, onValueChange = { lastName = it },
                    label = { Text("Cognome") },
                    isError = lastNameError != null,
                    supportingText = lastNameError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber, onValueChange = { phoneNumber = it.filter { c -> c.isDigit() }.take(10) },
                    label = { Text("Telefono") },
                    isError = phoneNumberError != null,
                    supportingText = phoneNumberError?.let { { Text(it) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = taxCode,
                    onValueChange = { value -> taxCode = value.filter { it.isLetterOrDigit() }.uppercase().take(16) },
                    label = { Text("Codice fiscale") },
                    isError = taxCodeError != null,
                    supportingText = taxCodeError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                if (savedDocuments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Usa un documento salvato", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedDocuments.forEach { document ->
                            AssistChip(
                                onClick = { fillFromSaved(document) },
                                label = { Text("${document.documentType} · ${document.documentNumber}") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = documentType, onValueChange = { documentType = it },
                    label = { Text("Tipo documento") }, placeholder = { Text("Es. CARTA_IDENTITA") },
                    isError = documentTypeError != null,
                    supportingText = documentTypeError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = documentNumber,
                    onValueChange = { value -> documentNumber = value.filter { it.isLetterOrDigit() }.uppercase().take(20) },
                    label = { Text("Numero documento") },
                    isError = documentNumberError != null,
                    supportingText = documentNumberError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = expirationDate, onValueChange = {},
                    label = { Text("Scadenza documento") }, placeholder = { Text("Seleziona una data") },
                    isError = expirationDateError != null,
                    supportingText = expirationDateError?.let { { Text(it) } },
                    readOnly = true, singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = openDatePicker) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Scegli data")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = issuingCountry, onValueChange = { issuingCountry = it.uppercase().take(3) },
                    label = { Text("Paese emissione") }, placeholder = { Text("Es. ITA") },
                    isError = issuingCountryError != null,
                    supportingText = issuingCountryError?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!formValid) {
                        submitAttempted = true
                    } else {
                        onConfirm(
                            PassengerRequestDTO(
                                firstName = firstName,
                                lastName = lastName,
                                phoneNumber = phoneNumber,
                                taxCode = taxCode,
                                documentType = documentType,
                                documentNumber = documentNumber,
                                documentExpirationDate = expirationDate,
                                issuingCountry = issuingCountry
                            )
                        )
                    }
                }
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
