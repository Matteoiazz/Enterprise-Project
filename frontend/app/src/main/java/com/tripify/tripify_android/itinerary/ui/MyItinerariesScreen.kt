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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.CreateListRequest
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.data.ItineraryRetrofit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Le mie liste": crea nuovi itinerari e li apre per aggiungerci componenti prima
 * di eventualmente pubblicarli. Usa direttamente ItineraryApi (nessuno stato
 * condiviso complesso serve qui, solo lista + creazione).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyItinerariesScreen(
    tokenManager: TokenManager,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val api = remember { ItineraryRetrofit.create(tokenManager) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var lists by remember { mutableStateOf<List<FavoriteListDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            isLoading = true
            try {
                val response = api.getMyLists()
                if (response.isSuccessful) lists = response.body() ?: emptyList()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Impossibile caricare le tue liste")
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

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuovo itinerario", style = CatalogType.Section, color = CatalogColors.Ink) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("es. Weekend a Londra") },
                    singleLine = true,
                    shape = CatalogShapes.Field,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        showCreateDialog = false
                        scope.launch {
                            try {
                                val response = api.createList(CreateListRequest(name.trim()))
                                if (response.isSuccessful) reload()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Impossibile creare la lista")
                            }
                        }
                    }
                }) { Text("Crea", color = CatalogColors.AccentDark) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Annulla", color = CatalogColors.InkMuted) }
            }
        )
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Le mie liste", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        },
        floatingActionButton = {
            if (isLoggedIn) {
                FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = CatalogColors.AccentDark) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuovo itinerario", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                !isLoggedIn -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Accedi per vedere le tue liste", style = CatalogType.Section, color = CatalogColors.Ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Devi effettuare l'accesso per creare e gestire i tuoi itinerari.", style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
                isLoading -> CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
                lists.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nessuna lista ancora", style = CatalogType.Section, color = CatalogColors.Ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Crea il tuo primo itinerario con il pulsante +", style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(lists, key = { it.id }) { list ->
                        Surface(
                            shape = CatalogShapes.Field,
                            color = CatalogColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline),
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetail(list.id) }
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
                                    val subtitle = if (list.catalogItemIds.size == 1) "1 tappa" else "${list.catalogItemIds.size} tappe"
                                    Text(subtitle, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CatalogColors.InkSubtle)
                            }
                        }
                    }
                }
            }
        }
    }
}
