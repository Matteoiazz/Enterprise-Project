package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

private val AVAILABLE_AMENITIES = listOf(
    "Wi-Fi", "Palestra", "Room Service", "Aria Condizionata",
    "Area Studio", "Parcheggio", "Spa", "Piscina", "Bar", "Fibra Dedicata"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComplexFilterBottomSheet(
    currentCategory: String,
    initialMaxPrice: Float,
    initialMinRating: Int,
    initialDestination: String,
    initialDeparture: String,
    initialAmenities: List<String>,
    initialDirectOnly: Boolean,
    initialGuideOnly: Boolean,
    onDismiss: () -> Unit,
    onApplyFilters: (price: Float, rating: Int, amenities: List<String>, direct: Boolean, guide: Boolean, destination: String, departure: String) -> Unit
) {
    var destination by remember { mutableStateOf(initialDestination) }
    var departure by remember { mutableStateOf(initialDeparture) }
    var maxPrice by remember { mutableFloatStateOf(initialMaxPrice) }
    var minRating by remember { mutableIntStateOf(initialMinRating) }
    var selectedAmenities by remember { mutableStateOf(initialAmenities.toSet()) }
    var directFlightOnly by remember { mutableStateOf(initialDirectOnly) }
    var guideIncludedOnly by remember { mutableStateOf(initialGuideOnly) }

    val showHotelSection = currentCategory == "Tutti" || currentCategory == "Hotel"
    val showFlightSection = currentCategory == "Tutti" || currentCategory == "Voli"
    val showActivitySection = currentCategory == "Tutti" || currentCategory == "Attività"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Sheet,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CatalogColors.Hairline) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter).padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentCategory == "Tutti") "Filtra la ricerca" else "Cerca $currentCategory",
                    style = CatalogType.Section, color = CatalogColors.Ink
                )
                TextButton(
                    onClick = {
                        destination = ""
                        departure = ""
                        maxPrice = NO_PRICE_LIMIT
                        minRating = 0
                        selectedAmenities = emptySet()
                        directFlightOnly = false
                        guideIncludedOnly = false
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("Azzera", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted)
                }
            }

            HorizontalDivider(color = CatalogColors.Hairline)

            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(horizontal = CatalogSpacing.Gutter).padding(top = 20.dp, bottom = 20.dp)
            ) {
                CatalogTextField(value = destination, onValueChange = { destination = it }, placeholder = if (currentCategory == "Voli") "Dove vuoi volare?" else "Dove vuoi andare?", leadingIcon = Icons.Filled.LocationOn)

                if (currentCategory == "Voli") {
                    Spacer(modifier = Modifier.height(10.dp))
                    CatalogTextField(value = departure, onValueChange = { departure = it }, placeholder = "Da dove parti?", leadingIcon = Icons.Filled.FlightTakeoff)
                }

                FilterSection("Budget massimo") {
                    Text(text = if (maxPrice >= NO_PRICE_LIMIT) "Nessun limite" else "Fino a €${maxPrice.toInt()}", style = CatalogType.Section, color = CatalogColors.AccentDark)
                    Slider(
                        value = maxPrice, onValueChange = { maxPrice = it }, valueRange = 50f..NO_PRICE_LIMIT, steps = 18,
                        colors = SliderDefaults.colors(thumbColor = CatalogColors.AccentDark, activeTrackColor = CatalogColors.AccentDark, inactiveTrackColor = CatalogColors.Hairline)
                    )
                }

                if (showHotelSection) {
                    FilterSection("Categoria minima") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 3, 4, 5).forEach { stars ->
                                SheetChip(label = if (stars == 0) "Tutte" else "$stars+ ★", selected = minRating == stars, onClick = { minRating = stars })
                            }
                        }
                    }
                    FilterSection("Servizi desiderati") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AVAILABLE_AMENITIES.forEach { amenity ->
                                val isSelected = amenity in selectedAmenities
                                SheetChip(label = amenity, selected = isSelected, onClick = { selectedAmenities = if (isSelected) selectedAmenities - amenity else selectedAmenities + amenity })
                            }
                        }
                    }
                }

                if (showFlightSection) {
                    FilterSection("Opzioni volo") { ToggleRow(label = "Solo voli diretti", checked = directFlightOnly, onCheckedChange = { directFlightOnly = it }) }
                }

                if (showActivitySection) {
                    FilterSection("Dettagli esperienza") { ToggleRow(label = "Con guida inclusa", checked = guideIncludedOnly, onCheckedChange = { guideIncludedOnly = it }) }
                }
            }

            HorizontalDivider(color = CatalogColors.Hairline)
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 14.dp)) {
                Button(
                    onClick = {
                        onApplyFilters(maxPrice, minRating, selectedAmenities.toList(), directFlightOnly, guideIncludedOnly, destination, departure)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                    shape = CatalogShapes.Field, elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("MOSTRA RISULTATI", style = CatalogType.Button, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = CatalogColors.Hairline)
    Text(text = title.uppercase(), style = CatalogType.Overline, color = CatalogColors.InkSubtle)
    Spacer(modifier = Modifier.height(10.dp))
    content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(text = label, style = if (selected) CatalogType.LabelStrong else CatalogType.Label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White, containerColor = CatalogColors.Surface, labelColor = CatalogColors.InkMuted),
        shape = CatalogShapes.Chip, border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = CatalogColors.Hairline, selectedBorderColor = CatalogColors.AccentDark)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CatalogColors.AccentDark, checkedBorderColor = CatalogColors.AccentDark, uncheckedThumbColor = Color.White, uncheckedTrackColor = CatalogColors.Hairline, uncheckedBorderColor = CatalogColors.Hairline)
        )
    }
}
