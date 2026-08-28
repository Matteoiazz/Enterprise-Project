package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Storefront
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

    LaunchedEffect(Unit) {
        viewModel.loadOrganizers()
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
                    modifier = Modifier.padding(bottom = 24.dp)
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
            } else if (organizers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun organizzatore disponibile al momento.",
                            style = CatalogType.Body,
                            color = CatalogColors.InkMuted
                        )
                    }
                }
            } else {
                items(organizers, key = { it.email }) { org ->
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
                                    style = CatalogType.DetailTitle,
                                    color = CatalogColors.Ink
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = org.email,
                                    style = CatalogType.Caption,
                                    color = CatalogColors.InkMuted
                                )
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