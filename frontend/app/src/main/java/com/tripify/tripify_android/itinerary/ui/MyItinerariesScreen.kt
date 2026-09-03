package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.data.ItineraryRetrofit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Due modalità nella stessa schermata:
 * - "Le mie liste" (showSavedContent=false): solo le liste che possiedi, con FAB per
 *   crearne di nuove — usata quando devi poterci aggiungere/gestire componenti.
 * - "Salvati" (showSavedContent=true): tutto ciò che hai salvato in senso lato — le
 *   tue liste, quelle condivise, gli itinerari altrui a cui hai messo like, e i
 *   singoli elementi del catalogo a cui hai messo like — sola visualizzazione.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyItinerariesScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    catalogViewModel: CatalogViewModel? = null,
    showSavedContent: Boolean = false,
    onNavigateToCatalogItem: (Long) -> Unit = {}
) {
    val api = remember { ItineraryRetrofit.create(tokenManager) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var lists by remember { mutableStateOf<List<FavoriteListDto>>(emptyList()) }
    var likedCatalogItems by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(true) }
    // Distinto da "nessuna lista": senza, un errore di rete al primo caricamento
    // (lists resta vuota) mostrava lo stesso schermo di "non hai ancora nulla",
    // con solo una snackbar transitoria a segnalare che qualcosa e' andato storto.
    var loadError by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            isLoading = true
            loadError = false
            try {
                val response = if (showSavedContent) api.getSavedLists() else api.getMyLists()
                if (response.isSuccessful) lists = response.body() ?: emptyList()

                if (showSavedContent && catalogViewModel != null) {
                    val likedIdsResponse = api.getLikedCatalogItemIds()
                    if (likedIdsResponse.isSuccessful) {
                        val ids = likedIdsResponse.body() ?: emptyList()
                        likedCatalogItems = ids.mapNotNull { catalogViewModel.getOrFetchItem(it.toInt()) }
                    }
                }
            } catch (e: Exception) {
                loadError = true
                snackbarHostState.showSnackbar("Impossibile caricare i tuoi salvati")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            isLoggedIn = false
            isLoading = false
        } else {
            reload()
        }
    }

    // Rientrando su questa schermata dopo aver clonato/generato un itinerario da un
    // dettaglio aperto altrove (che naviga dritto al nuovo itinerario, senza passare
    // da qui), l'elenco non si aggiorna da solo: lo ricarica ogni volta che la
    // schermata torna in primo piano (stesso pattern di ItineraryListScreen).
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isLoggedIn) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (showSavedContent) "Salvati" else "Le mie liste", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val isEmpty = lists.isEmpty() && likedCatalogItems.isEmpty()
            when {
                !isLoggedIn -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Accedi per vedere i tuoi salvati", style = CatalogType.Section, color = CatalogColors.Ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Devi effettuare l'accesso per vedere e gestire i tuoi itinerari.", style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
                isLoading -> CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
                loadError -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Impossibile caricare", style = CatalogType.Section, color = CatalogColors.Ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Controlla la connessione e riprova.", style = CatalogType.Body, color = CatalogColors.InkMuted)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { reload() }) {
                        Text("Riprova", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                    }
                }
                isEmpty -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nessuna lista ancora", style = CatalogType.Section, color = CatalogColors.Ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Crea il tuo primo itinerario dalla tab Itinerari", style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (lists.isNotEmpty()) {
                        item(key = "header-lists") {
                            Text("ITINERARI", style = CatalogType.Overline, color = CatalogColors.InkMuted, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        items(lists, key = { "list-${it.id}" }) { list ->
                            ItineraryListRow(list = list, onClick = { onNavigateToDetail(list.id) })
                        }
                    }
                    if (likedCatalogItems.isNotEmpty()) {
                        item(key = "header-items") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("OGGETTI SALVATI", style = CatalogType.Overline, color = CatalogColors.InkMuted, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        items(likedCatalogItems, key = { "item-${it.id}" }) { item ->
                            SavedCatalogItemRow(item = item, onClick = { onNavigateToCatalogItem(item.id.toLong()) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryListRow(list: FavoriteListDto, onClick: () -> Unit) {
    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(CatalogColors.AccentSoft),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (list.visibility) {
                    "PUBLIC" -> Icons.Filled.Public
                    "SHARED" -> Icons.Filled.Group
                    else -> Icons.Filled.Lock
                }
                Icon(icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(list.name, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = if (list.items.size == 1) "1 tappa" else "${list.items.size} tappe"
                Text(subtitle, style = CatalogType.Caption, color = CatalogColors.InkMuted)
            }
            if (list.likedByMe) {
                Icon(Icons.Filled.Favorite, contentDescription = "Piaciuto", tint = CatalogColors.Alert, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CatalogColors.InkSubtle)
        }
    }
}

@Composable
private fun SavedCatalogItemRow(item: CatalogItem, onClick: () -> Unit) {
    Surface(
        shape = CatalogShapes.Field,
        color = CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CatalogShapes.Badge)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val (icon, label) = when (item) {
                    is CatalogItem.Flight -> Icons.Filled.Flight to "Volo"
                    is CatalogItem.Hotel -> Icons.Filled.Hotel to "Hotel"
                    is CatalogItem.Excursion -> Icons.Filled.Tour to "Attività"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(label, style = CatalogType.Overline, color = CatalogColors.InkMuted)
                }
                Text(item.title, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.Favorite, contentDescription = "Piaciuto", tint = CatalogColors.Alert, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CatalogColors.InkSubtle)
        }
    }
}
