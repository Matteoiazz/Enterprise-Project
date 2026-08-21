package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@Composable
fun FlightResultCard(
    flight: CatalogItem.Flight,
    onClick: () -> Unit
) {
    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = Modifier.fillMaxWidth().pressScale(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row {
                AsyncImage(
                    model = flight.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).clip(CatalogShapes.Field)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flight.title,
                        style = CatalogType.TitleCompact,
                        color = CatalogColors.Ink,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(flight.departureAirport, style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold), color = CatalogColors.InkMuted)
                        Icon(Icons.Filled.Flight, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(12.dp).padding(horizontal = 4.dp))
                        Text(flight.arrivalAirport, style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold), color = CatalogColors.InkMuted)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${flight.departureCity} → ${flight.arrivalCity}",
                        style = CatalogType.Caption,
                        color = CatalogColors.InkMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = flight.price,
                        style = CatalogType.Price,
                        color = CatalogColors.AccentDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(flight.departureTime, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                }

                Surface(
                    color = if (flight.isDirect) CatalogColors.AccentSoft else CatalogColors.SurfaceMuted,
                    shape = CatalogShapes.Pill
                ) {
                    Text(
                        text = if (flight.isDirect) "Diretto" else "${flight.stops} ${if (flight.stops == 1) "scalo" else "scali"}",
                        style = CatalogType.Caption.copy(fontWeight = FontWeight.Bold),
                        color = if (flight.isDirect) CatalogColors.AccentDark else CatalogColors.InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val lowSeats = flight.availableSeats < 5
                    Icon(
                        Icons.Filled.EventSeat,
                        contentDescription = null,
                        tint = if (lowSeats) CatalogColors.Alert else CatalogColors.InkMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lowSeats) "Ultimi ${flight.availableSeats} posti" else "${flight.availableSeats} posti",
                        style = CatalogType.Caption.copy(fontWeight = if (lowSeats) FontWeight.Bold else FontWeight.Normal),
                        color = if (lowSeats) CatalogColors.Alert else CatalogColors.InkMuted
                    )
                }
            }
        }
    }
}
