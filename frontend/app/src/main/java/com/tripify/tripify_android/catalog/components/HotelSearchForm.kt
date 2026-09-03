package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelSearchForm(
    city: String,
    onCityChange: (String) -> Unit,
    checkInMillis: Long?,
    onCheckInChange: (Long?) -> Unit,
    checkOutMillis: Long?,
    onCheckOutChange: (Long?) -> Unit,
    onSearch: () -> Unit,
    fetchSuggestions: suspend (String) -> List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CityAutocompleteField(
                value = city,
                onValueChange = onCityChange,
                label = "Città",
                icon = Icons.Filled.LocationOn,
                fetchSuggestions = fetchSuggestions
            )
            Spacer(modifier = Modifier.height(10.dp))
            DateRangeRow(
                checkInMillis = checkInMillis,
                checkOutMillis = checkOutMillis,
                onCheckInChange = onCheckInChange,
                onCheckOutChange = onCheckOutChange
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                shape = CatalogShapes.Field,
                modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(onClick = onSearch)
            ) {
                Text("CERCA HOTEL", style = CatalogType.Button, color = Color.White)
            }
        }
    }
}
