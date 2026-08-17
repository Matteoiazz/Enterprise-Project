package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF7A7A73)
private val Hairline = Color(0xFFE6E2D8)

@Composable
fun FlightResultCard(
    flight: CatalogItem.Flight,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row {
                AsyncImage(
                    model = flight.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flight.title,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(flight.departureAirport, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                        Icon(Icons.Filled.Flight, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(12.dp).padding(horizontal = 4.dp))
                        Text(flight.arrivalAirport, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${flight.departureCity} → ${flight.arrivalCity}",
                        fontSize = 11.sp,
                        color = InkMuted
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
            HorizontalDivider(color = Hairline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = InkMuted, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(flight.departureTime, fontSize = 11.sp, color = InkMuted)
                }

                Surface(
                    color = if (flight.isDirect) TripifyGreen.copy(alpha = 0.1f) else Hairline,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (flight.isDirect) "Diretto" else "${flight.stops} ${if (flight.stops == 1) "scalo" else "scali"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (flight.isDirect) TripifyDarkGreen else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val lowSeats = flight.availableSeats < 5
                    Icon(
                        Icons.Filled.EventSeat,
                        contentDescription = null,
                        tint = if (lowSeats) Color(0xFFB3261E) else InkMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lowSeats) "${flight.availableSeats} posti" else "${flight.availableSeats} posti",
                        fontSize = 11.sp,
                        color = if (lowSeats) Color(0xFFB3261E) else InkMuted,
                        fontWeight = if (lowSeats) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}