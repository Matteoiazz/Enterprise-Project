package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tour
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
fun ExcursionResultCard(
    excursion: CatalogItem.Excursion,
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
                    model = excursion.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).clip(CatalogShapes.Field)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = CatalogColors.AccentSoft,
                        shape = CatalogShapes.Pill
                    ) {
                        Text(
                            excursion.activityType,
                            color = CatalogColors.AccentDark,
                            style = CatalogType.Caption.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = excursion.title,
                        style = CatalogType.TitleCompact,
                        color = CatalogColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(excursion.duration, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                        if (excursion.rating != null && excursion.rating > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            RatingStars(rating = excursion.rating, starSize = 11.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = excursion.price,
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
                    Icon(
                        Icons.Filled.Tour,
                        contentDescription = null,
                        tint = if (excursion.guideIncluded) CatalogColors.Accent else CatalogColors.InkMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (excursion.guideIncluded) "Guida inclusa" else "Esplorazione libera",
                        style = CatalogType.Caption.copy(fontWeight = if (excursion.guideIncluded) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (excursion.guideIncluded) CatalogColors.AccentDark else CatalogColors.InkMuted
                    )
                }

                excursion.maxParticipants?.let { max ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Max $max", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                    }
                }
            }
        }
    }
}
