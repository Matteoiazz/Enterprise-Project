package com.tripify.tripify_android.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.chat.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val currentUserId = viewModel.currentUserId

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ASSISTENZA & CHAT",
                                style = CatalogType.Overline,
                                color = CatalogColors.Ink
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Torna indietro",
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
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Lista dei messaggi
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = CatalogSpacing.Gutter),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages) { msg ->
                        val isMyMessage = msg.senderId == currentUserId

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                modifier = Modifier.widthIn(max = 300.dp),
                                color = if (isMyMessage)
                                    CatalogColors.Ink // Usa il colore scuro/primario della home per i tuoi messaggi
                                else
                                    CatalogColors.SurfaceMuted, // Superficie neutra per gli altri
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMyMessage) 16.dp else 4.dp,
                                    bottomEnd = if (isMyMessage) 4.dp else 16.dp
                                ),
                                border = if (!isMyMessage) androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    if (!isMyMessage) {
                                        Text(
                                            text = "Utente",
                                            style = CatalogType.Caption,
                                            color = CatalogColors.AccentDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Text(
                                        text = msg.content,
                                        style = CatalogType.Body,
                                        color = if (isMyMessage) Color.White else CatalogColors.Ink
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = "12:54",
                                        style = CatalogType.Caption,
                                        fontSize = 9.sp,
                                        color = if (isMyMessage) Color.White.copy(alpha = 0.7f) else CatalogColors.InkMuted,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Barra di scrittura inferiore coerente
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CatalogColors.Surface,
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CatalogSpacing.Gutter, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Scrivi un messaggio...", style = CatalogType.Body, color = CatalogColors.InkMuted) },
                            shape = CatalogShapes.Card,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatalogColors.AccentDark,
                                unfocusedBorderColor = CatalogColors.Hairline,
                                focusedContainerColor = CatalogColors.Background,
                                unfocusedContainerColor = CatalogColors.Background
                            )
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CatalogShapes.Badge)
                                .background(CatalogColors.AccentDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Invia",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}