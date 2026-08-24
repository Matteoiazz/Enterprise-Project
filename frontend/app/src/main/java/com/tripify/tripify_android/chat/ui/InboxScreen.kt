package com.tripify.tripify_android.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.chat.viewmodel.InboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onChatRoomClick: (Long) -> Unit, // Callback quando clicchi su una chat (passa il roomId)
    onBackClick: () -> Unit          // Per tornare alla home
) {
    val chatRooms by viewModel.chatRooms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le mie Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                chatRooms.isEmpty() -> {
                    // Stato vuoto: Nessuna chat mai creata
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nessuna chat mai creata",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    // Lista delle chat aperte con i vari organizzatori
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatRooms) { room ->
                            // Capiamo chi è l'interlocutore (se l'utente è il viaggiatore, l'altro è l'host e viceversa)
                            val otherUserId = if (room.travelerId == viewModel.currentUserId) room.hostId else room.travelerId

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChatRoomClick(room.id) },
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Chat con Organizzatore (ID: $otherUserId)",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Stanza #${room.id}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
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