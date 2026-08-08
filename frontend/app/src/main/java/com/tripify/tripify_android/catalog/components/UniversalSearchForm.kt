package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

private val Hairline = Color(0xFFE6E2D8)

@Composable
fun UniversalSearchForm(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Napoli, Tokyo, Maldive…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cerca", tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TripifyGreen,
                    unfocusedBorderColor = Hairline,
                    focusedContainerColor = SfondoPremium,
                    unfocusedContainerColor = SfondoPremium
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { /* In futuro: apri data picker */ },
                    colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("CERCA", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }

                OutlinedIconButton(
                    onClick = { onOpenFilters() },
                    border = BorderStroke(1.dp, Hairline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filtri avanzati", tint = TripifyDarkGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}