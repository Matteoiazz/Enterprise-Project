package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.booking.model.BoardingPassState
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.model.BookingLineDTO
import com.tripify.tripify_android.data.model.BookingResponseDTO
import com.tripify.tripify_android.data.model.PassengerResponseDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val detailDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ITALIAN)

private fun formatDetailDate(raw: String): String =
    try {
        LocalDateTime.parse(raw).format(detailDateFormatter)
    } catch (e: Exception) {
        raw
    }

private fun detailStatusLabel(status: String): String = when (status) {
    "PENDING" -> "In attesa di pagamento"
    "CONFIRMED" -> "Confermata"
    "CANCELLED" -> "Annullata"
    else -> status
}

private fun detailStatusColor(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "CONFIRMED" -> CatalogColors.AccentDark
    "CANCELLED" -> CatalogColors.Alert
    else -> CatalogColors.Gold
}

private fun detailStatusBackground(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "CONFIRMED" -> CatalogColors.AccentSoft
    "CANCELLED" -> CatalogColors.AlertSoft
    else -> CatalogColors.GoldSoft
}

// Riepilogo completo di una singola prenotazione (raggiungibile solo dalle
// prenotazioni CONFERMATE, vedi BookingCard): articoli con foto/nome/prezzo,
// ospiti registrati su ciascuno (nome + documento, senza dati sensibili come
// codice fiscale o numero di telefono), totale e stato. Solo lettura, nessuna
// azione: quelle restano sulla card nella lista prenotazioni.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
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

    val booking = (bookingState as? BookingState.Success)?.bookings?.find { it.id == bookingId }
    val guestsByLineId = (boardingPassState as? BoardingPassState.Success)?.lines
        ?.associate { (line, passengers) -> line.id to passengers }
        .orEmpty()

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Riepilogo prenotazione", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
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
                    BookingDetailHeader(booking = booking)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Articoli",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                booking.lines.forEach { line ->
                    item(key = "line-${line.id}") {
                        BookingDetailLineCard(
                            line = line,
                            guests = guestsByLineId[line.id].orEmpty(),
                            catalogViewModel = catalogViewModel
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Totale", style = CatalogType.Body, color = CatalogColors.InkMuted)
                        Text(
                            text = "€${"%.2f".format(booking.totalAmount)}",
                            style = CatalogType.PriceLarge,
                            color = CatalogColors.AccentDark
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BookingDetailHeader(booking: BookingResponseDTO) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(formatDetailDate(booking.bookingDate), style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                Spacer(modifier = Modifier.height(4.dp))
                val travelerCount = booking.participantIds.size + 1
                Text(
                    text = if (travelerCount == 1) "1 viaggiatore" else "$travelerCount viaggiatori",
                    style = CatalogType.Caption,
                    color = CatalogColors.InkMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (booking.isLeader) "Sei tu il proprietario di questa prenotazione" else "Sei un partecipante a questo viaggio",
                    style = CatalogType.Caption,
                    color = CatalogColors.InkMuted
                )
            }
            Surface(color = detailStatusBackground(booking.status), shape = CatalogShapes.Pill) {
                Text(
                    text = detailStatusLabel(booking.status),
                    style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = detailStatusColor(booking.status),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BookingDetailLineCard(line: BookingLineDTO, guests: List<PassengerResponseDTO>, catalogViewModel: CatalogViewModel) {
    var resolved by remember(line.catalogItemId) { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(line.catalogItemId) {
        resolved = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())
    }

    Surface(
        shape = CatalogShapes.Card,
        color = CatalogColors.Surface,
        border = BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = resolved?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resolved?.title ?: "Articolo #${line.catalogItemId}",
                        style = CatalogType.BodyStrong,
                        color = CatalogColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Quantità: ${line.quantity ?: 1}",
                        style = CatalogType.Caption,
                        color = CatalogColors.InkMuted
                    )
                }
                Text(text = "€${"%.2f".format(line.price)}", style = CatalogType.Price, color = CatalogColors.AccentDark)
            }

            if (guests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Ospiti", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                Spacer(modifier = Modifier.height(6.dp))
                guests.forEach { guest ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${guest.firstName} ${guest.lastName}",
                            style = CatalogType.Body,
                            color = CatalogColors.Ink
                        )
                        Text(
                            text = "${guest.documentType} ${guest.documentNumber} · CF ${guest.taxCode}",
                            style = CatalogType.Caption,
                            color = CatalogColors.InkMuted
                        )
                        if (!guest.phoneNumber.isNullOrBlank()) {
                            Text(
                                text = "Tel. ${guest.phoneNumber}",
                                style = CatalogType.Caption,
                                color = CatalogColors.InkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
