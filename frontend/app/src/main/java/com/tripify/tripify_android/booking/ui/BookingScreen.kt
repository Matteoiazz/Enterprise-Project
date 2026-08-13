package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.component.BookingCard
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel

@Composable
fun BookingScreen(
    viewModel: BookingViewModel,
    userId: String // Ci serve per sapere di chi è lo storico
) {
    // 1. Ascoltiamo lo stato dal ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Variabili di stato per il Pop-up degli inviti (specifiche di questa schermata)
    var showDialog by remember { mutableStateOf(false) }
    var selectedBookingId by remember { mutableStateOf<Long?>(null) }
    var friendIdInput by remember { mutableStateOf("") }

    // 2. Appena si apre la schermata, chiediamo i dati al server
    LaunchedEffect(key1 = userId) {
        viewModel.fetchUserBookings(userId)
    }

    // 3. Disegniamo l'interfaccia in base allo stato
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is BookingState.Loading -> {
                // Mostriamo la rotellina di caricamento al centro
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is BookingState.Success -> {
                if (state.bookings.isEmpty()) {
                    Text(
                        text = "Non hai ancora effettuato nessun viaggio",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // Creiamo una lista scorrevole (come una RecyclerView)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.bookings) { booking ->
                            BookingCard(
                                booking = booking,
                                onInviteClick = { bookingId ->
                                    selectedBookingId = bookingId
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }

            is BookingState.Error -> {
                // Mostriamo il messaggio di errore in rosso
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }

    // 4. Disegniamo il Pop-up in sovrimpressione se serve
    if (showDialog && selectedBookingId != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                friendIdInput = ""
            },
            title = { Text("Invita un amico") },
            text = {
                Column {
                    Text("Inserisci l'ID o l'email dell'amico da invitare.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = friendIdInput,
                        onValueChange = { friendIdInput = it },
                        label = { Text("ID Amico") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.inviteFriend(
                            bookingId = selectedBookingId!!,
                            leaderId = userId,
                            friendId = friendIdInput,
                            onSuccess = {
                                showDialog = false
                                friendIdInput = ""
                            },
                            onError = { /* Gestione errore opzionale */ }
                        )
                    }
                ) {
                    Text("Invia")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        friendIdInput = ""
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }
}