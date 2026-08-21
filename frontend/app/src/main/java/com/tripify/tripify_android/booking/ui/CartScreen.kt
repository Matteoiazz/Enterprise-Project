package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripify.tripify_android.booking.components.CartItemCard
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.viewmodel.CartViewModel

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    userId: String // Ci serve per sapere di chi è il carrello
) {
    // 1. Ascoltiamo lo stato dal ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // 2. Appena si apre la schermata, chiediamo i dati al server
    LaunchedEffect(key1 = userId) {
        viewModel.fetchCart(userId)
    }

    // 3. Disegniamo l'interfaccia in base allo stato
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is CartState.Loading -> {
                // Mostriamo la rotellina di caricamento al centro
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is CartState.Success -> {
                if (state.cart.items.isEmpty()) {
                    Text(
                        text = "Il tuo carrello è vuoto",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // Creiamo una lista scorrevole (come una RecyclerView)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.cart.items) { item ->
                            CartItemCard(item = item)
                        }
                    }
                }
            }

            is CartState.Error -> {
                // Mostriamo il messaggio di errore in rosso
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }
}