package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.components.ClearFieldButton
import com.tripify.tripify_android.catalog.ui.components.PhotoCard
import com.tripify.tripify_android.catalog.ui.components.PhotoMeta
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryFeedState
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel

private fun itineraryImageUrl(list: FavoriteListDto): String =
    "https://picsum.photos/seed/itinerary${list.id}/600/800"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryListScreen(
    viewModel: ItineraryViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMyLists: () -> Unit
) {
    var city by remember { mutableStateOf("") }
    var sortByLikes by remember { mutableStateOf(true) }
    val feedState by viewModel.feedState.collectAsState()

    LaunchedEffect(sortByLikes) {
        viewModel.loadFeed(city.trim().ifBlank { null }, if (sortByLikes) "likes" else "recent")
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("ITINERARI", style = CatalogType.Wordmark, color = CatalogColors.Ink) },
                    actions = {
                        IconButton(onClick = onNavigateToMyLists) {
                            Icon(Icons.Filled.PersonOutline, contentDescription = "Le mie liste", tint = CatalogColors.AccentDark)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "search") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 14.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        placeholder = { Text("Cerca per città…", style = CatalogType.Label) },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { if (city.isNotEmpty()) ClearFieldButton(onClear = { city = ""; viewModel.loadFeed(null, if (sortByLikes) "likes" else "recent") }) },
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
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { viewModel.loadFeed(city.trim().ifBlank { null }, if (sortByLikes) "likes" else "recent") }
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sortByLikes,
                            onClick = { sortByLikes = true },
                            label = { Text("Più piaciuti", style = CatalogType.Caption) },
                            leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = androidx.compose.ui.graphics.Color.White, selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White),
                            shape = CatalogShapes.Chip
                        )
                        FilterChip(
                            selected = !sortByLikes,
                            onClick = { sortByLikes = false },
                            label = { Text("Più recenti", style = CatalogType.Caption) },
                            leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CatalogColors.AccentDark, selectedLabelColor = androidx.compose.ui.graphics.Color.White, selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White),
                            shape = CatalogShapes.Chip
                        )
                    }
                }
            }

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
                                Text("Nessun itinerario pubblico", style = CatalogType.Section, color = CatalogColors.Ink)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Prova un'altra città, o pubblica il primo tu.", style = CatalogType.Body, color = CatalogColors.InkMuted, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        items(state.lists, key = { it.id }) { list ->
                            Box(modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter, vertical = CatalogSpacing.ListGap / 2)) {
                                PhotoCard(
                                    imageUrl = itineraryImageUrl(list),
                                    eyebrow = list.city ?: "Itinerario",
                                    price = "❤ ${list.likesCount}",
                                    title = list.name,
                                    onClick = { onNavigateToDetail(list.id) }
                                ) {
                                    val componentsLabel = if (list.catalogItemIds.size == 1) "1 tappa" else "${list.catalogItemIds.size} tappe"
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
