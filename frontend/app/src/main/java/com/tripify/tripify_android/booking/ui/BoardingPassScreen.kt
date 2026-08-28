package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.tripify.tripify_android.booking.model.BoardingPassState
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.model.BookingLineDTO
import com.tripify.tripify_android.data.model.PassengerResponseDTO

// Il "biglietto" del viaggiatore: un QR per ogni passeggero registrato, aperto
// da CheckInService lato backend 24h prima del check-in (o subito dopo la
// conferma per voli/attività, che non hanno una data di check-in propria).
// Raggiungibile sia dal leader che dai partecipanti: chiunque abbia il
// telefono in mano deve poter mostrare il biglietto, non solo chi ha pagato.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardingPassScreen(
    viewModel: BookingViewModel,
    catalogViewModel: CatalogViewModel,
    bookingId: Long,
    onNavigateBack: () -> Unit = {}
) {
    val bookingState by viewModel.uiState.collectAsState()
    val boardingPassState by viewModel.boardingPassState.collectAsState()

    LaunchedEffect(bookingId, bookingState) {
        val booking = (bookingState as? BookingState.Success)?.bookings?.find { it.id == bookingId }
        if (booking != null) {
            viewModel.fetchBoardingPass(booking)
        } else if (bookingState !is BookingState.Loading) {
            // Schermata raggiunta senza passare dalla lista già in memoria
            // (es. dopo un cambio di configurazione): ricarica le prenotazioni,
            // il LaunchedEffect si rieseguirà appena bookingState cambia.
            viewModel.fetchUserBookings()
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Il mio biglietto", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = boardingPassState) {
                is BoardingPassState.Loading -> CircularProgressIndicator(
                    color = CatalogColors.AccentDark,
                    modifier = Modifier.align(Alignment.Center)
                )

                is BoardingPassState.Error -> Text(
                    text = state.message,
                    style = CatalogType.Body,
                    color = CatalogColors.Alert,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                is BoardingPassState.Success -> {
                    val linesWithPassengers = state.lines.filter { (_, passengers) -> passengers.isNotEmpty() }
                    if (linesWithPassengers.isEmpty()) {
                        Text(
                            text = "Nessun passeggero registrato su questa prenotazione.",
                            style = CatalogType.Body,
                            color = CatalogColors.InkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            linesWithPassengers.forEach { (line, passengers) ->
                                item(key = "line-${line.id}") {
                                    BoardingPassLineTitle(line = line, catalogViewModel = catalogViewModel)
                                }
                                items(passengers, key = { it.id }) { passenger ->
                                    PassengerTicketCard(passenger = passenger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardingPassLineTitle(line: BookingLineDTO, catalogViewModel: CatalogViewModel) {
    var resolvedTitle by remember(line.catalogItemId) { mutableStateOf<String?>(null) }
    LaunchedEffect(line.catalogItemId) {
        resolvedTitle = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())?.title
    }

    Text(
        text = resolvedTitle ?: "Articolo #${line.catalogItemId}",
        style = CatalogType.Section,
        color = CatalogColors.Ink,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun PassengerTicketCard(passenger: PassengerResponseDTO) {
    Surface(
        shape = CatalogShapes.Card,
        color = CatalogColors.Surface,
        border = BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${passenger.firstName} ${passenger.lastName}",
                style = CatalogType.CardTitle,
                color = CatalogColors.Ink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${passenger.documentType} · ${passenger.documentNumber}",
                style = CatalogType.Caption,
                color = CatalogColors.InkMuted
            )
            Spacer(modifier = Modifier.height(16.dp))

            val qrCodeData = passenger.qrCodeData
            when {
                passenger.checkedIn -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = CatalogColors.AccentDark,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Check-in effettuato", style = CatalogType.BodyStrong, color = CatalogColors.AccentDark)
                }

                qrCodeData != null -> {
                    val qrBitmap = rememberQrBitmap(qrCodeData)
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "QR di check-in",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        Text(
                            text = "Impossibile generare il QR.",
                            style = CatalogType.Body,
                            color = CatalogColors.Alert
                        )
                    }
                }

                else -> {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = CatalogColors.InkSubtle,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check-in non ancora aperto",
                        style = CatalogType.Body,
                        color = CatalogColors.InkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Codifica la stringa opaca del passeggero (un UUID generato lato server, mai
// il contenuto del documento) in un QR renderizzato localmente: niente
// immagini da scaricare, solo la stringa viaggia dal backend.
@Composable
private fun rememberQrBitmap(content: String, sizePx: Int = 512): ImageBitmap? =
    remember(content) {
        runCatching {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap.asImageBitmap()
        }.getOrNull()
    }
