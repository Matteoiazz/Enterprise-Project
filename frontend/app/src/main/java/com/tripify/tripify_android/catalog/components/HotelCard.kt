package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <-- IMPORT AGGIUNTO
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
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
fun HotelCard(
    hotel: CatalogItem.Hotel,
    onClick: () -> Unit // <-- PARAMETRO AGGIUNTO
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clickable { onClick() } // <-- LA CARD ORA È CLICCABILE
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = hotel.imageUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 300f)
                )
            )

            // Prezzo in alto a destra
            Surface(
                color = TripifyGreen,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text(
                    text = hotel.price, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Dettagli in basso
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(
                    text = hotel.title, color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Indirizzo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(hotel.address, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${hotel.rating} Eccellente", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}