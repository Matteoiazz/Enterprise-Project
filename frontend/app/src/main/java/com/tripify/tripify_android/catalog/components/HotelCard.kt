package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.formattedPrice
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency
import java.util.Locale

@Composable
fun HotelCard(
    hotel: CatalogItem.Hotel,
    onClick: () -> Unit
) {
    val currency by rememberCatalogCurrency()
    PhotoCard(
        imageUrl = hotel.imageUrl,
        eyebrow = "Hotel",
        price = hotel.formattedPrice(currency),
        title = hotel.title,
        onClick = onClick
    ) {
        PhotoMeta(icon = Icons.Filled.LocationOn, text = hotel.address)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hotel.rating > 0) {
                RatingStars(rating = hotel.rating, starSize = 12.dp, filledTint = CatalogColors.Gold, emptyTint = Color.White.copy(alpha = 0.3f))
                Text(text = String.format(Locale.ITALY, "%.1f", hotel.rating), style = CatalogType.Meta, color = Color.White)
                Text(text = "·", style = CatalogType.Meta, color = Color.White.copy(alpha = 0.55f))
            }
            Text(text = ratingLabel(hotel.rating), style = CatalogType.Meta, color = Color.White.copy(alpha = 0.92f))
        }
    }
}