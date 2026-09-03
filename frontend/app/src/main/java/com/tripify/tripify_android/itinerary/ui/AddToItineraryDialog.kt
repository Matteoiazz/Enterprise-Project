package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Tour
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.AddListItemRequest
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val DialogTextFieldColors: TextFieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CatalogColors.Accent,
        unfocusedBorderColor = CatalogColors.Hairline,
        focusedContainerColor = CatalogColors.Surface,
        unfocusedContainerColor = CatalogColors.Surface,
        cursorColor = CatalogColors.AccentDark,
        focusedTextColor = CatalogColors.Ink,
        unfocusedTextColor = CatalogColors.Ink,
        focusedLabelColor = CatalogColors.Accent,
        unfocusedLabelColor = CatalogColors.InkMuted
    )

/**
 * Piccolo dialog per aggiungere un CatalogItem a un itinerario. Raccoglie i dati che
 * itinerary-service richiede per validare la coerenza geografica/temporale del
 * viaggio e calcolare il prezzo reale: tariffa per i voli, camera + date per gli
 * hotel, data per le attività (vedi ItineraryService.addItemToList lato backend,
 * che rifiuta con un messaggio chiaro se qualcosa non torna).
 *
 * Se [fixedListId] è valorizzato l'aggiunta va dritta a quella lista (usato dal
 * pulsante "Aggiungi componente" dentro il dettaglio di un proprio itinerario);
 * altrimenti mostra la scelta tra le proprie liste (usato dal bottone "Aggiungi a
 * un itinerario" nel dettaglio di un CatalogItem).
 */
