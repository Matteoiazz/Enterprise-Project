package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.component.BookingCard
import com.tripify.tripify_android.booking.model.BookingState
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel,
    cartViewModel: CartViewModel,
    catalogViewModel: CatalogViewModel,
    onNavigateToCart: () -> Unit = {},
    onAddPassengersClick: (bookingId: Long) -> Unit = {},
    onShowBoardingPassClick: (bookingId: Long) -> Unit = {}
) {
    // 1. Ascoltiamo lo stato dal ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val cartState by cartViewModel.uiState.collectAsState()
    val cartItemCount = (cartState as? CartState.Success)?.cart?.items?.sumOf { it.quantity } ?: 0

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Variabili di stato per il Pop-up degli inviti (specifiche di questa schermata)
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedBookingId by remember { mutableStateOf<Long?>(null) }
    var friendIdInput by remember { mutableStateOf("") }

    // Variabili di stato per la conferma di annullamento
    var showCancelDialog by remember { mutableStateOf(false) }
    var bookingToCancel by remember { mutableStateOf<Long?>(null) }

    // 2. Appena si apre la schermata, chiediamo i dati al server. L'utente non
    // serve più passarlo: il backend lo ricava dal JWT (vedi BookingApi).
    LaunchedEffect(Unit) {
        viewModel.fetchUserBookings()
        cartViewModel.fetchCart()
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Le mie prenotazioni", style = CatalogType.TitleCompact, color = CatalogColors.Ink)
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(badge = {
                            if (cartItemCount > 0) {
                                Badge(containerColor = CatalogColors.Alert) { Text("$cartItemCount") }
                            }
                        }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrello", tint = CatalogColors.AccentDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is BookingState.Loading -> {
                    CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
                }

                is BookingState.Success -> {
                    if (state.bookings.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Non hai ancora effettuato nessun viaggio", style = CatalogType.Section, color = CatalogColors.Ink)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Le prenotazioni che completi dal carrello appariranno qui",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(state.bookings, key = { it.id }) { booking ->
                                BookingCard(
                                    booking = booking,
                                    catalogViewModel = catalogViewModel,
                                    onInviteClick = { bookingId ->
                                        selectedBookingId = bookingId
                                        showInviteDialog = true
                                    },
                                    onCancelClick = { bookingId ->
                                        bookingToCancel = bookingId
                                        showCancelDialog = true
                                    },
                                    onAddPassengersClick = onAddPassengersClick,
                                    onShowBoardingPassClick = onShowBoardingPassClick
                                )
                            }
                        }
                    }
                }

                is BookingState.Error -> {
                    Text(
                        text = state.message,
                        color = CatalogColors.Alert,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }
        }
    }

    // Pop-up per invitare un amico
    if (showInviteDialog && selectedBookingId != null) {
        AlertDialog(
            onDismissRequest = {
                showInviteDialog = false
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
                            friendId = friendIdInput,
                            onSuccess = {
                                showInviteDialog = false
                                friendIdInput = ""
                            },
                            onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                        )
                    }
                ) {
                    Text("Invia")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showInviteDialog = false
                        friendIdInput = ""
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    // Pop-up di conferma annullamento prenotazione
    if (showCancelDialog && bookingToCancel != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Annullare la prenotazione?") },
            text = { Text("Se era già confermata e pagata, verrà avviato anche il rimborso.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelBooking(
                            bookingId = bookingToCancel!!,
                            onSuccess = { showCancelDialog = false },
                            onError = { message ->
                                showCancelDialog = false
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.Alert)
                ) {
                    Text("Sì, annulla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Torna indietro")
                }
            }
        )
    }
}
