package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexFilterBottomSheet(
    currentCategory: String, // "Tutti", "Voli", "Hotel", "Escursioni"
    onDismiss: () -> Unit,
    // Qui in futuro passeremo l'oggetto "FilterState" completo, per ora teniamo stati locali per la UI
    onApplyFilters: (maxPrice: Float, minRating: Int, amenities: List<String>, directOnly: Boolean, guideOnly: Boolean) -> Unit
) {
    // Stati temporanei per la UI della tendina
    var maxPrice by remember { mutableFloatStateOf(1000f) }
    var minRating by remember { mutableIntStateOf(0) }

    // Hotel
    val availableAmenities = listOf("Wi-Fi", "Piscina", "Spa", "Colazione", "Parcheggio")
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    // Voli
    var directFlightOnly by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf("Qualsiasi") }

    // Escursioni
    var guideIncludedOnly by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()) // Rende la tendina scrollabile se ci sono tanti filtri
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtri Avanzati", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
                TextButton(onClick = { /* TODO: Resetta tutti gli stati */ }) {
                    Text("Resetta", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- SEZIONE 1: FILTRI GENERALI (Prezzo) ---
            Text("Budget Massimo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (maxPrice >= 1000f) "Nessun limite" else "Fino a €${maxPrice.toInt()}", fontWeight = FontWeight.Bold, color = TripifyGreen)
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 50f..1000f,
                steps = 19, // Passi da 50€
                colors = SliderDefaults.colors(thumbColor = TripifyGreen, activeTrackColor = TripifyGreen)
            )
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

            // --- SEZIONE 2: HOTEL ---
            if (currentCategory == "Tutti" || currentCategory == "Hotel") {
                Text("Stelle Hotel", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 3, 4, 5).forEach { stars ->
                        FilterChip(
                            selected = minRating == stars,
                            onClick = { minRating = stars },
                            label = { Text(if (stars == 0) "Tutte" else "$stars+ ⭐") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TripifyGreen.copy(alpha = 0.2f), selectedLabelColor = TripifyDarkGreen)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Servizi Extra", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(8.dp))
                // FlowRow sarebbe ideale qui, ma usiamo righe scindibili per compatibilità
                availableAmenities.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { amenity ->
                            val isSelected = selectedAmenities.contains(amenity)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAmenities = if (isSelected) selectedAmenities - amenity else selectedAmenities + amenity
                                },
                                label = { Text(amenity) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TripifyGreen.copy(alpha = 0.2f), selectedLabelColor = TripifyDarkGreen)
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))
            }

            // --- SEZIONE 3: VOLI ---
            if (currentCategory == "Tutti" || currentCategory == "Voli") {
                Text("Preferenze Volo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Solo voli diretti (Senza scali)", fontSize = 16.sp)
                    Switch(
                        checked = directFlightOnly,
                        onCheckedChange = { directFlightOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripifyGreen)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Orario di partenza", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Qualsiasi", "Mattina", "Sera").forEach { time ->
                        FilterChip(
                            selected = selectedTime == time,
                            onClick = { selectedTime = time },
                            label = { Text(time) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TripifyGreen.copy(alpha = 0.2f), selectedLabelColor = TripifyDarkGreen)
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))
            }

            // --- SEZIONE 4: ESCURSIONI ---
            if (currentCategory == "Tutti" || currentCategory == "Escursioni") {
                Text("Dettagli Esperienza", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Richiede guida turistica inclusa", fontSize = 16.sp)
                    Switch(
                        checked = guideIncludedOnly,
                        onCheckedChange = { guideIncludedOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripifyGreen)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // BOTTONE APPLICA
            Button(
                onClick = {
                    onApplyFilters(maxPrice, minRating, selectedAmenities.toList(), directFlightOnly, guideIncludedOnly)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen)
            ) {
                Text("MOSTRA RISULTATI", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}