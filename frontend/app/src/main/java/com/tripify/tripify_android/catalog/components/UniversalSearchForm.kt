package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.BorderStroke
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

@Composable
fun UniversalSearchForm(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Napoli, Tokyo, Maldive…", style = CatalogType.Label) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cerca", tint = CatalogColors.Accent, modifier = Modifier.size(18.dp)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) ClearFieldButton(onClear = { onQueryChange("") }) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatalogColors.Accent,
                    unfocusedBorderColor = CatalogColors.Hairline,
                    focusedContainerColor = CatalogColors.Background,
                    unfocusedContainerColor = CatalogColors.Background
                ),
                textStyle = CatalogType.Label,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                singleLine = true,
                shape = CatalogShapes.Field
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                    shape = CatalogShapes.Field,
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(44.dp).pressScale(onClick = onSearch)
                ) {
                    Text("CERCA", style = CatalogType.Button, color = Color.White)
                }

                OutlinedIconButton(
                    onClick = onOpenFilters,
                    border = BorderStroke(1.dp, CatalogColors.Hairline),
                    shape = CatalogShapes.Field,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filtri avanzati", tint = CatalogColors.AccentDark, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
