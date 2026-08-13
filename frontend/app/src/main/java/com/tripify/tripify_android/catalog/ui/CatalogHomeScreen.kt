package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val catalogItems by viewModel.catalogList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val maxPrice by viewModel.maxPrice.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text("TRIPIFY", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 3.sp, color = Ink)
                    },
                    actions = {
                        TextButton(onClick = { onNavigateToAuth() }) {
                            Text("ACCEDI", fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp, color = TripifyDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = Hairline, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // HEADER CON SFONDO E SEARCH BAR
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
                                    colors = listOf(TripifyDarkGreen.copy(alpha = 0.20f), Color.Transparent, TripifyDarkGreen.copy(alpha = 0.75f)),
                                    startY = 0f, endY = 650f
                                )
                            )
                        )

                        Column(
                            modifier = Modifier.align(Alignment.Center).offset(y = (-14).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Esplora il mondo", color = Color.White, fontSize = 26.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("VOLI · HOTEL · ESPERIENZE", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                        }
                    }

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
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
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
                                Box(modifier = Modifier.height(2.dp).width(if (selected) 18.dp else 0.dp).background(TripifyDarkGreen))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Hairline, thickness = 1.dp)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // CHIP FILTRI RAPIDI
            item {
                QuickFilterChips(
                    maxPrice = maxPrice,
                    minRating = minRating,
                    onOpenFilters = { showFilterSheet = true },
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // INTESTAZIONE RISULTATI
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (selectedCategory == "Tutti") "Esplora tutto" else selectedCategory, fontSize = 19.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Ink)
                    if (!isLoading && catalogItems.isNotEmpty()) {
                        Text(text = "${catalogItems.size} risultati", fontSize = 11.sp, color = InkMuted)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // LISTA ELEMENTI / STATO CARICAMENTO
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