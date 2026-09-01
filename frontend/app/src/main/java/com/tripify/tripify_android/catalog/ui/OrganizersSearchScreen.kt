package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizersSearchScreen(
    viewModel: ProfileViewModel,
    onNavigateToOrganizer: (String) -> Unit
) {
    val organizers = viewModel.organizersList
    val isLoading = viewModel.isLoadingOrganizers

    // Stato per la barra di ricerca
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadOrganizers()
    }

    // Filtriamo gli organizzatori in tempo reale
    val filteredOrganizers = organizers.filter { org ->
        val fullName = "${org.name.orEmpty()} ${org.surname.orEmpty()}".lowercase()
        val email = org.email.lowercase()
        val query = searchQuery.lowercase()
        fullName.contains(query) || email.contains(query)
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "ORGANIZZATORI",
                            style = CatalogType.Wordmark,
                            color = CatalogColors.Ink
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = CatalogColors.Surface
                    )
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(CatalogSpacing.Section)
        ) {
            item {
                Text(
                    text = "Esplora le migliori agenzie",
                    style = CatalogType.Hero,
                    color = CatalogColors.Ink,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = when {
                        isLoading -> "Caricamento organizzatori…"
                        organizers.isEmpty() -> "Nessun organizzatore disponibile"
                        searchQuery.isBlank() -> "${organizers.size} organizzatori su Tripify"
                        else -> "${filteredOrganizers.size} risultati"
                    },
                    style = CatalogType.Caption,
                    color = CatalogColors.InkMuted,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // BARRA DI RICERCA
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cerca per nome o email...", style = CatalogType.Body) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Cerca", tint = CatalogColors.InkSubtle)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Cancella", tint = CatalogColors.InkSubtle)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = CatalogShapes.Pill,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CatalogColors.Surface,
                        unfocusedContainerColor = CatalogColors.Surface,
                        focusedBorderColor = CatalogColors.Accent,
                        unfocusedBorderColor = CatalogColors.Hairline,
                        cursorColor = CatalogColors.Accent
                    )
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CatalogColors.AccentDark, strokeWidth = 3.dp)
                    }
                }
            } else if (filteredOrganizers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).background(CatalogColors.AccentSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isEmpty()) Icons.Filled.Storefront else Icons.Default.Search,
                                contentDescription = null,
                                tint = CatalogColors.AccentDark,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Nessun organizzatore" else "Nessun risultato",
                            style = CatalogType.Section,
                            color = CatalogColors.Ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Torna più tardi per scoprire le agenzie partner." else "Nessuna agenzia corrisponde a \"$searchQuery\".",
                            style = CatalogType.Body,
                            color = CatalogColors.InkMuted
                        )
                    }
                }
            } else {
                items(filteredOrganizers, key = { it.email }) { org ->
                    val displayName = "${org.name ?: ""} ${org.surname ?: ""}".trim().ifEmpty { "Organizzatore" }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clickable { onNavigateToOrganizer(org.email) },
                        shape = CatalogShapes.Card,
                        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!org.profilePictureUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = org.profilePictureUrl,
                                    contentDescription = displayName,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(CatalogColors.AccentSoft, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = CatalogColors.AccentDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = CatalogType.CardTitle,
                                    color = CatalogColors.Ink,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = org.email,
                                    style = CatalogType.Caption,
                                    color = CatalogColors.InkMuted,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(CatalogShapes.Badge)
                                        .background(CatalogColors.AccentSoft)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = CatalogColors.AccentDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PARTNER", style = CatalogType.Overline, color = CatalogColors.AccentDark)
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Dettagli",
                                tint = CatalogColors.InkSubtle,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}