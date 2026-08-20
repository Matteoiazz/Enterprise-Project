package com.tripify.tripify_android.catalog.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.components.*
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen

private val CATEGORIES = listOf("Tutti", "Voli", "Hotel", "Attività")
private const val HERO_IMAGE = "https://picsum.photos/seed/epic_travel/1200/900"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CatalogViewModel,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onNavigateToSearchResults: () -> Unit = {}
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val catalogItems by viewModel.catalogList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
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
    var flightDeparture by rememberSaveable { mutableStateOf("") }
    var flightDestination by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text("TRIPIFY", style = CatalogType.Wordmark, color = CatalogColors.Ink)
                    },
                    actions = {
                        TextButton(onClick = onNavigateToAuth) {
                            Text(
                                text = "ACCEDI",
                                style = CatalogType.Overline,
                                color = TripifyDarkGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = CatalogColors.Surface
                    )
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "hero") {
                HeroHeader(
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = {
                        viewModel.searchNow()
                        onNavigateToSearchResults()
                    },
                    onOpenFilters = { showFilterSheet = true },
                    flightDeparture = flightDeparture,
                    onFlightDepartureChange = { flightDeparture = it },
                    flightDestination = flightDestination,
                    onFlightDestinationChange = { flightDestination = it },
                    onSearchFlights = {
                        viewModel.searchFlightRoute(flightDeparture, flightDestination)
                        onNavigateToSearchResults()
                    },
                    fetchSuggestions = { query -> viewModel.fetchCitySuggestions(query) }
                )
            }

            // --- SEZIONE RACCOMANDAZIONI  ---
            if (hasSearched && recommendedItems.isNotEmpty()) {
                item(key = "recommendations") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "In base alle tue ultime ricerche",
                            style = CatalogType.Section,
                            color = CatalogColors.Ink,
                            modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter, vertical = 12.dp)
                        )
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
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp, modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter))
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            item(key = "categories") {
                CategoryTabs(
                    selected = selectedCategory,
                    onSelect = viewModel::setCategory
                )
            }

            item(key = "quick_filters") {
                QuickFilterChips(
                    maxPrice = maxPrice,
                    minRating = minRating,
                    onOpenFilters = { showFilterSheet = true },
                    modifier = Modifier.padding(top = 14.dp, bottom = 16.dp)
                )
            }

            item(key = "section_header") {
                SectionHeader(
                    title = if (selectedCategory == "Tutti") "Esplora tutto" else selectedCategory,
                    resultCount = if (isLoading) null else catalogItems.size
                )
            }

            when {
                isLoading -> items(count = 3, key = { "skeleton_$it" }) {
                    CatalogCardSkeleton(
                        modifier = Modifier.padding(
                            horizontal = CatalogSpacing.Gutter,
                            vertical = CatalogSpacing.ListGap / 2
                        )
                    )
                }

                catalogItems.isEmpty() -> item(key = "empty") { EmptyState() }

                else -> items(items = catalogItems, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier.padding(
                            horizontal = CatalogSpacing.Gutter,
                            vertical = CatalogSpacing.ListGap / 2
                        )
                    ) {
                        val openDetail = {
                            viewModel.onItemViewed(item)
                            onNavigateToDetail(item.id.toString())
                        }
                        when (item) {
                            is CatalogItem.Flight -> FlightCard(flight = item, onClick = openDetail)
                            is CatalogItem.Hotel -> HotelCard(hotel = item, onClick = openDetail)
                            is CatalogItem.Excursion -> ExcursionCard(excursion = item, onClick = openDetail)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun HeroHeader(
    selectedCategory: String,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    flightDeparture: String,
    onFlightDepartureChange: (String) -> Unit,
    flightDestination: String,
    onFlightDestinationChange: (String) -> Unit,
    onSearchFlights: () -> Unit,
    fetchSuggestions: suspend (String) -> List<String>
) {
    val cardOverlap = 26.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            CatalogImage(
                model = HERO_IMAGE,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            PhotoScrim(modifier = Modifier.fillMaxSize(), startY = 0f, maxAlpha = 0.72f)

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-10).dp)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VOLI · HOTEL · ESPERIENZE",
                    style = CatalogType.Overline,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Esplora il mondo",
                    style = CatalogType.Hero,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(modifier = Modifier.offset(y = -cardOverlap)) {
            if (selectedCategory == "Voli") {
                FlightSearchForm(
                    departure = flightDeparture,
                    onDepartureChange = onFlightDepartureChange,
                    destination = flightDestination,
                    onDestinationChange = onFlightDestinationChange,
                    onSearch = onSearchFlights,
                    fetchSuggestions = fetchSuggestions
                )
            } else {
                UniversalSearchForm(
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onOpenFilters = onOpenFilters
                )
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = CatalogSpacing.Gutter),
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            CATEGORIES.forEach { category ->
                val isSelected = selected == category
                val underline by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 0.dp,
                    animationSpec = tween(220),
                    label = "tabUnderline"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(category) }
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = category,
                        style = if (isSelected) CatalogType.LabelStrong else CatalogType.Label,
                        color = if (isSelected) CatalogColors.Ink else CatalogColors.InkSubtle
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(underline)
                            .clip(CatalogShapes.Badge)
                            .background(TripifyDarkGreen)
                    )
                }
            }
        }
        HorizontalDivider(color = CatalogColors.Hairline)
    }
}

@Composable
private fun SectionHeader(title: String, resultCount: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CatalogSpacing.Gutter)
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text = title, style = CatalogType.Section, color = CatalogColors.Ink)
        if (resultCount != null && resultCount > 0) {
            Text(
                text = if (resultCount == 1) "1 risultato" else "$resultCount risultati",
                style = CatalogType.Caption,
                color = CatalogColors.InkSubtle,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CatalogShapes.Card)
                .background(CatalogColors.SurfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                tint = CatalogColors.InkSubtle,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nessun risultato",
            style = CatalogType.Section,
            color = CatalogColors.Ink
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Prova ad allargare il budget o a rimuovere qualche filtro.",
            style = CatalogType.Body,
            color = CatalogColors.InkMuted,
            textAlign = TextAlign.Center
        )
    }
}