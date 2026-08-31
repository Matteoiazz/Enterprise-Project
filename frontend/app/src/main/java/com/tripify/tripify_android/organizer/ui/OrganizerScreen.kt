package com.tripify.tripify_android.organizer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.CatalogApi
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.CatalogItemDto
import com.tripify.tripify_android.data.model.CreateActivityRequest
import com.tripify.tripify_android.data.model.CreateFareClassRequest
import com.tripify.tripify_android.data.model.CreateFlightRequest
import com.tripify.tripify_android.data.model.CreateHotelRequest
import com.tripify.tripify_android.data.model.CreateRoomTypeRequest
import com.tripify.tripify_android.data.model.OrganizerItemDto
import com.tripify.tripify_android.organizer.viewmodel.OrganizerViewModel
import kotlinx.coroutines.launch

private enum class OrganizerTab { ANNUNCI, PRENOTAZIONI }
private enum class ListingType { FLIGHT, HOTEL, ACTIVITY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(
    viewModel: OrganizerViewModel,
    catalogViewModel: CatalogViewModel,
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit
) {
    var tab by remember { mutableStateOf(OrganizerTab.ANNUNCI) }
    val myItems by viewModel.myItems.collectAsState()
    val receivedBookings by viewModel.receivedBookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val catalogApi = remember { RetrofitClient.createCatalogApi(tokenManager) }
    val profileApi = remember { RetrofitClient.createProfileApi(tokenManager) }

    var showTypePicker by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<Int?>(null) }
    var editingItemType by remember { mutableStateOf<ListingType?>(null) }
    var editingItemDto by remember { mutableStateOf<CatalogItemDto?>(null) }
    var itemToDelete by remember { mutableStateOf<OrganizerItemDto?>(null) }

