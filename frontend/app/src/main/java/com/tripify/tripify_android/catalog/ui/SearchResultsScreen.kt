package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.components.*
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val catalogItems by viewModel.catalogList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val maxPrice by viewModel.maxPrice.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val departure by viewModel.departure.collectAsState()
    val amenities by viewModel.selectedAmenities.collectAsState()
    val directOnly by viewModel.directOnly.collectAsState()
    val guideOnly by viewModel.guideOnly.collectAsState()
    val hasSearched by viewModel.hasSearched.collectAsState()
    val recommendedItems by viewModel.recommendedItems.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        ComplexFilterBottomSheet(
            currentCategory = selectedCategory,
            initialMaxPrice = maxPrice,
            initialMinRating = minRating,
            initialDestination = destination,
            initialDeparture = departure,
            initialAmenities = amenities,
            initialDirectOnly = directOnly,
            initialGuideOnly = guideOnly,
            onDismiss = { showFilterSheet = false },
            onApplyFilters = { price, rating, selected, direct, guide, dest, dep ->
                viewModel.applyAdvancedFilters(price, rating, selected, direct, guide, dest, dep)
            }
        )
    }

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (searchQuery.isNotBlank()) "\"$searchQuery\"" else "Risultati",
                            style = CatalogType.CardTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified.let { 16.sp }),
                            color = CatalogColors.Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Filled.Tune, contentDescription = "Filtri", tint = TripifyDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )

                // Barra di ricerca modificabile in cima ai risultati, stile Booking
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Cerca...", style = CatalogType.Label) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TripifyGreen,
                            unfocusedBorderColor = CatalogColors.Hairline,
                            focusedContainerColor = CatalogColors.SurfaceMuted,
                            unfocusedContainerColor = CatalogColors.SurfaceMuted
                        ),
                        textStyle = CatalogType.Label,
                        singleLine = true,
                        shape = CatalogShapes.Field,
                        modifier = Modifier.weight(1f).height(48.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.searchNow() })
                    )
                }

                // Chip dei filtri attivi, tap per riaprire il bottom sheet
                QuickFilterChips(
                    maxPrice = maxPrice,
                    minRating = minRating,
                    onOpenFilters = { showFilterSheet = true },
                    modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == "Tutti") "Tutte le categorie" else selectedCategory,
                    style = CatalogType.LabelStrong,
                    color = CatalogColors.InkMuted
                )
                if (!isLoading) {
                    Text(
                        text = "${catalogItems.size} risultati",
                        style = CatalogType.LabelStrong,
                        color = CatalogColors.InkMuted
                    )
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TripifyGreen)
                    }
                }
                catalogItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nessun risultato trovato", style = CatalogType.Section, color = CatalogColors.Ink)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Prova ad allargare il budget o rimuovere qualche filtro",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(catalogItems) { item ->
                            Box(modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter, vertical = 6.dp)) {
                                val openDetail = {
                                    viewModel.onItemViewed(item)
                                    onNavigateToDetail(item.id.toString())
                                }
                                when (item) {
                                    is CatalogItem.Flight -> FlightResultCard(flight = item, onClick = openDetail)
                                    is CatalogItem.Hotel -> HotelResultCard(hotel = item, onClick = openDetail)
                                    is CatalogItem.Excursion -> ExcursionResultCard(excursion = item, onClick = openDetail)
                                }
                            }
                        }

                        // --- RACCOMANDAZIONI, ora qui: hanno senso solo dentro un contesto di ricerca già fatta ---
                        if (hasSearched && recommendedItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp, modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Altri viaggi che potrebbero interessarti",
                                    style = CatalogType.Section,
                                    color = CatalogColors.Ink,
                                    modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = CatalogSpacing.Gutter)
                                ) {
                                    items(recommendedItems) { item ->
                                        RecommendationCard(
                                            item = item,
                                            onClick = {
                                                viewModel.onItemViewed(item)
                                                onNavigateToDetail(item.id.toString())
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}