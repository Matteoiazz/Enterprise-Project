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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF7A7A73)
private val Hairline = Color(0xFFE6E2D8)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        color = InkMuted
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplexFilterBottomSheet(
    currentCategory: String,
    onDismiss: () -> Unit,
    onApplyFilters: (price: Float, rating: Int, amenities: List<String>, direct: Boolean, guide: Boolean, destination: String, departure: String) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var maxPrice by remember { mutableFloatStateOf(1000f) }

    var departure by remember { mutableStateOf("") }
    var directFlightOnly by remember { mutableStateOf(false) }

    var minRating by remember { mutableIntStateOf(0) }

    val availableAmenities = listOf(
        "Wi-Fi", "Palestra", "Room Service", "Aria Condizionata",
        "Area Studio", "Parcheggio", "Spa", "Piscina", "Bar", "Fibra Dedicata"
    )
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }

    var guideIncludedOnly by remember { mutableStateOf(false) }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = TripifyDarkGreen,
        selectedLabelColor = Color.White,
        containerColor = Color.White,
        labelColor = Ink
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cerca $currentCategory", fontSize = 19.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Ink)
                TextButton(
                    onClick = {
                        destination = ""
                        maxPrice = 1000f
                        departure = ""
                        directFlightOnly = false
                        minRating = 0
                        selectedAmenities = setOf()
                        guideIncludedOnly = false
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Resetta", color = InkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(if (currentCategory == "Voli") "Dove vuoi volare?" else "Dove vuoi andare?", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, unfocusedBorderColor = Hairline),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            if (currentCategory == "Voli") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = departure,
                    onValueChange = { departure = it },
                    label = { Text("Da dove parti?", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TripifyGreen, unfocusedBorderColor = Hairline),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp), color = Hairline)

            SectionLabel("Budget massimo")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (maxPrice >= 1000f) "Nessun limite" else "Fino a €${maxPrice.toInt()}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TripifyDarkGreen
            )
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 50f..1000f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = TripifyDarkGreen, activeTrackColor = TripifyDarkGreen, inactiveTrackColor = Hairline)
            )

            if (currentCategory == "Tutti" || currentCategory == "Hotel") {
                Divider(modifier = Modifier.padding(vertical = 20.dp), color = Hairline)
                SectionLabel("Caratteristiche hotel")
                Spacer(modifier = Modifier.height(12.dp))

                Text("Categoria minima", fontSize = 13.sp, color = InkMuted, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf(0, 3, 4, 5).forEach { stars ->
                        FilterChip(
                            selected = minRating == stars,
                            onClick = { minRating = stars },
                            label = { Text(if (stars == 0) "Tutte" else "$stars+ ★", fontSize = 12.sp) },
                            colors = chipColors,
                            shape = RoundedCornerShape(8.dp),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = minRating == stars, borderColor = Hairline, selectedBorderColor = TripifyDarkGreen)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text("Servizi desiderati", fontSize = 13.sp, color = InkMuted, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                availableAmenities.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        rowItems.forEach { amenity ->
                            val isSelected = selectedAmenities.contains(amenity)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAmenities = if (isSelected) selectedAmenities - amenity else selectedAmenities + amenity },
                                label = { Text(amenity, fontSize = 12.sp) },
                                colors = chipColors,
                                shape = RoundedCornerShape(8.dp),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = Hairline, selectedBorderColor = TripifyDarkGreen)
                            )
                        }
                    }
                }
            }

            if (currentCategory == "Tutti" || currentCategory == "Voli") {
                Divider(modifier = Modifier.padding(vertical = 20.dp), color = Hairline)
                SectionLabel("Opzioni volo")
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostra solo voli diretti", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Switch(
                        checked = directFlightOnly,
                        onCheckedChange = { directFlightOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripifyDarkGreen)
                    )
                }
            }

            if (currentCategory == "Tutti" || currentCategory == "Escursioni") {
                Divider(modifier = Modifier.padding(vertical = 20.dp), color = Hairline)
                SectionLabel("Dettagli esperienza")
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Richiede guida inclusa", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
                    Switch(
                        checked = guideIncludedOnly,
                        onCheckedChange = { guideIncludedOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripifyDarkGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onApplyFilters(maxPrice, minRating, selectedAmenities.toList(), directFlightOnly, guideIncludedOnly, destination, departure)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen)
            ) {
                Text("MOSTRA RISULTATI", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp)
            }
        }
    }
}