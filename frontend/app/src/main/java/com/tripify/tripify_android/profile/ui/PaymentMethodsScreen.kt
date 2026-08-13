package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    viewModel: PaymentMethodsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val methods by viewModel.paymentMethods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Gestione Errori
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Auto-caricamento all'apertura (come nei documenti!)
    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods()
    }

    Scaffold(
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Metodi di Pagamento", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = TripifyGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.AddCard, contentDescription = "Aggiungi Carta")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && methods.isEmpty()) {
                CircularProgressIndicator(color = TripifyGreen, modifier = Modifier.align(Alignment.Center))
            } else if (methods.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CreditCardOff, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nessuna carta salvata", color = Color.Gray, fontSize = 16.sp)
                    Text("Aggiungi un metodo di pagamento per le prenotazioni", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(methods) { card ->
                        CreditCardPremium(card)
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                AddPaymentForm(
                    onSave = { provider, number, exp ->
                        viewModel.addPaymentMethod(provider, number, exp) {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                        }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                    }
                )
            }
        }
    }
}

@Composable
fun CreditCardPremium(card: PaymentMethodDto) {
    val brush = Brush.linearGradient(listOf(TripifyDarkGreen, TripifyGreen))

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(180.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush).padding(24.dp)) {
            // Circuito (Visa, Mastercard ecc)
            Text(
                text = card.cardProvider.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Icona Chip
            Icon(
                Icons.Filled.Memory,
                contentDescription = "Chip",
                tint = Color(0xFFFFD700), // Oro
                modifier = Modifier.size(36.dp).align(Alignment.CenterStart).offset(y = (-20).dp)
            )

            // Numero Carta (Mascherato)
            Text(
                text = "****  ****  ****  ${card.lastFourDigits ?: "0000"}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.BottomStart).offset(y = (-30).dp)
            )

            // Scadenza
            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("SCADENZA", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(card.expirationMonthYear, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun AddPaymentForm(
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var provider by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiration by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
        Text("Nuova Carta", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TripifyDarkGreen)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = provider, onValueChange = { provider = it },
            label = { Text("Circuito (es. Visa, Mastercard)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = cardNumber, onValueChange = { cardNumber = it },
            label = { Text("Numero Carta (16 cifre)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = expiration, onValueChange = { expiration = it },
            label = { Text("Scadenza (MM/AA)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Annulla", color = Color.Gray) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(provider, cardNumber, expiration) },
                colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen)
            ) { Text("Salva Carta") }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}