package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.delay

private val Ink = Color(0xFF1A1A1A)
private val Hairline = Color(0xFFE6E2D8)

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

    LaunchedEffect(value) {
        if (value.trim().length < 2) {
            suggestions = emptyList()
            expanded = false
        } else {
            delay(300)
            suggestions = fetchSuggestions(value)
            expanded = suggestions.isNotEmpty()
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, unfocusedBorderColor = Hairline),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        if (expanded && suggestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 4.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column {
                    suggestions.forEach { city ->
                        Text(
                            text = city,
                            fontSize = 13.sp,
                            color = Ink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(city)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}