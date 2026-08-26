package com.tripify.tripify_android.itinerary.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.chat.repository.ChatRepository
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.util.extractUserIdFromToken
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryDetailState
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryDetailScreen(
    listId: Long,
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

    var currentUserId by remember { mutableStateOf<String?>(null) }
    var isBooking by remember { mutableStateOf(false) }
    var isChatting by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listId) {
        viewModel.loadDetail(listId)
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

                if (showPublishDialog) {
                    PublishDialog(
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

                Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        AsyncImage(
                            model = "https://picsum.photos/seed/itinerary${list.id}/900/600",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onNavigateBack, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.32f))) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (currentUserId != null) {
                                        viewModel.toggleLike(list.id)
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Accedi per mettere mi piace") }
                                    }
                                },
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White)
                            ) {
                                Icon(
                                    if (list.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Mi piace",
                                    tint = CatalogColors.Alert,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                        Text(list.name, style = CatalogType.DetailTitle, color = CatalogColors.Ink)
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

                        Spacer(modifier = Modifier.height(22.dp))
                        Text("TAPPE", style = CatalogType.Overline, color = CatalogColors.InkMuted)
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            list.items.forEach { item ->
                                ItineraryComponentRow(
                                    itemId = item.catalogItemId,
                                    catalogViewModel = catalogViewModel,
                                    onClick = { onNavigateToComponent(item.catalogItemId.toString()) }
                                )
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
                                viewModel.bookAll(list) { success, total ->
                                    isBooking = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (success == total) "Aggiunte $success tappe al carrello!"
                                            else "Aggiunte $success su $total tappe al carrello"
                                        )
                                    }
                                }
                            },
                            enabled = !isBooking,
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

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val token = tokenManager.tokenFlow.first()
                                    if (token.isNullOrBlank()) {
                                        snackbarHostState.showSnackbar("Accedi per contattare l'organizzatore")
                                        return@launch
                                    }
                                    isChatting = true
                                    val chatRoom = ChatRepository.getOrCreateChatRoom(hostId = list.ownerId, authToken = token)
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

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryComponentRow(
    itemId: Long,
    catalogViewModel: CatalogViewModel,
    onClick: () -> Unit
) {
    var item by remember(itemId) { mutableStateOf<CatalogItem?>(null) }

    LaunchedEffect(itemId) {
        item = catalogViewModel.getOrFetchItem(itemId.toInt())
    }

    val resolved = item
    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().clickable(enabled = resolved != null) { onClick() }
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (resolved == null) {
                Box(modifier = Modifier.size(52.dp).clip(CatalogShapes.Badge).background(CatalogColors.SurfaceMuted))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Caricamento…", style = CatalogType.Caption, color = CatalogColors.InkMuted)
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
                    Text(resolved.title, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CatalogColors.InkSubtle)
            }
        }
    }
}

@Composable
private fun PublishDialog(onDismiss: () -> Unit, onConfirm: (city: String) -> Unit) {
    var city by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    placeholder = { Text("es. Roma") },
                    singleLine = true,
                    shape = CatalogShapes.Field,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (city.isNotBlank()) onConfirm(city.trim()) }) {
                Text("Pubblica", color = CatalogColors.AccentDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = CatalogColors.InkMuted) }
        }
    )
}
