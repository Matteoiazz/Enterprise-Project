package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.formattedPrice
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency

@Composable
fun RecommendationCard(
    item: CatalogItem,
    onClick: () -> Unit
) {
    val currency by rememberCatalogCurrency()
    PhotoCard(
        imageUrl = item.imageUrl,
        eyebrow = when (item) {
            is CatalogItem.Flight -> "Volo"
            is CatalogItem.Hotel -> "Hotel"
            is CatalogItem.Excursion -> item.activityType
        },
        price = item.formattedPrice(currency),
        title = item.title,
        onClick = onClick,
        modifier = Modifier.width(158.dp),
        height = 140.dp,
        titleStyle = CatalogType.TitleCompact,
        titleMaxLines = 1
    ) {
        when (item) {
            is CatalogItem.Hotel -> PhotoMeta(icon = Icons.Filled.Star, text = if (item.rating > 0) "${item.rating} · ${item.city}" else item.city)
            is CatalogItem.Flight -> PhotoMeta(icon = Icons.Filled.Flight, text = "${item.departureAirport} → ${item.arrivalAirport}")
            is CatalogItem.Excursion -> PhotoMeta(icon = Icons.Filled.Schedule, text = item.duration)
        }
    }
}