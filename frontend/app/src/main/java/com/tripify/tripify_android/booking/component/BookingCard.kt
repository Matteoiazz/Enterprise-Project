package com.tripify.tripify_android.booking.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.BookingResponseDTO

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
    onInviteClick: (Long) -> Unit,
    onCancelClick: (Long) -> Unit = {}
) {
    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Viaggio #${booking.id}", style = CatalogType.CardTitle, color = CatalogColors.Ink)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = booking.bookingDate, style = CatalogType.Caption, color = CatalogColors.InkMuted)
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
                Text(
                    text = "${booking.lines.size} elemento/i" +
                        if (booking.participantIds.isNotEmpty()) " · ${booking.participantIds.size + 1} viaggiatori" else "",
                    style = CatalogType.Body,
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
            } else {
                Text(
                    text = "Sei un partecipante a questo viaggio",
                    color = CatalogColors.InkMuted,
                    style = CatalogType.Body
                )
            }
        }
    }
}
