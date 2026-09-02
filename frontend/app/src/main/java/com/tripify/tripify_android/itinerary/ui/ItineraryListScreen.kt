package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.LocalBottomNavBarHeight
import com.tripify.tripify_android.catalog.ui.components.ClearFieldButton
import com.tripify.tripify_android.catalog.ui.components.PhotoCard
import com.tripify.tripify_android.catalog.ui.components.PhotoMeta
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.data.FavoriteListItemDto
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryFeedState
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel
import kotlinx.coroutines.flow.first

private fun itineraryImageUrl(list: FavoriteListDto): String =
    "https://picsum.photos/seed/itinerary${list.id}/600/800"

private enum class ItineraryTab { PUBLIC, MINE }

/**
 * Anteprima del percorso: un'icona per tappa nell'ordine in cui compaiono nella lista,
 * stessa codifica colore della timeline nel dettaglio (volo/hotel/attività). Il tipo si
 * deduce dai campi già presenti nel DTO (obbligatori lato server per hotel/attività, vedi
 * ItineraryService.validateResolvedCoherence) senza bisogno di richiamare il catalogo per
 * ogni componente di ogni card della lista.
 */
@Composable
private fun RoutePreview(items: List<FavoriteListItemDto>, maxIcons: Int = 6) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        items.take(maxIcons).forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(modifier = Modifier.width(8.dp).height(1.dp).background(Color.White.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.width(3.dp))
            }
            val (icon, tint) = when {
                item.checkIn != null && item.checkOut != null -> Icons.Filled.Hotel to CatalogColors.Gold
                item.activityDate != null -> Icons.Filled.Tour to CatalogColors.AccentLight
                else -> Icons.Filled.Flight to Color.White
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        }
        if (items.size > maxIcons) {
            Spacer(modifier = Modifier.width(4.dp))
            Text("+${items.size - maxIcons}", style = CatalogType.Meta, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryListScreen(
    viewModel: ItineraryViewModel,
    tokenManager: TokenManager,
    onNavigateToDetail: (id: Long, publicToken: String?) -> Unit
) {
    var tab by remember { mutableStateOf(ItineraryTab.PUBLIC) }
    var city by remember { mutableStateOf("") }
    var sortByLikes by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val feedState by viewModel.feedState.collectAsState()

    LaunchedEffect(Unit) {
        isLoggedIn = !tokenManager.tokenFlow.first().isNullOrBlank()
    }

    fun reload() {
        when (tab) {
            ItineraryTab.PUBLIC -> viewModel.loadFeed(city.trim().ifBlank { null }, if (sortByLikes) "likes" else "recent")
            ItineraryTab.MINE -> viewModel.loadMine()
        }
    }

    LaunchedEffect(tab, sortByLikes) { reload() }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CatalogColors.Surface,
            shape = CatalogShapes.Card,
            title = { Text("Nuovo itinerario", style = CatalogType.Section, color = CatalogColors.Ink) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("es. Weekend a Londra", style = CatalogType.Label, color = CatalogColors.InkSubtle) },
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
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        showCreateDialog = false
                        viewModel.createList(name.trim()) { created ->
                            if (created != null) {
                                tab = ItineraryTab.MINE
                                reload()
                            }
                        }
                    }
                }) { Text("Crea", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Annulla", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted) }
            }
        )
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("ITINERARI", style = CatalogType.Wordmark, color = CatalogColors.Ink) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        },
        floatingActionButton = {
            if (isLoggedIn) {
                FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = CatalogColors.AccentDark) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuovo itinerario", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            // La barra di navigazione flottante galleggia sopra il contenuto (non
            // riserva più spazio suo): senza questo margine extra, l'ultima card
            // resterebbe per metà nascosta sotto di lei.
            contentPadding = PaddingValues(bottom = 24.dp + LocalBottomNavBarHeight.current)
        ) {
            item(key = "tabs") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tab == ItineraryTab.PUBLIC,
                        onClick = { tab = ItineraryTab.PUBLIC },
                        label = { Text("Pubblici", style = CatalogType.Caption) },
                        leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                        shape = CatalogShapes.Chip
                    )
                    FilterChip(
                        selected = tab == ItineraryTab.MINE,
                        onClick = { tab = ItineraryTab.MINE },
                        label = { Text("Miei itinerari", style = CatalogType.Caption) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                        shape = CatalogShapes.Chip
                    )
                }
            }

            if (tab == ItineraryTab.PUBLIC) {
                item(key = "search") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            placeholder = { Text("Cerca per città…", style = CatalogType.Label) },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(18.dp)) },
                            trailingIcon = { if (city.isNotEmpty()) ClearFieldButton(onClear = { city = ""; reload() }) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatalogColors.Accent,
                                unfocusedBorderColor = CatalogColors.Hairline,
                                focusedContainerColor = CatalogColors.Surface,
                                unfocusedContainerColor = CatalogColors.Surface
                            ),
                            textStyle = CatalogType.Label,
                            singleLine = true,
                            shape = CatalogShapes.Field,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { reload() })
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = sortByLikes,
                                onClick = { sortByLikes = true },
                                label = { Text("Più piaciuti", style = CatalogType.Caption) },
                                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                                shape = CatalogShapes.Chip
                            )
                            FilterChip(
                                selected = !sortByLikes,
                                onClick = { sortByLikes = false },
                                label = { Text("Più recenti", style = CatalogType.Caption) },
                                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                                shape = CatalogShapes.Chip
                            )
                        }
                    }
                }
            }

            if (tab == ItineraryTab.MINE && !isLoggedIn) {
                item(key = "login-required") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Accedi per vedere i tuoi itinerari", style = CatalogType.Section, color = CatalogColors.Ink)
                    }
                }
            } else {
                when (val state = feedState) {
                    is ItineraryFeedState.Loading -> item(key = "loading") {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CatalogColors.AccentDark)
                        }
                    }
                    is ItineraryFeedState.Error -> item(key = "error") {
                        Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, style = CatalogType.Body, color = CatalogColors.InkMuted, textAlign = TextAlign.Center)
                        }
                    }
                    is ItineraryFeedState.Success -> {
                        if (state.lists.isEmpty()) {
                            item(key = "empty") {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Map, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (tab == ItineraryTab.PUBLIC) "Nessun itinerario pubblico" else "Non hai ancora nessun itinerario",
                                        style = CatalogType.Section, color = CatalogColors.Ink
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (tab == ItineraryTab.PUBLIC) "Prova un'altra città, o pubblica il primo tu."
                                        else "Creane uno con il pulsante +",
                                        style = CatalogType.Body, color = CatalogColors.InkMuted, textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(state.lists, key = { it.id }) { list ->
                                Box(modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter, vertical = CatalogSpacing.ListGap / 2)) {
                                    PhotoCard(
                                        imageUrl = itineraryImageUrl(list),
                                        eyebrow = list.city ?: (if (tab == ItineraryTab.MINE) list.visibility else "Itinerario"),
                                        price = "❤ ${list.likesCount}",
                                        title = list.name,
                                        // Con un link pubblico si vede il dettaglio anche da sloggati; senza
                                        // (solo nella tab "Miei itinerari") serve comunque essere autenticati.
                                        onClick = { onNavigateToDetail(list.id, list.publicToken) }
                                    ) {
                                        if (list.items.isNotEmpty()) {
                                            RoutePreview(items = list.items)
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        val componentsLabel = if (list.items.size == 1) "1 tappa" else "${list.items.size} tappe"
                                        PhotoMeta(icon = Icons.Filled.Route, text = componentsLabel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