@Composable
fun AddToItineraryDialog(
    catalogItem: CatalogItem,
    fixedListId: Long? = null,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val api = remember { com.tripify.tripify_android.itinerary.data.ItineraryRetrofit.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var lists by remember { mutableStateOf<List<FavoriteListDto>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var selectedFareClassId by remember { mutableStateOf((catalogItem as? CatalogItem.Flight)?.fareClasses?.minByOrNull { it.price }?.id) }
    var selectedRoomTypeId by remember { mutableStateOf((catalogItem as? CatalogItem.Hotel)?.roomTypes?.minByOrNull { it.price }?.id) }
    var checkIn by remember { mutableStateOf<LocalDate?>(null) }
    var checkOut by remember { mutableStateOf<LocalDate?>(null) }
    var activityDate by remember { mutableStateOf<LocalDate?>(null) }

    val missingDetails = when (catalogItem) {
        is CatalogItem.Flight -> selectedFareClassId == null
        is CatalogItem.Hotel -> selectedRoomTypeId == null || checkIn == null || checkOut == null || !checkOut!!.isAfter(checkIn)
        is CatalogItem.Excursion -> activityDate == null
    }

    fun submit(targetListId: Long) {
        isSubmitting = true
        scope.launch {
            try {
                val request = AddListItemRequest(
                    catalogItemId = catalogItem.id.toLong(),
                    fareClassId = selectedFareClassId?.toLong(),
                    roomTypeId = selectedRoomTypeId?.toLong(),
                    checkIn = checkIn?.toString(),
                    checkOut = checkOut?.toString(),
                    activityDate = activityDate?.toString()
                )
                val response = api.addItem(targetListId, request)
                if (response.isSuccessful) {
                    onAdded()
                } else {
                    errorMessage = response.errorBody()?.string()
                        ?.let { runCatching { org.json.JSONObject(it).optString("message") }.getOrNull() }
                        ?.takeIf { it.isNotBlank() }
                        ?: "Questo componente non è coerente con l'itinerario"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile aggiungere l'elemento"
            }
            isSubmitting = false
        }
    }

    LaunchedEffect(Unit) {
        if (fixedListId == null) {
            try {
                val response = api.getMyLists()
                lists = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            } catch (e: Exception) {
                errorMessage = "Impossibile caricare le tue liste"
                lists = emptyList()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Card,
        title = { Text(if (fixedListId != null) "Aggiungi componente" else "Aggiungi a un itinerario", style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            Column {
                when (catalogItem) {
                    is CatalogItem.Flight -> FareClassPicker(
                        fareClasses = catalogItem.fareClasses,
                        selectedId = selectedFareClassId,
                        onSelect = { selectedFareClassId = it }
                    )
                    is CatalogItem.Hotel -> {
                        RoomTypePicker(
                            roomTypes = catalogItem.roomTypes,
                            selectedId = selectedRoomTypeId,
                            onSelect = { selectedRoomTypeId = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateField(label = "Check-in", date = checkIn, modifier = Modifier.weight(1f)) { checkIn = it }
                            DateField(label = "Check-out", date = checkOut, modifier = Modifier.weight(1f)) { checkOut = it }
                        }
                    }
                    is CatalogItem.Excursion -> DateField(label = "Data dell'attività", date = activityDate) { activityDate = it }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, style = CatalogType.Caption, color = CatalogColors.Alert)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (fixedListId != null) {
                    Button(
                        onClick = { submit(fixedListId) },
                        enabled = !missingDetails && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark, disabledContainerColor = CatalogColors.SurfaceMuted),
                        shape = CatalogShapes.Field,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CatalogColors.Surface)
                        } else {
                            Text("Aggiungi all'itinerario", style = CatalogType.LabelStrong, color = CatalogColors.Surface)
                        }
                    }
                    if (missingDetails) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Completa i dati sopra per continuare.", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                    }
                } else {
                    when {
                        lists == null -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.size(28.dp))
                        }
                        lists!!.isEmpty() -> Text(
                            "Non hai ancora nessuna lista. Creane una dalla tab Itinerari.",
                            style = CatalogType.Body, color = CatalogColors.InkMuted
                        )
                        else -> Column {
                            lists!!.forEach { list ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !missingDetails) { submit(list.id) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Add, contentDescription = null,
                                        tint = if (missingDetails) CatalogColors.InkSubtle else CatalogColors.AccentDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        list.name, style = CatalogType.BodyStrong,
                                        color = if (missingDetails) CatalogColors.InkSubtle else CatalogColors.Ink
                                    )
                                }
                                HorizontalDivider(color = CatalogColors.Hairline)
                            }
                            if (missingDetails) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Completa i dati sopra per poter aggiungere questo elemento.",
                                    style = CatalogType.Caption, color = CatalogColors.InkMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
        }
    )
}

/**
 * Dialog di ricerca per scegliere QUALE componente del catalogo aggiungere a un
 * itinerario già aperto: usato dal pulsante "Aggiungi componente" nel dettaglio di
 * un proprio itinerario. Riusa la stessa CatalogViewModel già usata dalla home per
 * ricerca/filtri, così non serve una seconda pipeline di ricerca.
 *
 * CatalogViewModel è condivisa con tutta l'app (stessa istanza passata a ogni
 * schermata): senza reset, categoria/testo di una ricerca fatta altrove (es. nella
 * Home) resterebbero applicati qui, filtrando i risultati in modo non ovvio per
 * l'utente (es. "Attività" che mostra un solo elemento perché combinato con un
 * vecchio testo di ricerca). Il dialog riparte quindi da "Tutti" + ricerca vuota
 * all'apertura, e ripristina lo stato precedente alla chiusura.
 */
@Composable
fun CatalogItemPickerDialog(
    catalogViewModel: CatalogViewModel,
    onDismiss: () -> Unit,
    onSelect: (CatalogItem) -> Unit
) {
    DisposableEffect(Unit) {
        val savedCategory = catalogViewModel.selectedCategory.value
        val savedQuery = catalogViewModel.searchQuery.value
        catalogViewModel.updateSearchQuery("")
        catalogViewModel.setCategory("Tutti")
        onDispose {
            catalogViewModel.updateSearchQuery(savedQuery)
            catalogViewModel.setCategory(savedCategory)
        }
    }

    val catalogList by catalogViewModel.catalogList.collectAsState()
    val searchQuery by catalogViewModel.searchQuery.collectAsState()
    val selectedCategory by catalogViewModel.selectedCategory.collectAsState()
    val isLoading by catalogViewModel.isLoading.collectAsState()
    val isLoadingMore by catalogViewModel.isLoadingMore.collectAsState()
    val isLastPage by catalogViewModel.isLastPage.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Card,
        title = { Text("Aggiungi componente", style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            Column {
                Text(
                    "Aggiungi in ordine di viaggio: prima il volo di andata, poi hotel/attività nella città di arrivo, poi il volo successivo.",
                    style = CatalogType.Caption, color = CatalogColors.InkMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { catalogViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Cerca voli, hotel, attività…", style = CatalogType.Label, color = CatalogColors.InkSubtle) },
                    singleLine = true,
                    shape = CatalogShapes.Field,
                    colors = DialogTextFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Tutti", "Voli", "Hotel", "Attività").forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { catalogViewModel.setCategory(category) },
                            label = { Text(category, style = CatalogType.Caption) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = CatalogColors.Surface),
                            shape = CatalogShapes.Chip
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.size(28.dp))
                    }
                    catalogList.isEmpty() -> Text("Nessun risultato.", style = CatalogType.Body, color = CatalogColors.InkMuted)
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(catalogList, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CatalogShapes.Badge)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val (icon, label) = when (item) {
                                        is CatalogItem.Flight -> Icons.Filled.Flight to "Volo"
                                        is CatalogItem.Hotel -> Icons.Filled.Hotel to "Hotel"
                                        is CatalogItem.Excursion -> Icons.Filled.Tour to "Attività"
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(label, style = CatalogType.Overline, color = CatalogColors.InkMuted)
                                    }
                                    Text(item.title, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        if (!isLastPage) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.size(20.dp))
                                    } else {
                                        TextButton(onClick = { catalogViewModel.loadNextPage() }) {
                                            Text("Carica altri risultati", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
        }
    )
}

@Composable
private fun FareClassPicker(
    fareClasses: List<com.tripify.tripify_android.catalog.model.FareClassUi>,
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    val currency by com.tripify.tripify_android.catalog.util.rememberCatalogCurrency()
    Column {
        Text("Tariffa", style = CatalogType.Caption, color = CatalogColors.InkMuted)
        fareClasses.forEach { fareClass ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = fareClass.id == selectedId, onClick = { onSelect(fareClass.id) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = fareClass.id == selectedId, onClick = { onSelect(fareClass.id) },
                    colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark, unselectedColor = CatalogColors.InkSubtle)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${fareClass.name} — ${com.tripify.tripify_android.catalog.util.CatalogPriceFormatter.format(fareClass.price, currency)}", style = CatalogType.Body, color = CatalogColors.Ink)
            }
        }
    }
}

@Composable
private fun RoomTypePicker(
    roomTypes: List<com.tripify.tripify_android.catalog.model.RoomTypeUi>,
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    val currency by com.tripify.tripify_android.catalog.util.rememberCatalogCurrency()
    Column {
        Text("Camera", style = CatalogType.Caption, color = CatalogColors.InkMuted)
        roomTypes.forEach { roomType ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = roomType.id == selectedId, onClick = { onSelect(roomType.id) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = roomType.id == selectedId, onClick = { onSelect(roomType.id) },
                    colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark, unselectedColor = CatalogColors.InkSubtle)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${roomType.name} — ${com.tripify.tripify_android.catalog.util.CatalogPriceFormatter.format(roomType.price, currency)}/notte", style = CatalogType.Body, color = CatalogColors.Ink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    modifier: Modifier = Modifier,
    onPick: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label, style = CatalogType.Caption) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = CatalogColors.AccentDark)
            }
        },
        colors = DialogTextFieldColors,
        modifier = modifier
    )
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            colors = DatePickerDefaults.colors(containerColor = CatalogColors.Surface),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK", color = CatalogColors.AccentDark) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annulla", color = CatalogColors.InkMuted) } }
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = CatalogColors.Surface,
                    selectedDayContainerColor = CatalogColors.AccentDark,
                    todayDateBorderColor = CatalogColors.AccentDark,
                    todayContentColor = CatalogColors.AccentDark
                )
            )
        }
    }
}
