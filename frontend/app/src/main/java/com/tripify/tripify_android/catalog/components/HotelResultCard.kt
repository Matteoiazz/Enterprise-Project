package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@Composable
fun HotelResultCard(
    hotel: CatalogItem.Hotel,
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
                    model = hotel.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).clip(CatalogShapes.Field)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hotel.title,
                        style = CatalogType.TitleCompact,
                        color = CatalogColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(hotel.city, style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hotel.rating > 0) {
                            RatingStars(rating = hotel.rating, starSize = 12.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(Icons.Filled.Bed, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        val roomTypeLabel = hotel.roomTypes.minByOrNull { it.price }?.name
                            ?: if (hotel.roomTypes.size > 1) "${hotel.roomTypes.size} tipologie" else "Camera Standard"
                        Text(roomTypeLabel, style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hotel.price,
                        style = CatalogType.Price,
                        color = CatalogColors.AccentDark
                    )
                }
            }

            if (hotel.amenities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    hotel.amenities.take(3).forEach { amenity ->
                        Surface(
                            color = CatalogColors.AccentSoft,
                            shape = CatalogShapes.Pill
                        ) {
                            Text(
                                amenity,
                                color = CatalogColors.AccentDark,
                                style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (hotel.amenities.size > 3) {
                        Surface(color = CatalogColors.SurfaceMuted, shape = CatalogShapes.Pill) {
                            Text(
                                "+${hotel.amenities.size - 3}",
                                color = CatalogColors.InkMuted,
                                style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
