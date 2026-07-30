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
    onApplyFilters: (price: Float, rating: Int, amenities: List<String>, direct: Boolean, guide: Boolean, destination: String, departure: String) -> Unit
) {
    // Stati Generali
    var destination by remember { mutableStateOf("") }
    var maxPrice by remember { mutableFloatStateOf(1000f) }

    // Stati Specifici
    var departure by remember { mutableStateOf("") } // Solo Voli
    var directFlightOnly by remember { mutableStateOf(false) } // Solo Voli

    var minRating by remember { mutableIntStateOf(0) } // Solo Hotel
    val availableAmenities = listOf("Wi-Fi", "Piscina", "Spa", "Colazione", "Parcheggio")
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) } // Solo Hotel

    var guideIncludedOnly by remember { mutableStateOf(false) } // Solo Escursioni

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // HEADER
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cerca $currentCategory", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
                TextButton(onClick = { /* TODO: Resetta tutto */ }) {
                    Text("Resetta", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- SEZIONE: DESTINAZIONE E PARTENZA ---
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(if (currentCategory == "Voli") "Dove vuoi volare?" else "Dove vuoi andare?") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TripifyGreen) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (currentCategory == "Voli") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = departure,
                    onValueChange = { departure = it },
                    label = { Text("Da dove parti?") },
                    leadingIcon = { Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = TripifyGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))

            // --- SEZIONE: BUDGET ---
            Text("Budget Massimo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (maxPrice >= 1000f) "Nessun limite" else "Fino a €${maxPrice.toInt()}", fontWeight = FontWeight.Bold, color = TripifyGreen)
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 50f..1000f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = TripifyGreen, activeTrackColor = TripifyGreen)
            )

            // --- SEZIONE DINAMICA: CARATTERISTICHE HOTEL ---
            if (currentCategory == "Tutti" || currentCategory == "Hotel") {
                Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))
                Text("Caratteristiche Hotel", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Categoria minima", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf(0, 3, 4, 5).forEach { stars ->
                        FilterChip(
                            selected = minRating == stars,
                            onClick = { minRating = stars },
                            label = { Text(if (stars == 0) "Tutte" else "$stars+ ⭐") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TripifyGreen.copy(alpha = 0.2f), selectedLabelColor = TripifyDarkGreen)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Servizi desiderati", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                availableAmenities.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { amenity ->
                            val isSelected = selectedAmenities.contains(amenity)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAmenities = if (isSelected) selectedAmenities - amenity else selectedAmenities + amenity },
                                label = { Text(amenity) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TripifyGreen.copy(alpha = 0.2f), selectedLabelColor = TripifyDarkGreen)
                            )
                        }
                    }
                }
            }

            // --- SEZIONE DINAMICA: OPZIONI VOLO ---
            if (currentCategory == "Tutti" || currentCategory == "Voli") {
                Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))
                Text("Opzioni Volo", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostra solo voli diretti", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = directFlightOnly,
                        onCheckedChange = { directFlightOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripifyGreen)
                    )
                }
            }

            // --- SEZIONE DINAMICA: ESCURSIONI ---
            if (currentCategory == "Tutti" || currentCategory == "Escursioni") {
                Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))
                Text("Dettagli Esperienza", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Richiede guida inclusa", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = guideIncludedOnly,
                        onCheckedChange = { guideIncludedOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripifyGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTTONE APPLICA
            Button(
                onClick = {
                    onApplyFilters(maxPrice, minRating, selectedAmenities.toList(), directFlightOnly, guideIncludedOnly, destination, departure)
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