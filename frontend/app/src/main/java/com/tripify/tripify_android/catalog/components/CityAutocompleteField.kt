package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import kotlinx.coroutines.delay

@Composable
fun CityAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    fetchSuggestions: suspend (String) -> List<String>,
    modifier: Modifier = Modifier
) {
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    // Selezionare un suggerimento chiama onValueChange(city), che cambia `value` e quindi
    // rilancerebbe comunque questo effect: senza questo flag, dopo i 300ms di debounce
    // "expanded" torna a true e il menu si riapre da solo sopra il campo appena compilato.
    var suppressNextFetch by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(value) {
        if (suppressNextFetch) {
            suppressNextFetch = false
            return@LaunchedEffect
        }
        if (value.trim().length < 2) {
            suggestions = emptyList()
            expanded = false
            isLoading = false
        } else {
            isLoading = true
            delay(300)
            suggestions = fetchSuggestions(value)
            isLoading = false
            expanded = true
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label, style = CatalogType.Label) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CatalogColors.Accent)
                    value.isNotEmpty() -> ClearFieldButton(onClear = {
                        suggestions = emptyList()
                        expanded = false
                        onValueChange("")
                    })
                }
            },
            textStyle = CatalogType.Label,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CatalogColors.Accent,
                unfocusedBorderColor = CatalogColors.Hairline,
                focusedContainerColor = CatalogColors.Surface,
                unfocusedContainerColor = CatalogColors.Surface
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            singleLine = true,
            shape = CatalogShapes.Field
        )

        if (expanded && suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = CatalogShapes.Field,
                shadowElevation = 8.dp,
                color = CatalogColors.Surface,
                border = BorderStroke(1.dp, CatalogColors.Hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    suggestions.forEachIndexed { index, city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    suppressNextFetch = true
                                    suggestions = emptyList()
                                    expanded = false
                                    onValueChange(city)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = CatalogColors.InkSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = city, style = CatalogType.Label, color = CatalogColors.Ink)
                        }
                        if (index != suggestions.lastIndex) {
                            HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
