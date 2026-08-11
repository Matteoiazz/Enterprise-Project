package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.TripifyDarkGreen

private val Ink = Color(0xFF1A1A1A)
private val Hairline = Color(0xFFE6E2D8)

@Composable
fun QuickFilterChips(
    maxPrice: Float,
    minRating: Int,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priceLabel = if (maxPrice >= 1000f) "Budget" else "Fino a €${maxPrice.toInt()}"
    val ratingLabel = if (minRating == 0) "Rating" else "$minRating★+"
    val priceActive = maxPrice < 1000f
    val ratingActive = minRating > 0

    Row(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = priceActive,
            onClick = onOpenFilters,
            label = { Text(priceLabel, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TripifyDarkGreen, selectedLabelColor = Color.White,
                containerColor = Color.White, labelColor = Ink
            ),
            shape = RoundedCornerShape(8.dp),
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = priceActive, borderColor = Hairline, selectedBorderColor = TripifyDarkGreen)
        )
        FilterChip(
            selected = ratingActive,
            onClick = onOpenFilters,
            label = { Text(ratingLabel, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TripifyDarkGreen, selectedLabelColor = Color.White,
                containerColor = Color.White, labelColor = Ink
            ),
            shape = RoundedCornerShape(8.dp),
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = ratingActive, borderColor = Hairline, selectedBorderColor = TripifyDarkGreen)
        )
    }
}