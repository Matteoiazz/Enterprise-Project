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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.component.CartItemCard
import com.tripify.tripify_android.booking.component.CurrencyPicker
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.util.convertCartAmount
import com.tripify.tripify_android.booking.util.currencySymbol
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    catalogViewModel: CatalogViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToCheckout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()

    // Stessa valuta scelta in Impostazioni > Valuta (e usata dal catalogo per
    // mostrare i prezzi): qui serve a convertire il totale quando il carrello
    // contiene articoli in valute diverse tra loro, invece di sommarli come se
    // fossero tutti nella stessa valuta. La scelta fatta qui resta valida
    // anche in CheckoutScreen, perché è lo stesso storage condiviso.
    val displayCurrency by rememberCatalogCurrency()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

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
                val selectedItems = state.cart.items.filter { it.id in selectedItemIds }
                // Ogni articolo si converte dalla SUA valuta originale (item.currency)
                // a quella scelta per la visualizzazione, non si sommano più i
                // priceAtAdded grezzi: altrimenti un carrello con un volo in USD e
                // un hotel in EUR darebbe un numero senza significato.
                val selectedTotal = selectedItems.sumOf {
                    convertCartAmount(it.priceAtAdded, it.currency, displayCurrency) * it.quantity
                }

                Surface(color = CatalogColors.Surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        CurrencyPicker(
                            selected = displayCurrency,
                            onSelect = { currency -> scope.launch { tokenManager.setCurrency(currency) } }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Totale (${selectedItems.size} articoli)",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted
                            )
                            Text(
                                text = "${currencySymbol(displayCurrency)} ${"%.2f".format(selectedTotal)}",
                                style = CatalogType.PriceLarge,
                                color = CatalogColors.AccentDark
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToCheckout,
                            enabled = selectedItems.isNotEmpty(),
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
                                CartItemCard(
                                    item = item,
                                    catalogViewModel = catalogViewModel,
                                    selected = item.id in selectedItemIds,
                                    onToggleSelected = { viewModel.toggleItemSelection(item.id) },
                                    onRemoveClick = { viewModel.removeItem(item.id) }
                                )
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
