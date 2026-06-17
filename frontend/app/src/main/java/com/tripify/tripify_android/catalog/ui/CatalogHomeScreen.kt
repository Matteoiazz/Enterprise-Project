package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

// Modelli e Componenti
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.components.UniversalSearchForm
import com.tripify.tripify_android.catalog.ui.components.HotelCard
import com.tripify.tripify_android.catalog.ui.components.ExcursionCard
import com.tripify.tripify_android.catalog.ui.components.FlightCard
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

// Colori di base
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CatalogViewModel = viewModel(),
    onNavigateToAuth: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {} // <-- NUOVO PARAMETRO
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var voceSelezionata by remember { mutableStateOf("Home") }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val catalogItems by viewModel.catalogList.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()

    val maxPrice by viewModel.maxPrice.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(TripifyGreen, TripifyDarkGreen)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "Avatar", modifier = Modifier.size(40.dp), tint = TripifyGreen)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Ospite", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text("Accedi per sincronizzare", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Vetrina Viaggi", fontWeight = FontWeight.Bold) },
                    selected = voceSelezionata == "Home",
                    onClick = { voceSelezionata = "Home"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Filled.Explore, contentDescription = "Esplora") },
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = TripifyGreen.copy(alpha = 0.1f), selectedIconColor = TripifyGreen, selectedTextColor = TripifyDarkGreen),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Le mie Prenotazioni") },
                    selected = voceSelezionata == "Prenotazioni",
                    onClick = { voceSelezionata = "Prenotazioni"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = "Prenotazioni") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = SfondoPremium,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("TRIPIFY", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 5.sp, color = TripifyDarkGreen)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TripifyDarkGreen)
                        }
                    },
                    actions = {
                        TextButton(onClick = { onNavigateToAuth() }) {
                            Text("ACCEDI", fontWeight = FontWeight.ExtraBold, color = TripifyGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                if (showFilterSheet) {
                    AdvancedFilterBottomSheet(
                        onDismiss = { showFilterSheet = false },
                        maxPrice = maxPrice,
                        onMaxPriceChange = { viewModel.updateMaxPrice(it) },
                        minRating = minRating,
                        onMinRatingChange = { viewModel.updateMinRating(it) }
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
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
                                            Color.Black.copy(alpha = 0.2f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        ),
                                        startY = 0f,
                                        endY = 1000f
                                    )
                                )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = (-40).dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Il mondo ti aspetta.",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Voli, Hotel ed Esperienze uniche.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        UniversalSearchForm(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onOpenFilters = { showFilterSheet = true },
                            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 80.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }

                item {
                    val categorie = listOf("Tutti", "Voli", "Hotel", "Escursioni")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categorie.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.setCategory(category) },
                                label = { Text(category, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TripifyGreen,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = if (selectedCategory == "Tutti") "Esplora tutto" else selectedCategory,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TripifyDarkGreen,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // <-- QUI INIETTIAMO L'ID SUL CLICK DELLE CARD
                items(catalogItems) { item ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        when (item) {
                            is CatalogItem.Flight -> FlightCard(
                                flight = item,
                                onClick = { onNavigateToDetail(item.id.toString()) }
                            )
                            is CatalogItem.Hotel -> HotelCard(
                                hotel = item,
                                onClick = { onNavigateToDetail(item.id.toString()) }
                            )
                            is CatalogItem.Excursion -> ExcursionCard(
                                excursion = item,
                                onClick = { onNavigateToDetail(item.id.toString()) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterBottomSheet(
    onDismiss: () -> Unit,
    maxPrice: Float,
    onMaxPriceChange: (Float) -> Unit,
    minRating: Int,
    onMinRatingChange: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Filtri Avanzati", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Prezzo Massimo: €${maxPrice.toInt()}", fontWeight = FontWeight.Bold)
            Slider(
                value = maxPrice,
                onValueChange = onMaxPriceChange,
                valueRange = 0f..1000f,
                steps = 20,
                colors = SliderDefaults.colors(thumbColor = TripifyGreen, activeTrackColor = TripifyGreen)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Categoria minima (Hotel)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 4, 5).forEach { stars ->
                    FilterChip(
                        selected = minRating == stars,
                        onClick = { onMinRatingChange(stars) },
                        label = { Text(if (stars == 0) "Tutte" else "$stars+ Stelle") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TripifyGreen.copy(alpha = 0.2f),
                            selectedLabelColor = TripifyDarkGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen)
            ) {
                Text("APPLICA E CHIUDI", fontWeight = FontWeight.Bold)
            }
        }
    }
}