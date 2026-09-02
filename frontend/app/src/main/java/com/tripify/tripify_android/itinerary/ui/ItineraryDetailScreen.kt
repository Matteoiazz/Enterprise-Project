package com.tripify.tripify_android.itinerary.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.itinerary.util.extractUserNameFromToken
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.CatalogPriceFormatter
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.chat.repository.ChatRepository
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.data.FavoriteListItemDto
import com.tripify.tripify_android.itinerary.util.extractUserIdFromToken
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryDetailState
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryDetailScreen(
    listId: Long? = null,
    publicToken: String? = null,
    viewModel: ItineraryViewModel,
    catalogViewModel: CatalogViewModel,
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
    onNavigateToComponent: (String) -> Unit,
    onChatWithOrganizer: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val detailState by viewModel.detailState.collectAsState()
    val currency by tokenManager.currencyFlow.collectAsState(initial = "EUR")

    var currentUserId by remember { mutableStateOf<String?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    var isChatting by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var showItemPickerDialog by remember { mutableStateOf(false) }
    var pendingCatalogItem by remember { mutableStateOf<CatalogItem?>(null) }
    var indexToRemove by remember { mutableStateOf<Int?>(null) }
    var showDeleteListDialog by remember { mutableStateOf(false) }
    var isDeletingList by remember { mutableStateOf(false) }
    var bookAllErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentList = (detailState as? ItineraryDetailState.Success)?.list
    val isOwnerTopLevel = currentUserId != null && currentUserId == currentList?.ownerId

    LaunchedEffect(listId, publicToken) {
        if (publicToken != null) {
            viewModel.loadDetailByPublicToken(publicToken)
        } else if (listId != null) {
            viewModel.loadDetail(listId)
        }
        val token = tokenManager.tokenFlow.first()
        currentUserId = token?.let { extractUserIdFromToken(it) }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Itinerario", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                actions = {
                    if (currentList != null) {
                        IconButton(onClick = {
                            viewModel.exportCalendar(context, currentList.id, currentList.name) { error ->
                                scope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        }) {
                            Icon(Icons.Filled.Event, contentDescription = "Esporta calendario", tint = CatalogColors.Ink)
                        }
                    }
                    val shareToken = currentList?.publicToken
                    if (!shareToken.isNullOrBlank()) {
                        IconButton(onClick = {
                            val link = "tripify://itinerary/public/$shareToken"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Guarda il mio itinerario \"${currentList?.name}\" su Tripify: $link")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Condividi itinerario"))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Condividi", tint = CatalogColors.Ink)
                        }
                    }
                    if (isOwnerTopLevel) {
                        IconButton(onClick = { showDeleteListDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Elimina itinerario", tint = CatalogColors.Alert)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        }
    ) { innerPadding ->
        when (val state = detailState) {
            is ItineraryDetailState.Loading -> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CatalogColors.AccentDark)
                }
            }
            is ItineraryDetailState.Error -> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
            }
            is ItineraryDetailState.Success -> {
                val list = state.list
                val isOwner = currentUserId != null && currentUserId == list.ownerId
                // Un collaboratore (sharedUserIds) può modificare i componenti della lista,
                // ma non le azioni strutturali (elimina, visibilità, link) che restano owner-only.
                val canEdit = isOwner || (currentUserId != null && list.sharedUserIds.contains(currentUserId))
                var showRenameDialog by remember { mutableStateOf(false) }

                // Risolti tutti insieme (non riga per riga) cosi' si possono raggruppare
                // per giorno prima di disegnare la timeline sotto.
                var resolvedComponents by remember(list.items) { mutableStateOf<List<CatalogItem?>>(emptyList()) }
                var isResolvingComponents by remember(list.items) { mutableStateOf(true) }
                LaunchedEffect(list.items) {
                    isResolvingComponents = true
                    resolvedComponents = list.items.map { catalogViewModel.getOrFetchItem(it.catalogItemId.toInt()) }
                    isResolvingComponents = false
                }

                if (showRenameDialog) {
                    RenameDialog(
                        currentName = list.name,
                        onDismiss = { showRenameDialog = false },
                        onConfirm = { newName ->
                            showRenameDialog = false
                            viewModel.renameList(list.id, newName) { success ->
                                if (!success) scope.launch { snackbarHostState.showSnackbar("Impossibile rinominare l'itinerario") }
                            }
                        }
                    )
                }

                if (showPublishDialog) {
                    PublishDialog(
                        initialCity = list.city ?: "",
                        onDismiss = { showPublishDialog = false },
                        onConfirm = { city ->
                            showPublishDialog = false
                            viewModel.updateVisibility(list.id, "PUBLIC", city) { success, error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "Itinerario pubblicato!" else (error ?: "Impossibile pubblicare")
                                    )
                                }
                            }
                        }
                    )
                }

                if (showItemPickerDialog) {
                    CatalogItemPickerDialog(
                        catalogViewModel = catalogViewModel,
                        onDismiss = { showItemPickerDialog = false },
                        onSelect = { selected ->
                            showItemPickerDialog = false
                            pendingCatalogItem = selected
                        }
                    )
                }

                pendingCatalogItem?.let { catalogItem ->
                    AddToItineraryDialog(
                        catalogItem = catalogItem,
                        fixedListId = list.id,
                        onDismiss = { pendingCatalogItem = null },
                        onAdded = {
                            pendingCatalogItem = null
                            viewModel.loadDetail(list.id)
                            scope.launch { snackbarHostState.showSnackbar("Componente aggiunto all'itinerario") }
                        }
                    )
                }

                indexToRemove?.let { index ->
                    AlertDialog(
                        onDismissRequest = { indexToRemove = null },
                        containerColor = CatalogColors.Surface,
                        shape = CatalogShapes.Card,
                        title = { Text("Rimuovere questo componente?", style = CatalogType.Section, color = CatalogColors.Ink) },
                        text = { Text("Non potrai annullare questa azione.", style = CatalogType.Body, color = CatalogColors.InkMuted) },
                        confirmButton = {
                            TextButton(onClick = {
                                indexToRemove = null
                                viewModel.removeItem(list.id, index) { success, alsoRemoved ->
                                    scope.launch {
                                        val message = when {
                                            !success -> "Impossibile rimuovere il componente"
                                            alsoRemoved.isEmpty() -> "Componente rimosso"
                                            else -> "Rimossi anche: ${alsoRemoved.joinToString(", ")} (non più raggiungibili)"
                                        }
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            }) { Text("Rimuovi", style = CatalogType.LabelStrong, color = CatalogColors.Alert) }
                        },
                        dismissButton = {
                            TextButton(onClick = { indexToRemove = null }) { Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
                        }
                    )
                }

                Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        AsyncImage(
                            model = "https://picsum.photos/seed/itinerary${list.id}/900/600",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, CatalogColors.Scrim.copy(alpha = 0.35f))
                                    )
                                )
                        )
                        IconButton(
                            onClick = {
                                if (currentUserId != null) {
                                    viewModel.toggleLike(list.id)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Accedi per mettere mi piace") }
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).size(38.dp).clip(CircleShape).background(Color.White)
                        ) {
                            Icon(
                                if (list.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Mi piace",
                                tint = CatalogColors.Alert,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().background(CatalogColors.Surface, CatalogShapes.Sheet).offset(y = (-20).dp).padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(CatalogColors.Accent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text((list.city ?: "ITINERARIO").uppercase(), style = CatalogType.Overline, color = CatalogColors.InkMuted)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val roleLabel = if (isOwner) "Proprietario" else if (canEdit) "Collaboratore" else null
                        if (roleLabel != null) {
                            Surface(shape = CatalogShapes.Pill, color = CatalogColors.AccentSoft) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        if (isOwner) Icons.Filled.Star else Icons.Filled.Group,
                                        contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(roleLabel, style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold), color = CatalogColors.AccentDark)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (isOwner) {
                            Row(
                                modifier = Modifier.clickable { showRenameDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(list.name, style = CatalogType.DetailTitle, color = CatalogColors.Ink)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.Edit, contentDescription = "Rinomina", tint = CatalogColors.InkSubtle, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Text(list.name, style = CatalogType.DetailTitle, color = CatalogColors.Ink)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = CatalogColors.Alert, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${list.likesCount} mi piace", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                            Spacer(modifier = Modifier.width(14.dp))
                            Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${list.bookingsCount} prenotazioni", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                        }

                        if (list.totalPrice != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (currency == "USD") Icons.Filled.AttachMoney else Icons.Filled.Euro,
                                    contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Totale: ${CatalogPriceFormatter.symbolFor(currency)}%.2f".format(CatalogPriceFormatter.convert(list.totalPrice.toDouble(), currency)),
                                    style = CatalogType.BodyStrong, color = CatalogColors.Ink
                                )
                            }
                        }

                        if (isOwner && list.visibility != "PUBLIC") {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { showPublishDialog = true },
                                shape = CatalogShapes.Field,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.AccentDark)
                            ) {
                                Icon(Icons.Filled.Public, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rendi pubblica", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                            }
                        }

                        if (isOwner && list.visibility == "PUBLIC") {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateVisibility(list.id, "PRIVATE", null) { success, error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (success) "Itinerario reso privato" else (error ?: "Impossibile rendere privato")
                                            )
                                        }
                                    }
                                },
                                shape = CatalogShapes.Field,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rendi privata", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted)
                            }
                        }

                        // I due link (visualizzazione e invito) sono indipendenti dalla
                        // visibilità: funzionano anche su una lista privata o condivisa,
                        // senza i requisiti minimi di pubblicazione.
                        if (isOwner) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = CatalogShapes.Card,
                                color = CatalogColors.SurfaceMuted,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                            ) {
                                Column {
                                    val viewLink = list.publicToken?.takeIf { it.isNotBlank() }?.let { "tripify://itinerary/public/$it" }
                                    LinkRow(
                                        icon = Icons.Filled.Link,
                                        title = "Link di visualizzazione",
                                        subtitle = "Chi lo apre può vedere l'itinerario, anche senza accedere.",
                                        link = viewLink,
                                        onEnable = {
                                            viewModel.enableLinkSharing(list.id) { success ->
                                                if (!success) scope.launch { snackbarHostState.showSnackbar("Impossibile generare il link") }
                                            }
                                        },
                                        onShare = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Guarda il mio itinerario \"${list.name}\" su Tripify: $viewLink")
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Condividi itinerario"))
                                        },
                                        onDisable = {
                                            viewModel.disableLinkSharing(list.id) { success ->
                                                if (!success) scope.launch { snackbarHostState.showSnackbar("Impossibile disattivare il link") }
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = CatalogColors.Hairline)
                                    val inviteLink = list.collabToken?.takeIf { it.isNotBlank() }?.let { "tripify://itinerary/join/$it" }
                                    LinkRow(
                                        icon = Icons.Filled.GroupAdd,
                                        title = "Link di invito",
                                        subtitle = "Chi lo apre da loggato entra come collaboratore e può modificare la lista.",
                                        link = inviteLink,
                                        onEnable = {
                                            viewModel.enableCollabInvite(list.id) { success ->
                                                if (!success) scope.launch { snackbarHostState.showSnackbar("Impossibile generare l'invito") }
                                            }
                                        },
                                        onShare = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Aiutami a pianificare \"${list.name}\" su Tripify: $inviteLink")
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Invita a collaborare"))
                                        },
                                        onDisable = {
                                            viewModel.disableCollabInvite(list.id) { success ->
                                                if (!success) scope.launch { snackbarHostState.showSnackbar("Impossibile disattivare l'invito") }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TAPPE", style = CatalogType.Overline, color = CatalogColors.InkMuted)
                            if (canEdit) {
                                Row(
                                    modifier = Modifier.clickable { showItemPickerDialog = true },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AddCircle, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aggiungi", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (canEdit) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CatalogColors.AccentSoft, CatalogShapes.Field)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (list.items.isEmpty())
                                        "Aggiungi i componenti in ordine di viaggio: prima il volo di andata, poi l'hotel e/o le attività nella città di arrivo, poi il volo successivo. Ogni componente deve trovarsi nella stessa città e nelle stesse date del volo che ce lo porta."
                                    else
                                        "Ricorda: i componenti vanno aggiunti in ordine cronologico (prima il volo, poi hotel/attività nella città di arrivo, poi il volo successivo) — l'app rifiuta un componente se città o date non tornano con l'ultimo volo aggiunto.",
                                    style = CatalogType.Caption, color = CatalogColors.AccentDark
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (isResolvingComponents) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.size(22.dp))
                            }
                        } else {
                            val days = groupItineraryByDay(list.items, resolvedComponents)
                            Column {
                                days.forEachIndexed { dayIndex, (dateKey, entries) ->
                                    if (dateKey != null) {
                                        DayHeader(dayNumber = dayIndex + 1, dateLabel = formatDayLabel(dateKey))
                                    }
                                    entries.forEachIndexed { posInDay, entry ->
                                        val (icon, accent) = when (entry.resolved) {
                                            is CatalogItem.Flight -> Icons.Filled.Flight to CatalogColors.AccentDark
                                            is CatalogItem.Hotel -> Icons.Filled.Hotel to CatalogColors.Gold
                                            is CatalogItem.Excursion -> Icons.Filled.Tour to CatalogColors.Accent
                                            null -> Icons.Filled.QuestionMark to CatalogColors.InkSubtle
                                        }
                                        TimelineRail(
                                            accentColor = accent,
                                            icon = icon,
                                            isFirstOfDay = posInDay == 0,
                                            isLastOfDay = posInDay == entries.lastIndex
                                        ) {
                                            ItineraryComponentCard(
                                                item = entry.item,
                                                resolved = entry.resolved,
                                                canRemove = canEdit,
                                                onClick = { onNavigateToComponent(entry.item.catalogItemId.toString()) },
                                                onRemove = { indexToRemove = entry.index }
                                            )
                                        }
                                        if (posInDay != entries.lastIndex) Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (currentUserId == null) {
                                    scope.launch { snackbarHostState.showSnackbar("Accedi per prenotare") }
                                    return@Button
                                }
                                isBooking = true
                                viewModel.bookAll(list) { success, total, errors ->
                                    isBooking = false
                                    if (success == total) {
                                        scope.launch { snackbarHostState.showSnackbar("Aggiunte $success tappe al carrello!") }
                                    } else if (errors.isNotEmpty()) {
                                        bookAllErrors = errors
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Aggiunte $success su $total tappe al carrello") }
                                    }
                                }
                            },
                            enabled = !isBooking && list.items.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Field,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (isBooking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PRENOTA TUTTO", style = CatalogType.Button, color = Color.White)
                            }
                        }

                        // Non ha senso chattare con l'organizzatore quando l'organizzatore sei tu.
                        if (!isOwner) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val token = tokenManager.tokenFlow.first()
                                        if (token.isNullOrBlank()) {
                                            snackbarHostState.showSnackbar("Accedi per contattare l'organizzatore")
                                            return@launch
                                        }

                                        // Estraiamo il nome dal token
                                        val travelerName = extractUserNameFromToken(token) ?: "Cliente"

                                        isChatting = true
                                        // Passiamo anche il travelerName al backend
                                        val chatRoom = ChatRepository.getOrCreateChatRoom(
                                            hostId = list.ownerId,
                                            title = "Organizzatore ${list.name}",
                                            travelerName = travelerName,
                                            authToken = token
                                        )
                                        isChatting = false

                                        if (chatRoom != null) {
                                            onChatWithOrganizer(chatRoom.id)
                                        } else {
                                            snackbarHostState.showSnackbar("Impossibile aprire la chat con l'organizzatore")
                                        }
                                    }
                                },
                                enabled = !isChatting,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = CatalogShapes.Field,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                            ) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Chat", tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chatta con l'organizzatore", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteListDialog && currentList != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingList) showDeleteListDialog = false },
            containerColor = CatalogColors.Surface,
            shape = CatalogShapes.Card,
            title = { Text("Eliminare l'itinerario?", style = CatalogType.Section, color = CatalogColors.Ink) },
            text = { Text("\"${currentList.name}\" e tutte le sue tappe verranno eliminati. Non potrai annullare questa azione.", style = CatalogType.Body, color = CatalogColors.InkMuted) },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingList,
                    onClick = {
                        isDeletingList = true
                        viewModel.deleteList(currentList.id) { success ->
                            isDeletingList = false
                            showDeleteListDialog = false
                            if (success) {
                                onNavigateBack()
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Impossibile eliminare l'itinerario") }
                            }
                        }
                    }
                ) { Text("Elimina", style = CatalogType.LabelStrong, color = CatalogColors.Alert) }
            },
            dismissButton = {
                TextButton(enabled = !isDeletingList, onClick = { showDeleteListDialog = false }) {
                    Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted)
                }
            }
        )
    }

    if (bookAllErrors.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { bookAllErrors = emptyList() },
            containerColor = CatalogColors.Surface,
            shape = CatalogShapes.Card,
            title = { Text("Alcune tappe non sono state aggiunte", style = CatalogType.Section, color = CatalogColors.Ink) },
            text = {
                Column {
                    bookAllErrors.forEach { error ->
                        Text("• $error", style = CatalogType.Body, color = CatalogColors.InkMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { bookAllErrors = emptyList() }) {
                    Text("Ho capito", style = CatalogType.LabelStrong, color = CatalogColors.Ink)
                }
            }
        )
    }
}

