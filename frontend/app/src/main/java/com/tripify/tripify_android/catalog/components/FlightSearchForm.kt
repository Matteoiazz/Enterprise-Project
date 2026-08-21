package com.tripify.tripify_android.catalog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

const val NO_PRICE_LIMIT = 1000f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightSearchForm(
    departure: String,
    onDepartureChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    dateMillis: Long?,
    onDateChange: (Long?) -> Unit,
    passengers: Int,
    onPassengersChange: (Int) -> Unit,
    onSearch: () -> Unit,
    fetchSuggestions: suspend (String) -> List<String>,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showPassengersMenu by remember { mutableStateOf(false) }

    Card(
        shape = CatalogShapes.Card,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            CityAutocompleteField(
                value = departure,
                onValueChange = onDepartureChange,
                label = "Partenza",
                icon = Icons.Filled.FlightTakeoff,
                fetchSuggestions = fetchSuggestions
            )
            Spacer(modifier = Modifier.height(10.dp))
            CityAutocompleteField(
                value = destination,
                onValueChange = onDestinationChange,
                label = "Destinazione",
                icon = Icons.Filled.FlightLand,
                fetchSuggestions = fetchSuggestions
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val dateLabel = dateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN))
                } ?: "Data"

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    border = BorderStroke(1.dp, if (dateMillis != null) CatalogColors.AccentDark else CatalogColors.Hairline),
                    shape = CatalogShapes.Field,
                    contentPadding = PaddingValues(vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth, contentDescription = null,
                        tint = if (dateMillis != null) CatalogColors.AccentDark else CatalogColors.InkSubtle,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        dateLabel, style = CatalogType.Caption.copy(fontWeight = FontWeight.Medium),
                        color = if (dateMillis != null) CatalogColors.AccentDark else CatalogColors.InkSubtle
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showPassengersMenu = true },
                        border = BorderStroke(1.dp, if (passengers > 1) CatalogColors.AccentDark else CatalogColors.Hairline),
                        shape = CatalogShapes.Field,
                        contentPadding = PaddingValues(vertical = 0.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person, contentDescription = null,
                            tint = if (passengers > 1) CatalogColors.AccentDark else CatalogColors.InkSubtle,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (passengers == 1) "1 Pax" else "$passengers Pax",
                            style = CatalogType.Caption.copy(fontWeight = FontWeight.Medium),
                            color = if (passengers > 1) CatalogColors.AccentDark else CatalogColors.InkSubtle
                        )
                    }

                    DropdownMenu(expanded = showPassengersMenu, onDismissRequest = { showPassengersMenu = false }) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            IconButton(
                                onClick = { if (passengers > 1) onPassengersChange(passengers - 1) },
                                enabled = passengers > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Meno passeggeri", tint = if (passengers > 1) CatalogColors.AccentDark else CatalogColors.Hairline)
                            }
                            Text("$passengers", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                            IconButton(
                                onClick = { if (passengers < 9) onPassengersChange(passengers + 1) },
                                enabled = passengers < 9,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Più passeggeri", tint = if (passengers < 9) CatalogColors.AccentDark else CatalogColors.Hairline)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                shape = CatalogShapes.Field,
                modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(onClick = onSearch)
            ) {
                Text("CERCA VOLI", style = CatalogType.Button, color = Color.White)
            }
        }
    }

    if (showDatePicker) {
        val todayMillis = remember { LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis ?: todayMillis,
            selectableDates = remember {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = catalogDatePickerColors(),
            confirmButton = {
                TextButton(onClick = {
                    onDateChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                    Text("Conferma", color = CatalogColors.AccentDark)
                }
            },
            dismissButton = {
                if (dateMillis != null) {
                    TextButton(onClick = {
                        onDateChange(null)
                        showDatePicker = false
                    }) {
                        Text("Rimuovi data", color = CatalogColors.InkMuted)
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState, colors = catalogDatePickerColors())
        }
    }
}
