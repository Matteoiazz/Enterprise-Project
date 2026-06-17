package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <-- IMPORT AGGIUNTO
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.core.theme.TripifyGreen

@Composable
fun FlightCard(
    flight: CatalogItem.Flight,
    onClick: () -> Unit // <-- PARAMETRO AGGIUNTO
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clickable { onClick() } // <-- LA CARD ORA È CLICCABILE
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = flight.imageUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 300f)
                )
            )
            Surface(
                color = TripifyGreen,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text(
                    text = flight.price, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(
                    text = flight.title, color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Rotta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flight.departureAirport, color = Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.Flight, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(flight.arrivalAirport, color = Color.LightGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Info Extra: Data e Posti rimasti
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(flight.departureTime, color = Color.White, fontSize = 13.sp)

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(Icons.Filled.EventSeat, contentDescription = null, tint = if(flight.availableSeats < 5) Color.Red else Color.LightGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if(flight.availableSeats < 5) "Ultimi ${flight.availableSeats} posti!" else "${flight.availableSeats} posti", color = if(flight.availableSeats < 5) Color.Red else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}