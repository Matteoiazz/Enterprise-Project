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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@Composable
fun FlightResultCard(
    flight: CatalogItem.Flight,
    onClick: () -> Unit
) {
    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
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
                    // Nota: 15sp non ha un token CatalogType equivalente (CardTitle è 19sp, pensato
                    // per le PhotoCard vetrina). Dimensione mantenuta custom per questo formato compatto.
                    Text(
                        text = flight.title,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = CatalogColors.Ink,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(flight.departureAirport, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CatalogColors.InkMuted)
                        Icon(Icons.Filled.Flight, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(12.dp).padding(horizontal = 4.dp))
                        Text(flight.arrivalAirport, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CatalogColors.InkMuted)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${flight.departureCity} → ${flight.arrivalCity}",
                        fontSize = 11.sp,
                        color = CatalogColors.InkMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = flight.price,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = TripifyDarkGreen
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
                    Text(flight.departureTime, fontSize = 11.sp, color = CatalogColors.InkMuted)
                }

                Surface(
                    color = if (flight.isDirect) TripifyGreen.copy(alpha = 0.1f) else CatalogColors.Hairline,
                    shape = CatalogShapes.Chip
                ) {
                    Text(
                        text = if (flight.isDirect) "Diretto" else "${flight.stops} ${if (flight.stops == 1) "scalo" else "scali"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (flight.isDirect) TripifyDarkGreen else CatalogColors.InkMuted,
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
                        fontSize = 11.sp,
                        color = if (lowSeats) CatalogColors.Alert else CatalogColors.InkMuted,
                        fontWeight = if (lowSeats) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}