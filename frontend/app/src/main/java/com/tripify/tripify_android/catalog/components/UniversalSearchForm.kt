package com.tripify.tripify_android.catalog.ui.components

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

@Composable
fun UniversalSearchForm(
    searchQuery: String, // RICEVE IL TESTO DAL VIEWMODEL
    onQueryChange: (String) -> Unit, // COMUNICA AL VIEWMODEL COSA SCRIVE L'UTENTE
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange, // Aggiorna in tempo reale
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

            // Sostituisci il vecchio bottone CERCA con questo:
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* In futuro: apri data picker */ },
                    colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("CERCA", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }

                // IL NUOVO TASTO FILTRI
                FilledIconButton(
                    onClick = { onOpenFilters() }, // Aggiungi questo parametro alla funzione UniversalSearchForm!
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = TripifyDarkGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Filtri Avanzati", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


        }
    }
}