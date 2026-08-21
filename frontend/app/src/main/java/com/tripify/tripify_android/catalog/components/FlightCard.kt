package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@Composable
fun FlightCard(
    flight: CatalogItem.Flight,
    onClick: () -> Unit
) {
    val lowSeats = flight.availableSeats in 1..4

    PhotoCard(
        imageUrl = flight.imageUrl,
        eyebrow = if (flight.isDirect) "Volo diretto" else "Volo",
        price = flight.price,
        title = flight.title,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = flight.departureAirport, style = CatalogType.Meta, color = Color.White)
            RouteDot()
            Icon(imageVector = Icons.Filled.Flight, contentDescription = null, tint = CatalogColors.AccentLight, modifier = Modifier.size(13.dp))
            RouteDot()
            Text(text = flight.arrivalAirport, style = CatalogType.Meta, color = Color.White)
        }

        Spacer(modifier = Modifier.height(5.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PhotoMeta(icon = Icons.Filled.CalendarMonth, text = flight.departureTime)

            if (flight.availableSeats > 0) {
                PhotoMeta(
                    icon = Icons.Filled.EventSeat,
                    text = if (lowSeats) "Ultimi ${flight.availableSeats} posti" else "${flight.availableSeats} posti",
                    tint = if (lowSeats) CatalogColors.AlertOnDark else Color.White.copy(alpha = 0.72f),
                    textColor = if (lowSeats) CatalogColors.AlertOnDark else Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
private fun RouteDot() {
    Spacer(modifier = Modifier.width(7.dp))
    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
    Spacer(modifier = Modifier.width(7.dp))
}