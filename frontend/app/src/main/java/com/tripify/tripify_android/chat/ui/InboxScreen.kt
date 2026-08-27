package com.tripify.tripify_android.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.chat.viewmodel.InboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onChatRoomClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val chatRooms by viewModel.chatRooms.collectAsState()

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text("LE MIE CHAT", style = CatalogType.Wordmark, color = CatalogColors.Ink)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Indietro",
                                tint = CatalogColors.Ink
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                chatRooms.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CatalogShapes.Card)
                                .background(CatalogColors.SurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = CatalogColors.InkSubtle
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessuna chat attiva",
                            style = CatalogType.Section,
                            color = CatalogColors.Ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Le tue conversazioni con gli host appariranno qui.",
                            style = CatalogType.Body,
                            color = CatalogColors.InkMuted
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = CatalogSpacing.Gutter,
                            vertical = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatRooms) { room ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CatalogShapes.Card)
                                    .clickable { onChatRoomClick(room.id) },
                                color = CatalogColors.Surface,
                                shape = CatalogShapes.Card,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CatalogShapes.Badge)
                                            .background(CatalogColors.AccentDark.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = CatalogColors.AccentDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Chat con Organizzatore",
                                            style = CatalogType.LabelStrong,
                                            color = CatalogColors.Ink
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "ID: ${room.id.take(8)}...",
                                            style = CatalogType.Caption,
                                            color = CatalogColors.InkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}