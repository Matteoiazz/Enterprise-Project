package com.tripify.tripify_android.organizer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.CatalogPriceFormatter
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency
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
    val context = LocalContext.current
    var tab by remember { mutableStateOf(OrganizerTab.ANNUNCI) }
    val myItems by viewModel.myItems.collectAsState()
    val receivedBookings by viewModel.receivedBookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val itemCache by catalogViewModel.itemCache.collectAsState()
    val currency by rememberCatalogCurrency()

    LaunchedEffect(receivedBookings) {
        receivedBookings.map { it.catalogItemId.toInt() }.distinct().forEach { catalogViewModel.getOrFetchItem(it) }
    }

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
                ExtendedFloatingActionButton(
                    onClick = { showTypePicker = true },
                    containerColor = CatalogColors.AccentDark,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuovo annuncio", style = CatalogType.Button) }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OrganizerTabBar(
                selected = tab,
                onSelect = { tab = it },
                annunciCount = myItems.size,
                prenotazioniCount = receivedBookings.size
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
                } else when (tab) {
                    OrganizerTab.ANNUNCI -> {
                        if (myItems.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.Storefront,
                                title = "Non hai ancora nessun annuncio",
                                subtitle = "Crea il tuo primo annuncio con il pulsante \"Nuovo annuncio\""
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(myItems, key = { it.id }) { item ->
                                    OrganizerItemRow(
                                        item = item,
                                        catalogViewModel = catalogViewModel,
                                        onEdit = { openEdit(item) },
                                        onDelete = { itemToDelete = item },
                                        isSubmitting = isSubmitting,
                                        onReactivate = {
                                            viewModel.reactivateItem(item.id) { success ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (success) "Annuncio riattivato" else "Impossibile riattivare l'annuncio"
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OrganizerTab.PRENOTAZIONI -> {
                        if (receivedBookings.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.EventNote,
                                title = "Nessuna prenotazione ricevuta",
                                subtitle = "Qui compariranno le prenotazioni fatte sui tuoi annunci"
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(key = "summary") {
                                    ReceivedBookingsSummary(
                                        lines = receivedBookings,
                                        titleFor = { id -> itemCache[id.toInt()]?.title },
                                        currency = currency
                                    )
                                }
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
    if (editingItemType != null && editingItemId == null && editingItemDto == null) {
        when (editingItemType) {
            ListingType.FLIGHT -> FlightFormDialog(existing = null, onDismiss = { editingItemType = null }, isSubmitting = isSubmitting, onSubmit = { req ->
                viewModel.createFlight(req) { success ->
                    if (success) { editingItemType = null; scope.launch { snackbarHostState.showSnackbar("Volo creato") } }
                }
            })
            ListingType.HOTEL -> HotelFormDialog(existing = null, onDismiss = { editingItemType = null }, isSubmitting = isSubmitting, onSubmit = { req, uris ->
                viewModel.createHotel(req, uris, context) { success ->
                    if (success) { editingItemType = null; scope.launch { snackbarHostState.showSnackbar("Hotel creato") } }
                }
            })
            ListingType.ACTIVITY -> ActivityFormDialog(existing = null, onDismiss = { editingItemType = null }, isSubmitting = isSubmitting, onSubmit = { req ->
                viewModel.createActivity(req) { success ->
                    if (success) { editingItemType = null; scope.launch { snackbarHostState.showSnackbar("Attività creata") } }
                }
            })
            null -> {}
        }
    }

    if (editingItemDto != null && editingItemId != null) {
        val id = editingItemId!!
        val dto = editingItemDto!!
        fun closeEdit() { editingItemId = null; editingItemType = null; editingItemDto = null }
        when (editingItemType) {
            ListingType.FLIGHT -> FlightFormDialog(existing = dto, onDismiss = { closeEdit() }, isSubmitting = isSubmitting, onSubmit = { req ->
                viewModel.updateFlight(id, req) { success ->
                    if (success) { closeEdit(); scope.launch { snackbarHostState.showSnackbar("Volo aggiornato") } }
                }
            })
            ListingType.HOTEL -> HotelFormDialog(
                existing = dto,
                onDismiss = { closeEdit() },
                isSubmitting = isSubmitting,
                onSubmit = { req, uris ->
                    viewModel.updateHotel(id, req, uris, context) { success ->
                        if (success) { closeEdit(); scope.launch { snackbarHostState.showSnackbar("Hotel aggiornato") } }
                    }
                },
                onDeleteImage = { url -> viewModel.deleteHotelImage(id, url) {} }
            )
            ListingType.ACTIVITY -> ActivityFormDialog(existing = dto, onDismiss = { closeEdit() }, isSubmitting = isSubmitting, onSubmit = { req ->
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
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(CatalogColors.AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = CatalogType.Section, color = CatalogColors.Ink, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, style = CatalogType.Body, color = CatalogColors.InkMuted, textAlign = TextAlign.Center)
    }
}

private data class ListingTypeMeta(val label: String, val icon: ImageVector)

private fun listingTypeMeta(itemType: String): ListingTypeMeta = when (itemType.uppercase()) {
    "FLIGHT" -> ListingTypeMeta("Volo", Icons.Filled.Flight)
    "HOTEL" -> ListingTypeMeta("Hotel", Icons.Filled.Hotel)
    else -> ListingTypeMeta("Attività", Icons.Filled.Tour)
}

@Composable
private fun OrganizerTabBar(
    selected: OrganizerTab,
    onSelect: (OrganizerTab) -> Unit,
    annunciCount: Int,
    prenotazioniCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(CatalogShapes.Pill)
            .background(CatalogColors.SurfaceMuted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OrganizerTabSegment("Annunci", annunciCount, selected == OrganizerTab.ANNUNCI, Modifier.weight(1f)) { onSelect(OrganizerTab.ANNUNCI) }
        OrganizerTabSegment("Prenotazioni", prenotazioniCount, selected == OrganizerTab.PRENOTAZIONI, Modifier.weight(1f)) { onSelect(OrganizerTab.PRENOTAZIONI) }
    }
}

@Composable
private fun OrganizerTabSegment(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) CatalogColors.Surface else Color.Transparent, label = "tabBg")
    val fg by animateColorAsState(if (selected) CatalogColors.Ink else CatalogColors.InkMuted, label = "tabFg")
    Row(
        modifier = modifier
            .clip(CatalogShapes.Pill)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = CatalogType.LabelStrong, color = fg, maxLines = 1)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                style = CatalogType.Caption,
                color = if (selected) CatalogColors.AccentDark else CatalogColors.InkSubtle
            )
        }
    }
}

@Composable
private fun TypeBadge(meta: ListingTypeMeta) {
    Row(
        modifier = Modifier
            .clip(CatalogShapes.Badge)
            .background(CatalogColors.AccentSoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(meta.icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(meta.label.uppercase(), style = CatalogType.Overline, color = CatalogColors.AccentDark)
    }
}

@Composable
private fun OrganizerItemRow(
    item: OrganizerItemDto,
    catalogViewModel: CatalogViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isSubmitting: Boolean = false,
    onReactivate: () -> Unit = {}
) {
    val currency by rememberCatalogCurrency()
    var resolved by remember(item.id) { mutableStateOf(catalogViewModel.itemCache.value[item.id]) }
    LaunchedEffect(item.id) {
        if (resolved == null) resolved = catalogViewModel.getOrFetchItem(item.id)
    }
    val meta = listingTypeMeta(item.itemType)

    Surface(
        shape = CatalogShapes.Card,
        color = CatalogColors.Surface,
        border = BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CatalogShapes.Field).background(CatalogColors.SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    val cover = resolved?.imageUrls?.firstOrNull()
                    if (cover != null) {
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(meta.icon, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypeBadge(meta)
                        if (!item.isActive) {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = CatalogShapes.Pill, color = CatalogColors.AlertSoft) {
                                Text(
                                    "DISATTIVATO",
                                    style = CatalogType.Overline,
                                    color = CatalogColors.Alert,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.title,
                        style = CatalogType.BodyStrong,
                        color = CatalogColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        CatalogPriceFormatter.format(item.price, currency),
                        style = CatalogType.LabelStrong,
                        color = CatalogColors.AccentDark
                    )
                }
            }
            HorizontalDivider(color = CatalogColors.Hairline)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Modifica", style = CatalogType.Label, color = CatalogColors.InkMuted)
                }
                if (item.isActive) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = CatalogColors.Alert, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Elimina", style = CatalogType.Label, color = CatalogColors.Alert)
                    }
                } else {
                    TextButton(onClick = onReactivate, enabled = !isSubmitting) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CatalogColors.AccentDark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Riattiva", style = CatalogType.Label, color = CatalogColors.AccentDark)
                    }
                }
            }
        }
    }
}

private data class BookingStatusStyle(val label: String, val container: Color, val content: Color)

private fun bookingStatusStyle(status: String): BookingStatusStyle = when (status.uppercase()) {
    "CONFIRMED" -> BookingStatusStyle("Confermata", CatalogColors.AccentSoft, CatalogColors.AccentDark)
    "PENDING" -> BookingStatusStyle("In attesa", CatalogColors.GoldSoft, CatalogColors.Gold)
    "CANCELLED", "CANCELED" -> BookingStatusStyle("Annullata", CatalogColors.AlertSoft, CatalogColors.Alert)
    "COMPLETED" -> BookingStatusStyle("Completata", CatalogColors.SurfaceMuted, CatalogColors.InkMuted)
    else -> BookingStatusStyle(status.lowercase().replaceFirstChar { it.uppercase() }, CatalogColors.SurfaceMuted, CatalogColors.InkMuted)
}

@Composable
private fun StatusChip(style: BookingStatusStyle) {
    Text(
        text = style.label.uppercase(),
        style = CatalogType.Overline,
        color = style.content,
        modifier = Modifier
            .clip(CatalogShapes.Badge)
            .background(style.container)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun BuyerAvatar(label: String) {
    val initial = label.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(CatalogColors.AccentDark),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, style = CatalogType.LabelStrong, color = Color.White)
    }
}

@Composable
private fun ReceivedBookingRow(
    line: com.tripify.tripify_android.data.model.ReceivedBookingLineDto,
    catalogViewModel: CatalogViewModel,
    profileApi: com.tripify.tripify_android.profile.api.ProfileApiService
) {
    val currency by rememberCatalogCurrency()
    var resolved by remember(line.catalogItemId) { mutableStateOf(catalogViewModel.itemCache.value[line.catalogItemId.toInt()]) }
    LaunchedEffect(line.catalogItemId) {
        if (resolved == null) resolved = catalogViewModel.getOrFetchItem(line.catalogItemId.toInt())
    }

    var buyer by remember(line.buyerUserId) { mutableStateOf<com.tripify.tripify_android.data.UserResponse?>(null) }
    LaunchedEffect(line.buyerUserId) {
        buyer = try { profileApi.getUserSummary(line.buyerUserId) } catch (e: Exception) { null }
    }

    val buyerLabel = buyer?.let { b -> "${b.name ?: ""} ${b.surname ?: ""}".trim().ifEmpty { b.email } } ?: line.buyerUserId
    val statusStyle = bookingStatusStyle(line.status)

    Surface(
        shape = CatalogShapes.Card,
        color = CatalogColors.Surface,
        border = BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CatalogShapes.Field).background(CatalogColors.SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    val cover = resolved?.imageUrls?.firstOrNull()
                    if (cover != null) {
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        resolved?.title ?: "Articolo #${line.catalogItemId}",
                        style = CatalogType.BodyStrong,
                        color = CatalogColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        CatalogPriceFormatter.format(line.price, currency),
                        style = CatalogType.LabelStrong,
                        color = CatalogColors.AccentDark
                    )
                }
                StatusChip(statusStyle)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CatalogColors.Hairline)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BuyerAvatar(buyerLabel)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(buyerLabel, style = CatalogType.Label, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Prenotazione #${line.bookingId} · ${line.bookingDate}", style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1)
                }
            }

            if ((line.checkIn != null && line.checkOut != null) || line.quantity != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (line.checkIn != null && line.checkOut != null) {
                        BookingMetaChip(Icons.Filled.DateRange, "${line.checkIn} → ${line.checkOut}")
                    }
                    line.quantity?.let { BookingMetaChip(Icons.Filled.ConfirmationNumber, "x$it") }
                }
            }
        }
    }
}

@Composable
private fun BookingMetaChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(CatalogShapes.Badge)
            .background(CatalogColors.SurfaceMuted)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1)
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
