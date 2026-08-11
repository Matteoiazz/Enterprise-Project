package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
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

@Composable
fun FlightCard(
    flight: CatalogItem.Flight,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = flight.imageUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TripifyDarkGreen.copy(alpha = 0.88f)),
                        startY = 140f
                    )
                )
            )

            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text(
                    text = flight.price,
                    color = TripifyDarkGreen,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    text = flight.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Rotta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flight.departureAirport, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Flight, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(flight.arrivalAirport, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(flight.departureTime, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)

                    Spacer(modifier = Modifier.width(14.dp))

                    val lowSeats = flight.availableSeats < 5
                    Icon(
                        Icons.Filled.EventSeat,
                        contentDescription = null,
                        tint = if (lowSeats) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lowSeats) "Ultimi ${flight.availableSeats} posti" else "${flight.availableSeats} posti",
                        color = if (lowSeats) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = if (lowSeats) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}