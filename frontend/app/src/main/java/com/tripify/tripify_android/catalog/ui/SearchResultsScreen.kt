package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.components.*
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val catalogItems by viewModel.catalogList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isLastPage by viewModel.isLastPage.collectAsState()
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
    val errorMessage by viewModel.errorMessage.collectAsState()
    val totalResults by viewModel.totalResults.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(listState, isLastPage, isLoading) {
        snapshotFlow { listState.layoutInfo.let { it.visibleItemsInfo.lastOrNull()?.index to it.totalItemsCount } }
            .collect { (lastVisibleIndex, totalCount) ->
                if (!isLoading && !isLastPage && lastVisibleIndex != null && lastVisibleIndex >= totalCount - 4) {
                    viewModel.loadNextPage()
                }
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
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (searchQuery.isNotBlank()) "\"$searchQuery\"" else "Risultati",
                            style = CatalogType.TitleCompact,
                            color = CatalogColors.Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Filled.Tune, contentDescription = "Filtri", tint = CatalogColors.AccentDark)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Cerca...", style = CatalogType.Label) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { if (searchQuery.isNotEmpty()) ClearFieldButton(onClear = { viewModel.updateSearchQuery("") }) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CatalogColors.Accent,
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
                        text = if (totalResults == 1L) "1 risultato" else "$totalResults risultati",
                        style = CatalogType.LabelStrong,
                        color = CatalogColors.InkMuted
                    )
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CatalogColors.AccentDark)
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
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(catalogItems, key = { it.id }) { item ->
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

                        if (isLoadingMore) {
                            item(key = "loading_more") {
                                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        if (hasSearched && recommendedItems.isNotEmpty()) {
                            item(key = "recommendations") {
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
                                    items(recommendedItems, key = { it.id }) { item ->
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

                        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}
