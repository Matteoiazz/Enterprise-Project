package com.tripify.tripify_android.booking.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.BookingLineDTO
import com.tripify.tripify_android.data.model.PassengerRequestDTO
import com.tripify.tripify_android.data.model.TravelDocumentDto
import kotlinx.coroutines.launch
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
                item {
                    Text(
                        "Viaggio #${booking.id}",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
    var title by remember(line.catalogItemId) { mutableStateOf<String?>(null) }
    LaunchedEffect(line.catalogItemId) {
        title = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())?.title
    }

    val maxPassengers = line.quantity
    val isFull = maxPassengers != null && line.passengerCount >= maxPassengers

    Surface(
        shape = CatalogShapes.Card,
        color = CatalogColors.Surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title ?: "Articolo #${line.catalogItemId}", style = CatalogType.CardTitle, color = CatalogColors.Ink)
            Spacer(modifier = Modifier.height(4.dp))
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
    var taxCode by remember { mutableStateOf("") }
    var documentType by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var issuingCountry by remember { mutableStateOf("") }

    val formValid = firstName.isNotBlank() && lastName.isNotBlank() && taxCode.isNotBlank() &&
        documentType.isNotBlank() && documentNumber.isNotBlank() && expirationDate.isNotBlank() && issuingCountry.isNotBlank()

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
                    label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lastName, onValueChange = { lastName = it },
                    label = { Text("Cognome") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = taxCode, onValueChange = { taxCode = it.uppercase() },
                    label = { Text("Codice fiscale") }, singleLine = true, modifier = Modifier.fillMaxWidth()
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
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = documentNumber, onValueChange = { documentNumber = it },
                    label = { Text("Numero documento") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = expirationDate, onValueChange = {},
                    label = { Text("Scadenza documento") }, placeholder = { Text("Seleziona una data") },
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
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = formValid,
                onClick = {
                    onConfirm(
                        PassengerRequestDTO(
                            firstName = firstName,
                            lastName = lastName,
                            taxCode = taxCode,
                            documentType = documentType,
                            documentNumber = documentNumber,
                            documentExpirationDate = expirationDate,
                            issuingCountry = issuingCountry
                        )
                    )
                }
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
