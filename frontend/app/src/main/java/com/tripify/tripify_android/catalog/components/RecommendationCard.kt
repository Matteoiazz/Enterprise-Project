package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.core.theme.TripifyDarkGreen

@Composable
fun RecommendationCard(
    item: CatalogItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .width(140.dp)
            .height(105.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TripifyDarkGreen.copy(alpha = 0.88f)),
                        startY = 100f
                    )
                )
            )

            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            ) {
                Text(
                    text = item.price,
                    color = TripifyDarkGreen,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item) {
                        is CatalogItem.Hotel -> {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFE8C468), modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${item.rating} · ${item.city}", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        is CatalogItem.Flight -> {
                            Icon(Icons.Filled.Flight, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${item.departureAirport} → ${item.arrivalAirport}", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp)
                        }
                        is CatalogItem.Excursion -> {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(item.duration, color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}