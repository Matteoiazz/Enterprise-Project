package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.components.UniversalSearchForm
import com.tripify.tripify_android.catalog.ui.components.FlightSearchForm
import com.tripify.tripify_android.catalog.ui.components.QuickFilterChips
import com.tripify.tripify_android.catalog.ui.components.HotelCard
import com.tripify.tripify_android.catalog.ui.components.ExcursionCard
import com.tripify.tripify_android.catalog.ui.components.FlightCard
import com.tripify.tripify_android.catalog.ui.components.ComplexFilterBottomSheet
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.launch

private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF7A7A73)
private val Hairline = Color(0xFFE6E2D8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CatalogViewModel = viewModel(),
    onNavigateToAuth: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var voceSelezionata by remember { mutableStateOf("Home") }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val catalogItems by viewModel.catalogList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val maxPrice by viewModel.maxPrice.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    // Stato locale del form voli (specifico per la categoria "Voli")
    var flightDeparture by remember { mutableStateOf("") }
    var flightDestination by remember { mutableStateOf("") }

    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(280.dp)
            ) {
                // ... Header con il gradiente e l'avatar ...
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(colors = listOf(TripifyGreen, TripifyDarkGreen))
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    onNavigateToProfile()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "Avatar", modifier = Modifier.size(26.dp), tint = TripifyGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ospite", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Accedi per sincronizzare", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Il mio Profilo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    selected = voceSelezionata == "Profilo",
                    onClick = {
                        voceSelezionata = "Profilo"
                        scope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profilo", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Vetrina Viaggi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    selected = voceSelezionata == "Home",
                    onClick = { voceSelezionata = "Home"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Filled.Explore, contentDescription = "Esplora", modifier = Modifier.size(18.dp)) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = TripifyGreen.copy(alpha = 0.1f),
                        selectedIconColor = TripifyGreen,
                        selectedTextColor = TripifyDarkGreen
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // ECCO QUELLO CORRETTO ALLA FINE DEL MENU:
                NavigationDrawerItem(
                    label = { Text("Le mie Prenotazioni", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    selected = voceSelezionata == "Prenotazioni",
                    onClick = {
                        voceSelezionata = "Prenotazioni"
                        scope.launch { drawerState.close() }
                        onNavigateToBookings() // <-- Chiama la rotta corretta
                    },
                    icon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = "Prenotazioni", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = SfondoPremium,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text("TRIPIFY", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 3.sp, color = Ink)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Ink, modifier = Modifier.size(20.dp))
                            }
                        },
                        actions = {
                            TextButton(onClick = { onNavigateToAuth() }) {
                                Text("ACCEDI", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp, color = TripifyDarkGreen)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                    )
                    Divider(color = Hairline, thickness = 1.dp)
                }

                if (showFilterSheet) {
                    ComplexFilterBottomSheet(
                        currentCategory = selectedCategory,
                        onDismiss = { showFilterSheet = false },
                        onApplyFilters = { price, rating, amenities, direct, guide, destination, departure ->
                            viewModel.applyAdvancedFilters(price, rating, amenities, direct, guide, destination, departure)
                        }
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                // HEADER
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            AsyncImage(
                                model = "https://picsum.photos/seed/epic_travel/800/600",
                                contentDescription = "Sfondo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            TripifyDarkGreen.copy(alpha = 0.20f),
                                            Color.Transparent,
                                            TripifyDarkGreen.copy(alpha = 0.75f)
                                        ),
                                        startY = 0f,
                                        endY = 650f
                                    )
                                )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = (-14).dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Esplora il mondo",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VOLI · HOTEL · ESPERIENZE",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // --- FORM DI RICERCA: specifico per Voli, generico per le altre categorie ---
                        if (selectedCategory == "Voli") {
                            FlightSearchForm(
                                departure = flightDeparture,
                                onDepartureChange = { flightDeparture = it },
                                destination = flightDestination,
                                onDestinationChange = { flightDestination = it },
                                onSearch = { viewModel.searchFlightRoute(flightDeparture, flightDestination) },
                                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 28.dp)
                            )
                        } else {
                            UniversalSearchForm(
                                searchQuery = searchQuery,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                onOpenFilters = { showFilterSheet = true },
                                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                // BARRA DELLE CATEGORIE
                item {
                    val categorie = listOf("Tutti", "Voli", "Hotel", "Attività")
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            categorie.forEach { category ->
                                val selected = selectedCategory == category
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.setCategory(category) }
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Ink else InkMuted
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .width(if (selected) 18.dp else 0.dp)
                                            .background(TripifyDarkGreen)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Hairline, thickness = 1.dp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // CHIP FILTRI RAPIDI (budget / rating)
                item {
                    QuickFilterChips(
                        maxPrice = maxPrice,
                        minRating = minRating,
                        onOpenFilters = { showFilterSheet = true },
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategory == "Tutti") "Esplora tutto" else selectedCategory,
                            fontSize = 19.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        if (!isLoading && catalogItems.isNotEmpty()) {
                            Text(
                                text = "${catalogItems.size} risultati",
                                fontSize = 11.sp,
                                color = InkMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TripifyGreen, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    }
                } else if (catalogItems.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nessun viaggio trovato con questi filtri", color = InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    items(catalogItems) { item ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            when (item) {
                                is CatalogItem.Flight -> FlightCard(flight = item, onClick = { onNavigateToDetail(item.id.toString()) })
                                is CatalogItem.Hotel -> HotelCard(hotel = item, onClick = { onNavigateToDetail(item.id.toString()) })
                                is CatalogItem.Excursion -> ExcursionCard(excursion = item, onClick = { onNavigateToDetail(item.id.toString()) })
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}