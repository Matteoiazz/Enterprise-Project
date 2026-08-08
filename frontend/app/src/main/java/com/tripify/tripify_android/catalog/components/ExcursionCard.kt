package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ExcursionCard(
    excursion: CatalogItem.Excursion,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = excursion.imageUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TripifyDarkGreen.copy(alpha = 0.88f)),
                        startY = 120f
                    )
                )
            )

            Surface(
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text(
                    text = excursion.price,
                    color = TripifyDarkGreen,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    text = excursion.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(excursion.duration, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                    if (excursion.guideIncluded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Tour, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guida inclusa", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}