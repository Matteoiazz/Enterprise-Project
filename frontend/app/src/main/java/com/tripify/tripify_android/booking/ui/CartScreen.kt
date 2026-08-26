package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.component.CartItemCard
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    catalogViewModel: CatalogViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToCheckout: () -> Unit = {}
) {
    // 1. Ascoltiamo lo stato dal ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // 2. Appena si apre la schermata, chiediamo i dati al server. L'utente non
    // serve più passarlo: il backend lo ricava dal JWT (vedi BookingApi).
    LaunchedEffect(Unit) {
        viewModel.fetchCart()
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Carrello", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        },
        bottomBar = {
            val state = uiState
            if (state is CartState.Success && state.cart.items.isNotEmpty()) {
                Surface(color = CatalogColors.Surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Totale", style = CatalogType.Body, color = CatalogColors.InkMuted)
                            Text(
                                text = "€${"%.2f".format(state.cart.totalAmount)}",
                                style = CatalogType.PriceLarge,
                                color = CatalogColors.AccentDark
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToCheckout,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Field
                        ) {
                            Text("Procedi al pagamento", style = CatalogType.Button)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is CartState.Loading -> {
                    CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
                }

                is CartState.Success -> {
                    if (state.cart.items.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Il tuo carrello è vuoto", style = CatalogType.Section, color = CatalogColors.Ink)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Aggiungi un volo, un hotel o un'attività dal catalogo",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(state.cart.items, key = { it.id }) { item ->
                                CartItemCard(item = item, catalogViewModel = catalogViewModel)
                            }
                        }
                    }
                }

                is CartState.Error -> {
                    Text(
                        text = state.message,
                        color = CatalogColors.Alert,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }
        }
    }
}
