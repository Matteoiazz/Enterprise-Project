package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.BookingLineDTO
import com.tripify.tripify_android.data.model.BookingResponseDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val bookingDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ITALIAN)

private fun formatBookingDate(raw: String): String =
    try {
        LocalDateTime.parse(raw).format(bookingDateFormatter)
    } catch (e: Exception) {
        raw
    }

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "In attesa di pagamento"
    "CONFIRMED" -> "Confermata"
    "CANCELLED" -> "Annullata"
    else -> status
}

private fun statusColor(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "CONFIRMED" -> CatalogColors.AccentDark
    "CANCELLED" -> CatalogColors.Alert
    else -> CatalogColors.Gold
}

private fun statusBackground(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "CONFIRMED" -> CatalogColors.AccentSoft
    "CANCELLED" -> CatalogColors.AlertSoft
    else -> CatalogColors.GoldSoft
}

@Composable
fun BookingCard(
    booking: BookingResponseDTO,
    catalogViewModel: CatalogViewModel,
    onInviteClick: (Long) -> Unit,
    onCancelClick: (Long) -> Unit = {},
    onAddPassengersClick: (Long) -> Unit = {},
    onShowBoardingPassClick: (Long) -> Unit = {},
    onCardClick: (Long) -> Unit = {}
) {
    // Il riepilogo completo apre solo dalle prenotazioni CONFERMATE: una
    // PENDING non ha ancora ospiti/QR definitivi, un'annullata non compare
    // proprio più in lista (vedi BookingViewModel). I bottoni interni
    // (Annulla/Invita/...) restano cliccabili normalmente: Compose non fa
    // risalire il loro click al clickable della Card che li contiene.
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).let {
        if (booking.status == "CONFIRMED") it.clickable { onCardClick(booking.id) } else it
    }

    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = cardModifier
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = formatBookingDate(booking.bookingDate), style = CatalogType.Caption, color = CatalogColors.InkMuted)
                }

                Surface(color = statusBackground(booking.status), shape = CatalogShapes.Pill) {
                    Text(
                        text = statusLabel(booking.status),
                        style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor(booking.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (booking.lines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                booking.lines.forEach { line ->
                    BookingLineSummaryRow(line = line, catalogViewModel = catalogViewModel)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val travelerCount = booking.participantIds.size + 1
                Text(
                    text = if (travelerCount == 1) "1 viaggiatore" else "$travelerCount viaggiatori",
                    style = CatalogType.Caption,
                    color = CatalogColors.InkMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Totale", style = CatalogType.Body, color = CatalogColors.InkMuted)
                Text(
                    text = "€${"%.2f".format(booking.totalAmount)}",
                    style = CatalogType.Price,
                    color = CatalogColors.AccentDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LOGICA LEADER vs PARTECIPANTE
            if (booking.isLeader) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (booking.status != "CANCELLED") {
                        OutlinedButton(
                            onClick = { onCancelClick(booking.id) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CatalogColors.Alert),
                            border = BorderStroke(1.dp, CatalogColors.Alert.copy(alpha = 0.4f)),
                            shape = CatalogShapes.Field
                        ) {
                            Text("Annulla", style = CatalogType.Button)
                        }
                    }
                    Button(
                        onClick = { onInviteClick(booking.id) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                        shape = CatalogShapes.Field
                    ) {
                        Text("Invita", style = CatalogType.Button)
                    }
                }

                if (booking.status != "CANCELLED" && booking.lines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onAddPassengersClick(booking.id) },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CatalogColors.AccentDark),
                        border = BorderStroke(1.dp, CatalogColors.AccentDark.copy(alpha = 0.4f)),
                        shape = CatalogShapes.Field
                    ) {
                        Text("Aggiungi passeggeri", style = CatalogType.Button)
                    }
                }
            } else {
                Text(
                    text = "Sei un partecipante a questo viaggio",
                    color = CatalogColors.InkMuted,
                    style = CatalogType.Body
                )
            }

            // Visibile a leader e partecipanti: il biglietto serve a chiunque
            // viaggi, non solo a chi ha pagato. Compare solo a prenotazione
            // confermata (prima non c'è ancora nulla da mostrare al check-in).
            if (booking.status == "CONFIRMED" && booking.lines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onShowBoardingPassClick(booking.id) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                    shape = CatalogShapes.Field
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Il mio biglietto", style = CatalogType.Button)
                }
            }
        }
    }
}

// Riga di una singola prenotazione con foto e nome dell'articolo, stesso
// aspetto di CartItemCard: qui però è di sola lettura, niente checkbox/elimina.
@Composable
private fun BookingLineSummaryRow(line: BookingLineDTO, catalogViewModel: CatalogViewModel) {
    var resolved by remember(line.catalogItemId) { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(line.catalogItemId) {
        resolved = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = resolved?.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
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
        Text(
            text = "€${"%.2f".format(line.price)}",
            style = CatalogType.Price,
            color = CatalogColors.AccentDark
        )
    }
}
