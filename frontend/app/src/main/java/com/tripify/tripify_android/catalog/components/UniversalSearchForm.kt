package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyGreen

@Composable
fun UniversalSearchForm(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(24.dp), // Bordi ancora più smussati
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp), // Ombra fortissima per farlo "staccare" dallo sfondo
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Unico campo di ricerca grande e accogliente
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Es. Napoli, Tokyo, Maldive...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cerca", tint = TripifyGreen) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TripifyGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = SfondoPremium,
                    unfocusedContainerColor = SfondoPremium
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottoni per date e passeggeri
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = SfondoPremium, contentColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aggiungi date", fontSize = 13.sp)
                }
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = SfondoPremium, contentColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(0.8f)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2 Ospiti", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Call to Action enorme
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("CERCA", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
            }
        }
    }
}