/** Una tappa gia' risolta contro il catalogo, con l'indice originale in list.items (serve a onRemove). */
private data class TimelineEntry(val index: Int, val item: FavoriteListItemDto, val resolved: CatalogItem?)

/** Chiave del giorno a cui appartiene una tappa: data del volo/check-in/attivita'. Null se non risolvibile. */
private fun dayKeyFor(item: FavoriteListItemDto, resolved: CatalogItem?): String? = when (resolved) {
    is CatalogItem.Flight -> resolved.departureTime.take(10)
    is CatalogItem.Hotel -> item.checkIn
    is CatalogItem.Excursion -> item.activityDate
    null -> null
}

/** Raggruppa le tappe (gia' in ordine cronologico) in giorni consecutivi con la stessa data. */
private fun groupItineraryByDay(items: List<FavoriteListItemDto>, resolved: List<CatalogItem?>): List<Pair<String?, List<TimelineEntry>>> {
    val groups = mutableListOf<Pair<String?, MutableList<TimelineEntry>>>()
    items.forEachIndexed { index, item ->
        val entry = TimelineEntry(index, item, resolved.getOrNull(index))
        val key = dayKeyFor(item, entry.resolved)
        if (groups.isNotEmpty() && groups.last().first == key) {
            groups.last().second.add(entry)
        } else {
            groups.add(key to mutableListOf(entry))
        }
    }
    return groups
}

