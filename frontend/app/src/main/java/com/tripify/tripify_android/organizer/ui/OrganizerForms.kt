package com.tripify.tripify_android.organizer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.model.CatalogItemDto
import com.tripify.tripify_android.data.model.CreateActivityRequest
import com.tripify.tripify_android.data.model.CreateFareClassRequest
import com.tripify.tripify_android.data.model.CreateFlightRequest
import com.tripify.tripify_android.data.model.CreateHotelRequest
import com.tripify.tripify_android.data.model.CreateRoomTypeRequest
import java.time.Instant
import java.time.ZoneOffset

private val FormFieldColors: TextFieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CatalogColors.Accent,
        unfocusedBorderColor = CatalogColors.Hairline,
        focusedContainerColor = CatalogColors.Surface,
        unfocusedContainerColor = CatalogColors.Surface
    )

private data class FareClassField(val name: String, val price: String, val seats: String)
private data class RoomTypeField(val name: String, val price: String, val totalRooms: String, val maxOccupancy: String)

/** "2026-09-05T10:00:00" -> ("2026-09-05", "10:00"); mancante -> ("", ""). */
private fun splitIso(iso: String?): Pair<String, String> {
    if (iso == null || iso.length < 16) return "" to ""
    return iso.substring(0, 10) to iso.substring(11, 16)
}

private fun combineIso(date: String, time: String): String {
    val safeTime = time.ifBlank { "00:00" }
    return "${date}T$safeTime:00"
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = CatalogType.Caption) },
        singleLine = true,
        shape = CatalogShapes.Field,
        colors = FormFieldColors,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOnlyField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, style = CatalogType.Caption) },
        trailingIcon = { IconButton(onClick = { showPicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null, tint = CatalogColors.AccentDark) } },
        shape = CatalogShapes.Field,
        colors = FormFieldColors,
        modifier = modifier.fillMaxWidth()
    )
    if (showPicker) {
        val initialMillis = value.takeIf { it.length == 10 }?.let {
            java.time.LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annulla") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun FormDialogShell(title: String, onDismiss: () -> Unit, submitEnabled: Boolean, onSubmit: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Card,
        title = { Text(title, style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = submitEnabled) { Text("Salva", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) } }
    )
}

