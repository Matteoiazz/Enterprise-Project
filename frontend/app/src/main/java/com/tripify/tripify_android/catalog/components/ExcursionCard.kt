package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tour
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem

@Composable
fun ExcursionCard(
    excursion: CatalogItem.Excursion,
    onClick: () -> Unit
) {
    PhotoCard(
        imageUrl = excursion.imageUrl,
        eyebrow = excursion.activityType,
        price = excursion.price,
        title = excursion.title,
        onClick = onClick
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PhotoMeta(icon = Icons.Filled.Schedule, text = excursion.duration)

            if (excursion.guideIncluded) {
                PhotoMeta(icon = Icons.Filled.Tour, text = "Guida inclusa")
            } else {
                excursion.maxParticipants?.let { max ->
                    PhotoMeta(icon = Icons.Filled.Groups, text = "Max $max")
                }
            }
        }
    }
}