    LaunchedEffect(tab) {
        when (tab) {
            OrganizerTab.ANNUNCI -> viewModel.loadMyItems()
            OrganizerTab.PRENOTAZIONI -> viewModel.loadReceivedBookings()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    fun openEdit(item: OrganizerItemDto) {
        scope.launch {
            val dto = try { catalogApi.getItemById(item.id) } catch (e: Exception) { null }
            if (dto == null) {
                snackbarHostState.showSnackbar("Impossibile caricare l'annuncio")
                return@launch
            }
            editingItemId = item.id
            editingItemType = when (item.itemType.uppercase()) {
                "FLIGHT" -> ListingType.FLIGHT
                "HOTEL" -> ListingType.HOTEL
                else -> ListingType.ACTIVITY
            }
            editingItemDto = dto
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Organizzatore", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        },
        floatingActionButton = {
            if (tab == OrganizerTab.ANNUNCI) {
                FloatingActionButton(onClick = { showTypePicker = true }, containerColor = CatalogColors.AccentDark) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuovo annuncio", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tab == OrganizerTab.ANNUNCI,
                    onClick = { tab = OrganizerTab.ANNUNCI },
                    label = { Text("I miei annunci", style = CatalogType.Caption) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White),
                    shape = CatalogShapes.Chip
                )
                FilterChip(
                    selected = tab == OrganizerTab.PRENOTAZIONI,
                    onClick = { tab = OrganizerTab.PRENOTAZIONI },
                    label = { Text("Prenotazioni ricevute", style = CatalogType.Caption) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White),
                    shape = CatalogShapes.Chip
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
                } else when (tab) {
                    OrganizerTab.ANNUNCI -> {
                        if (myItems.isEmpty()) {
                            EmptyState("Non hai ancora nessun annuncio", "Creane uno con il pulsante +")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(myItems, key = { it.id }) { item ->
                                    OrganizerItemRow(
                                        item = item,
                                        onEdit = { openEdit(item) },
                                        onDelete = { itemToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                    OrganizerTab.PRENOTAZIONI -> {
                        if (receivedBookings.isEmpty()) {
                            EmptyState("Nessuna prenotazione ricevuta", "Compariranno qui le prenotazioni fatte sui tuoi annunci")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(receivedBookings, key = { "${it.bookingId}-${it.catalogItemId}" }) { line ->
                                    ReceivedBookingRow(line = line, catalogViewModel = catalogViewModel, profileApi = profileApi)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTypePicker) {
        ListingTypePickerDialog(
            onDismiss = { showTypePicker = false },
            onSelect = { type ->
                showTypePicker = false
                editingItemType = type
                editingItemId = null
                editingItemDto = null
            }
        )
    }

    // Form di creazione: si apre quando è scelto un tipo ma non c'è ancora un id
    // (editingItemId==null) né un dto caricato per la modifica.
    if (editingItemType != null && editingItemId == null && editingItemDto == null) {
        when (editingItemType) {
            ListingType.FLIGHT -> FlightFormDialog(existing = null, onDismiss = { editingItemType = null }, onSubmit = { req ->
                viewModel.createFlight(req) { success ->
                    if (success) { editingItemType = null; scope.launch { snackbarHostState.showSnackbar("Volo creato") } }
                }
            })
            ListingType.HOTEL -> HotelFormDialog(existing = null, onDismiss = { editingItemType = null }, onSubmit = { req ->
                viewModel.createHotel(req) { success ->
                    if (success) { editingItemType = null; scope.launch { snackbarHostState.showSnackbar("Hotel creato") } }
                }
            })
            ListingType.ACTIVITY -> ActivityFormDialog(existing = null, onDismiss = { editingItemType = null }, onSubmit = { req ->
                viewModel.createActivity(req) { success ->
                    if (success) { editingItemType = null; scope.launch { snackbarHostState.showSnackbar("Attività creata") } }
                }
            })
            null -> {}
        }
    }

    // Form di modifica: c'è sia il tipo che il dto già caricato.
    if (editingItemDto != null && editingItemId != null) {
        val id = editingItemId!!
        val dto = editingItemDto!!
        fun closeEdit() { editingItemId = null; editingItemType = null; editingItemDto = null }
        when (editingItemType) {
            ListingType.FLIGHT -> FlightFormDialog(existing = dto, onDismiss = { closeEdit() }, onSubmit = { req ->
                viewModel.updateFlight(id, req) { success ->
                    if (success) { closeEdit(); scope.launch { snackbarHostState.showSnackbar("Volo aggiornato") } }
                }
            })
            ListingType.HOTEL -> HotelFormDialog(existing = dto, onDismiss = { closeEdit() }, onSubmit = { req ->
                viewModel.updateHotel(id, req) { success ->
                    if (success) { closeEdit(); scope.launch { snackbarHostState.showSnackbar("Hotel aggiornato") } }
                }
            })
            ListingType.ACTIVITY -> ActivityFormDialog(existing = dto, onDismiss = { closeEdit() }, onSubmit = { req ->
                viewModel.updateActivity(id, req) { success ->
                    if (success) { closeEdit(); scope.launch { snackbarHostState.showSnackbar("Attività aggiornata") } }
                }
            })
            null -> {}
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = CatalogColors.Surface,
            shape = CatalogShapes.Card,
            title = { Text("Eliminare \"${item.title}\"?", style = CatalogType.Section, color = CatalogColors.Ink) },
            text = { Text("Sparirà dalla ricerca. Chi l'ha già prenotato continuerà a vederlo nelle proprie prenotazioni.", style = CatalogType.Body, color = CatalogColors.InkMuted) },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete = null
                    viewModel.deleteItem(item.id) { success ->
                        scope.launch { snackbarHostState.showSnackbar(if (success) "Annuncio eliminato" else "Impossibile eliminare l'annuncio") }
                    }
                }) { Text("Elimina", style = CatalogType.LabelStrong, color = CatalogColors.Alert) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
            }
        )
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Storefront, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = CatalogType.Section, color = CatalogColors.Ink)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, style = CatalogType.Body, color = CatalogColors.InkMuted)
    }
}

@Composable
private fun OrganizerItemRow(item: OrganizerItemDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (item.itemType.uppercase()) {
                "FLIGHT" -> Icons.Filled.Flight
                "HOTEL" -> Icons.Filled.Hotel
                else -> Icons.Filled.Tour
            }
            Icon(icon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1)
                Text("€${"%.2f".format(item.price)}", style = CatalogType.Caption, color = CatalogColors.AccentDark)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Modifica", tint = CatalogColors.InkMuted) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Elimina", tint = CatalogColors.Alert) }
        }
    }
}

@Composable
private fun ReceivedBookingRow(
    line: com.tripify.tripify_android.data.model.ReceivedBookingLineDto,
    catalogViewModel: CatalogViewModel,
    profileApi: com.tripify.tripify_android.profile.api.ProfileApiService
) {
    var resolved by remember(line.catalogItemId) { mutableStateOf<CatalogItem?>(null) }
    LaunchedEffect(line.catalogItemId) { resolved = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt()) }

    var buyer by remember(line.buyerUserId) { mutableStateOf<com.tripify.tripify_android.data.UserResponse?>(null) }
    LaunchedEffect(line.buyerUserId) {
        buyer = try { profileApi.getUserSummary(line.buyerUserId) } catch (e: Exception) { null }
    }

    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(resolved?.title ?: "Articolo #${line.catalogItemId}", style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1)
                Text("€${"%.2f".format(line.price)}", style = CatalogType.BodyStrong, color = CatalogColors.AccentDark)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val buyerLabel = buyer?.let { b -> "${b.name ?: ""} ${b.surname ?: ""}".trim().ifEmpty { b.email } } ?: line.buyerUserId
            Text("Prenotato da: $buyerLabel", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            Text("Prenotazione #${line.bookingId} · ${line.status}", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            if (line.checkIn != null && line.checkOut != null) {
                Text("${line.checkIn} → ${line.checkOut}", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            }
            line.quantity?.let { Text("Quantità: $it", style = CatalogType.Caption, color = CatalogColors.InkMuted) }
        }
    }
}

@Composable
private fun ListingTypePickerDialog(onDismiss: () -> Unit, onSelect: (ListingType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Card,
        title = { Text("Nuovo annuncio", style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            Column {
                listOf(
                    Triple(ListingType.FLIGHT, Icons.Filled.Flight, "Volo"),
                    Triple(ListingType.HOTEL, Icons.Filled.Hotel, "Hotel"),
                    Triple(ListingType.ACTIVITY, Icons.Filled.Tour, "Attività")
                ).forEach { (type, icon, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(onClick = { onSelect(type) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = CatalogColors.InkMuted) } }
    )
}