@Composable
fun FlightFormDialog(existing: CatalogItemDto?, onDismiss: () -> Unit, onSubmit: (CreateFlightRequest) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var price by remember { mutableStateOf(existing?.price?.toString() ?: "") }
    var departureAirport by remember { mutableStateOf(existing?.departureAirport ?: "") }
    var arrivalAirport by remember { mutableStateOf(existing?.arrivalAirport ?: "") }
    var departureCity by remember { mutableStateOf(existing?.departureCity ?: "") }
    var arrivalCity by remember { mutableStateOf(existing?.arrivalCity ?: "") }
    val (initialDepDate, initialDepTime) = remember { splitIso(existing?.departureTime) }
    val (initialArrDate, initialArrTime) = remember { splitIso(existing?.arrivalTime) }
    var departureDate by remember { mutableStateOf(initialDepDate) }
    var departureTime by remember { mutableStateOf(initialDepTime) }
    var arrivalDate by remember { mutableStateOf(initialArrDate) }
    var arrivalTime by remember { mutableStateOf(initialArrTime) }
    var totalSeats by remember { mutableStateOf(existing?.totalSeats?.toString() ?: "") }
    var stops by remember { mutableStateOf(existing?.stops?.toString() ?: "0") }
    val fareClasses = remember {
        mutableStateListOf<FareClassField>().apply {
            existing?.fareClasses?.forEach { add(FareClassField(it.name, it.price.toString(), it.totalSeats.toString())) }
            if (isEmpty()) { add(FareClassField("Economy", "", "")); add(FareClassField("Business", "", "")) }
        }
    }

    val isValid = title.isNotBlank() && price.toDoubleOrNull() != null &&
            departureAirport.length == 3 && arrivalAirport.length == 3 &&
            departureCity.isNotBlank() && arrivalCity.isNotBlank() &&
            departureDate.isNotBlank() && arrivalDate.isNotBlank() &&
            totalSeats.toIntOrNull() != null &&
            fareClasses.all { it.name.isNotBlank() && it.price.toDoubleOrNull() != null && it.seats.toIntOrNull() != null }

    FormDialogShell(
        title = if (existing == null) "Nuovo volo" else "Modifica volo",
        onDismiss = onDismiss,
        submitEnabled = isValid,
        onSubmit = {
            onSubmit(
                CreateFlightRequest(
                    title = title, description = description.ifBlank { null }, price = price.toDouble(),
                    departureAirport = departureAirport.uppercase(), arrivalAirport = arrivalAirport.uppercase(),
                    departureCity = departureCity, arrivalCity = arrivalCity,
                    departureTime = combineIso(departureDate, departureTime),
                    arrivalTime = combineIso(arrivalDate, arrivalTime),
                    totalSeats = totalSeats.toInt(), stops = stops.toIntOrNull() ?: 0,
                    fareClasses = fareClasses.map { CreateFareClassRequest(it.name, it.price.toDouble(), it.seats.toIntOrNull() ?: 0) }
                )
            )
        }
    ) {
        LabeledField("Titolo", title, { title = it })
        LabeledField("Descrizione", description, { description = it })
        LabeledField("Prezzo base (€)", price, { price = it }, keyboardType = KeyboardType.Decimal)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("Aeroporto partenza (IATA)", departureAirport, { departureAirport = it.take(3) }, modifier = Modifier.weight(1f))
            LabeledField("Aeroporto arrivo (IATA)", arrivalAirport, { arrivalAirport = it.take(3) }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("Città partenza", departureCity, { departureCity = it }, modifier = Modifier.weight(1f))
            LabeledField("Città arrivo", arrivalCity, { arrivalCity = it }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateOnlyField("Data partenza", departureDate, { departureDate = it }, modifier = Modifier.weight(1f))
            LabeledField("Ora (HH:mm)", departureTime, { departureTime = it }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateOnlyField("Data arrivo", arrivalDate, { arrivalDate = it }, modifier = Modifier.weight(1f))
            LabeledField("Ora (HH:mm)", arrivalTime, { arrivalTime = it }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("Posti totali", totalSeats, { totalSeats = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
            LabeledField("Scali", stops, { stops = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }

        Text("Tariffe", style = CatalogType.LabelStrong, color = CatalogColors.Ink)
        fareClasses.forEachIndexed { index, fc ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LabeledField("Nome", fc.name, { fareClasses[index] = fc.copy(name = it) }, modifier = Modifier.weight(1f))
                LabeledField("Prezzo", fc.price, { fareClasses[index] = fc.copy(price = it) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                LabeledField("Posti", fc.seats, { fareClasses[index] = fc.copy(seats = it) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                IconButton(onClick = { if (fareClasses.size > 1) fareClasses.removeAt(index) }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Rimuovi tariffa", tint = CatalogColors.Alert)
                }
            }
        }
        TextButton(onClick = { fareClasses.add(FareClassField("", "", "")) }) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Aggiungi tariffa", color = CatalogColors.AccentDark)
        }
    }
}

@Composable
fun HotelFormDialog(existing: CatalogItemDto?, onDismiss: () -> Unit, onSubmit: (CreateHotelRequest) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var price by remember { mutableStateOf(existing?.price?.toString() ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var lat by remember { mutableStateOf(existing?.locationLat?.toString() ?: "") }
    var lng by remember { mutableStateOf(existing?.locationLng?.toString() ?: "") }
    var amenitiesText by remember { mutableStateOf(existing?.amenities?.joinToString(", ") ?: "") }
    val roomTypes = remember {
        mutableStateListOf<RoomTypeField>().apply {
            existing?.roomTypes?.forEach { add(RoomTypeField(it.name, it.price.toString(), it.totalRooms.toString(), it.maxOccupancy?.toString() ?: "")) }
            if (isEmpty()) add(RoomTypeField("", "", "", ""))
        }
    }

    val isValid = title.isNotBlank() && price.toDoubleOrNull() != null &&
            address.isNotBlank() && city.isNotBlank() &&
            lat.toDoubleOrNull() != null && lng.toDoubleOrNull() != null &&
            roomTypes.all { it.name.isNotBlank() && it.price.toDoubleOrNull() != null && it.totalRooms.toIntOrNull() != null }

    FormDialogShell(
        title = if (existing == null) "Nuovo hotel" else "Modifica hotel",
        onDismiss = onDismiss,
        submitEnabled = isValid,
        onSubmit = {
            onSubmit(
                CreateHotelRequest(
                    title = title, description = description.ifBlank { null }, price = price.toDouble(),
                    locationLat = lat.toDouble(), locationLng = lng.toDouble(), address = address, city = city,
                    amenities = amenitiesText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    roomTypes = roomTypes.map {
                        CreateRoomTypeRequest(it.name, null, it.price.toDouble(), it.totalRooms.toIntOrNull() ?: 1, it.maxOccupancy.toIntOrNull())
                    }
                )
            )
        }
    ) {
        LabeledField("Titolo", title, { title = it })
        LabeledField("Descrizione", description, { description = it })
        LabeledField("Prezzo base (€)", price, { price = it }, keyboardType = KeyboardType.Decimal)
        LabeledField("Indirizzo", address, { address = it })
        LabeledField("Città", city, { city = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("Latitudine", lat, { lat = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            LabeledField("Longitudine", lng, { lng = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
        }
        LabeledField("Servizi (separati da virgola)", amenitiesText, { amenitiesText = it })

        Text("Camere", style = CatalogType.LabelStrong, color = CatalogColors.Ink)
        roomTypes.forEachIndexed { index, rt ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledField("Nome", rt.name, { roomTypes[index] = rt.copy(name = it) }, modifier = Modifier.weight(1f))
                    LabeledField("Prezzo/notte", rt.price, { roomTypes[index] = rt.copy(price = it) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    IconButton(onClick = { if (roomTypes.size > 1) roomTypes.removeAt(index) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Rimuovi camera", tint = CatalogColors.Alert)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledField("Camere totali", rt.totalRooms, { roomTypes[index] = rt.copy(totalRooms = it) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    LabeledField("Occupazione max", rt.maxOccupancy, { roomTypes[index] = rt.copy(maxOccupancy = it) }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
            }
        }
        TextButton(onClick = { roomTypes.add(RoomTypeField("", "", "", "")) }) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Aggiungi camera", color = CatalogColors.AccentDark)
        }
    }
}

@Composable
fun ActivityFormDialog(existing: CatalogItemDto?, onDismiss: () -> Unit, onSubmit: (CreateActivityRequest) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var price by remember { mutableStateOf(existing?.price?.toString() ?: "") }
    var activityType by remember { mutableStateOf(existing?.activityType ?: "") }
    var duration by remember { mutableStateOf(existing?.duration ?: "") }
    var meetingPoint by remember { mutableStateOf(existing?.meetingPoint ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var maxParticipants by remember { mutableStateOf(existing?.maxParticipants?.toString() ?: "") }
    var guideIncluded by remember { mutableStateOf(existing?.guideIncluded ?: false) }

    val isValid = title.isNotBlank() && price.toDoubleOrNull() != null &&
            activityType.isNotBlank() && duration.isNotBlank() && city.isNotBlank()

    FormDialogShell(
        title = if (existing == null) "Nuova attività" else "Modifica attività",
        onDismiss = onDismiss,
        submitEnabled = isValid,
        onSubmit = {
            onSubmit(
                CreateActivityRequest(
                    title = title, description = description.ifBlank { null }, price = price.toDouble(),
                    activityType = activityType, duration = duration, meetingPoint = meetingPoint.ifBlank { null },
                    city = city, maxParticipants = maxParticipants.toIntOrNull(), guideIncluded = guideIncluded
                )
            )
        }
    ) {
        LabeledField("Titolo", title, { title = it })
        LabeledField("Descrizione", description, { description = it })
        LabeledField("Prezzo (€)", price, { price = it }, keyboardType = KeyboardType.Decimal)
        LabeledField("Tipo attività", activityType, { activityType = it })
        LabeledField("Durata", duration, { duration = it })
        LabeledField("Punto di ritrovo", meetingPoint, { meetingPoint = it })
        LabeledField("Città", city, { city = it })
        LabeledField("Partecipanti massimi", maxParticipants, { maxParticipants = it }, keyboardType = KeyboardType.Number)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = guideIncluded, onCheckedChange = { guideIncluded = it }, colors = CheckboxDefaults.colors(checkedColor = CatalogColors.AccentDark))
            Text("Guida inclusa", style = CatalogType.Body, color = CatalogColors.Ink)
        }
    }
}
