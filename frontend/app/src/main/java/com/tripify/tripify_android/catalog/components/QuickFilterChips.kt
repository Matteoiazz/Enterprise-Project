package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@Composable
fun QuickFilterChips(
    maxPrice: Float,
    minRating: Int,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priceLabel = if (maxPrice >= NO_PRICE_LIMIT) "Budget" else "Fino a €${maxPrice.toInt()}"
    val ratingLabel = if (minRating == 0) "Rating" else "$minRating★+"
    val priceActive = maxPrice < NO_PRICE_LIMIT
    val ratingActive = minRating > 0

    Row(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = priceActive,
            onClick = onOpenFilters,
            label = { Text(priceLabel, style = CatalogType.Caption) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                containerColor = CatalogColors.Surface, labelColor = CatalogColors.Ink
            ),
            shape = CatalogShapes.Chip,
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = priceActive, borderColor = CatalogColors.Hairline, selectedBorderColor = CatalogColors.AccentDark)
        )
        FilterChip(
            selected = ratingActive,
            onClick = onOpenFilters,
            label = { Text(ratingLabel, style = CatalogType.Caption) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                containerColor = CatalogColors.Surface, labelColor = CatalogColors.Ink
            ),
            shape = CatalogShapes.Chip,
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = ratingActive, borderColor = CatalogColors.Hairline, selectedBorderColor = CatalogColors.AccentDark)
        )
    }
}