private fun formatDayLabel(dayKey: String): String = try {
    java.time.LocalDate.parse(dayKey)
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.ITALIAN))
} catch (e: Exception) {
    dayKey
}

@Composable
private fun DayHeader(dayNumber: Int, dateLabel: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = if (dayNumber == 1) 0.dp else 20.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(CatalogColors.AccentDark),
            contentAlignment = Alignment.Center
        ) {
            Text("$dayNumber", style = CatalogType.Caption.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("Giorno $dayNumber", style = CatalogType.LabelStrong, color = CatalogColors.Ink)
        Spacer(modifier = Modifier.width(6.dp))
        Text(dateLabel, style = CatalogType.Caption, color = CatalogColors.InkMuted)
    }
}

/**
 * Riga della timeline: pallino colorato per tipo di componente su una linea verticale
 * continua (assente sopra la prima tappa del giorno e sotto l'ultima), con il contenuto
 * a destra. L'altezza della rotaia segue quella del contenuto (Row a IntrinsicSize.Min).
 */
@Composable
private fun TimelineRail(
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isFirstOfDay: Boolean,
    isLastOfDay: Boolean,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp).fillMaxHeight()) {
            Box(
                modifier = Modifier.width(2.dp).weight(1f)
                    .background(if (isFirstOfDay) Color.Transparent else CatalogColors.Hairline)
            )
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
            Box(
                modifier = Modifier.width(2.dp).weight(1f)
                    .background(if (isLastOfDay) Color.Transparent else CatalogColors.Hairline)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun ItineraryComponentCard(
    item: FavoriteListItemDto,
    resolved: CatalogItem?,
    canRemove: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val currency by rememberCatalogCurrency()

    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().clickable(enabled = resolved != null) { onClick() }
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (resolved == null) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CatalogShapes.Badge).background(CatalogColors.SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QuestionMark, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Componente non più disponibile", style = CatalogType.Caption, color = CatalogColors.InkMuted)
            } else {
                AsyncImage(
                    model = resolved.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(CatalogShapes.Badge)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val (icon, label) = when (resolved) {
                        is CatalogItem.Flight -> Icons.Filled.Flight to "Volo"
                        is CatalogItem.Hotel -> Icons.Filled.Hotel to "Hotel"
                        is CatalogItem.Excursion -> Icons.Filled.Tour to "Attività"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label, style = CatalogType.Overline, color = CatalogColors.InkMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(resolved.title, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (item.quantity > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("×${item.quantity}", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                        }
                    }

                    when (resolved) {
                        is CatalogItem.Flight -> {
                            Text(
                                "${resolved.departureCity} → ${resolved.arrivalCity} · ${resolved.departureTime}",
                                style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            val fareClass = resolved.fareClasses.find { it.id.toLong() == item.fareClassId }
                            if (fareClass != null) {
                                Text(fareClass.name, style = CatalogType.Caption, color = CatalogColors.InkSubtle, maxLines = 1)
                            }
                        }
                        is CatalogItem.Hotel -> {
                            val nights = nightsBetween(item.checkIn, item.checkOut)
                            if (item.checkIn != null && item.checkOut != null) {
                                Text(
                                    "${item.checkIn} → ${item.checkOut}" + (nights?.let { " · $it ${if (it == 1L) "notte" else "notti"}" } ?: ""),
                                    style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            val roomType = resolved.roomTypes.find { it.id.toLong() == item.roomTypeId }
                            if (roomType != null) {
                                Text(roomType.name, style = CatalogType.Caption, color = CatalogColors.InkSubtle, maxLines = 1)
                            }
                        }
                        is CatalogItem.Excursion -> {
                            if (item.activityDate != null) {
                                Text(item.activityDate, style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1)
                            }
                        }
                    }
                }
                if (item.price != null) {
                    Text(
                        "${CatalogPriceFormatter.symbolFor(currency)}%.2f".format(CatalogPriceFormatter.convert(item.price.toDouble(), currency)),
                        style = CatalogType.BodyStrong, color = CatalogColors.AccentDark,
                        maxLines = 1, modifier = Modifier.padding(end = 6.dp)
                    )
                }
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Rimuovi", tint = CatalogColors.Alert, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CatalogColors.InkSubtle)
                }
            }
        }
    }
}

private fun nightsBetween(checkIn: String?, checkOut: String?): Long? {
    if (checkIn == null || checkOut == null) return null
    return try {
        java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(checkIn), java.time.LocalDate.parse(checkOut))
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun PublishDialog(initialCity: String = "", onDismiss: () -> Unit, onConfirm: (city: String) -> Unit) {
    var city by remember { mutableStateOf(initialCity) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Card,
        title = { Text("Rendi pubblica la lista", style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            Column {
                Text(
                    "Servono almeno 2 voli, 1 hotel e 1 attività tra i componenti. Indica la città di riferimento:",
                    style = CatalogType.Body, color = CatalogColors.InkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    placeholder = { Text("es. Roma", style = CatalogType.Label, color = CatalogColors.InkSubtle) },
                    singleLine = true,
                    shape = CatalogShapes.Field,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CatalogColors.Accent,
                        unfocusedBorderColor = CatalogColors.Hairline,
                        focusedContainerColor = CatalogColors.Surface,
                        unfocusedContainerColor = CatalogColors.Surface,
                        cursorColor = CatalogColors.AccentDark,
                        focusedTextColor = CatalogColors.Ink,
                        unfocusedTextColor = CatalogColors.Ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (city.isNotBlank()) onConfirm(city.trim()) }) {
                Text("Pubblica", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
        }
    )
}

/** Una riga della card "link": badge icona, titolo/descrizione, e a destra "Attiva" oppure copia+condividi+disattiva. */
@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    link: String?,
    onEnable: () -> Unit,
    onShare: () -> Unit,
    onDisable: () -> Unit
) {
    val isActive = link != null
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var justCopied by remember(link) { mutableStateOf(false) }
    LaunchedEffect(justCopied) {
        if (justCopied) {
            kotlinx.coroutines.delay(1200)
            justCopied = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isActive) CatalogColors.AccentSoft else CatalogColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isActive) CatalogColors.AccentDark else CatalogColors.InkSubtle, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = CatalogType.LabelStrong, color = CatalogColors.Ink)
            Text(subtitle, style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (link != null) {
            IconButton(
                onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link))
                    justCopied = true
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    if (justCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copia link",
                    tint = if (justCopied) CatalogColors.AccentDark else CatalogColors.InkSubtle,
                    modifier = Modifier.size(17.dp)
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Condividi", tint = CatalogColors.AccentDark, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDisable, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Disattiva", tint = CatalogColors.InkSubtle, modifier = Modifier.size(17.dp))
            }
        } else {
            TextButton(onClick = onEnable, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                Text("Attiva", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
            }
        }
    }
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (name: String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatalogColors.Surface,
        shape = CatalogShapes.Card,
        title = { Text("Rinomina itinerario", style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = CatalogShapes.Field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatalogColors.Accent,
                    unfocusedBorderColor = CatalogColors.Hairline,
                    focusedContainerColor = CatalogColors.Surface,
                    unfocusedContainerColor = CatalogColors.Surface,
                    cursorColor = CatalogColors.AccentDark,
                    focusedTextColor = CatalogColors.Ink,
                    unfocusedTextColor = CatalogColors.Ink
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Salva", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
        }
    )
}
