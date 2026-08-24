package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreditCardOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods()
    }

    Scaffold(
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "METODI DI PAGAMENTO",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            color = TripifyDarkGreen
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = TripifyDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = TripifyDarkGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi Carta", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && methods.isEmpty()) {
                CircularProgressIndicator(color = TripifyDarkGreen, modifier = Modifier.align(Alignment.Center))
            } else if (methods.isEmpty()) {
                EmptyPaymentState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(methods) { card ->
                        CreditCardPremium(
                            card = card,
                            onDeleteClick = {
                                card.id?.let { id -> viewModel.deletePaymentMethod(id) }
                            }
                        )
                    }
                }
            }

            if (isLoading && methods.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = TripifyGreen,
                    trackColor = Color.Transparent
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                dragHandle = null
            ) {
                AddPaymentForm(
                    onSave = { provider, number, exp ->
                        val formattedExp = if (exp.length == 4) "${exp.substring(0, 2)}/${exp.substring(2, 4)}" else exp
                        viewModel.addPaymentMethod(provider, number, formattedExp) {
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
fun EmptyPaymentState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(TripifyGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CreditCardOff, contentDescription = null, modifier = Modifier.size(50.dp), tint = TripifyDarkGreen)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Nessuna carta", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aggiungi il tuo metodo di pagamento principale per prenotazioni veloci e sicure in 1 click.",
            textAlign = TextAlign.Center, color = Color.Gray, fontSize = 15.sp
        )
    }
}

@Composable
fun CreditCardPremium(
    card: PaymentMethodDto,
    onDeleteClick: () -> Unit
) {
    val brush = Brush.linearGradient(
        colors = listOf(TripifyDarkGreen, Color(0xFF0B3023)),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush).padding(24.dp)) {

            // TASTO ELIMINA + CIRCUITO IN ALTO A DESTRA
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = card.cardProvider.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )

                // Tondino elimina elegante
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Elimina Carta",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CHIP + CONTACTLESS
            Row(
                modifier = Modifier.align(Alignment.CenterStart).offset(y = (-10).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp, 32.dp)
                        .background(Color(0xFFFFD700).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = "Chip",
                        tint = TripifyDarkGreen,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = "Contactless",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp).offset(y = 2.dp)
                )
            }

            // NUMERO CARTA
            Text(
                text = "•••• •••• •••• ${card.lastFourDigits ?: "0000"}",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).offset(y = (-30).dp)
            )

            // SCADENZA
            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("SCAD.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Text(card.expirationMonthYear, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentForm(
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var provider by remember { mutableStateOf("Visa") }
    var cardNumber by remember { mutableStateOf("") }
    var expiration by remember { mutableStateOf("") }

    val providerOptions = listOf("Visa", "Mastercard", "American Express", "Maestro")
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        LivePremiumCard(provider = provider, cardNumber = cardNumber, expiration = expiration)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = provider,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Circuito", fontWeight = FontWeight.SemiBold, color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF4F7F5),
                            unfocusedContainerColor = Color(0xFFF4F7F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TripifyDarkGreen,
                            unfocusedTextColor = TripifyDarkGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        providerOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, color = TripifyDarkGreen, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    provider = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                TextField(
                    value = cardNumber,
                    onValueChange = {
                        val digits = it.filter { char -> char.isDigit() }
                        if (digits.length <= 16) cardNumber = digits
                    },
                    label = { Text("Numero Carta", fontWeight = FontWeight.SemiBold, color = Color.Gray) },
                    placeholder = { Text("0000 0000 0000 0000", color = Color.LightGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF4F7F5),
                        unfocusedContainerColor = Color(0xFFF4F7F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TripifyDarkGreen,
                        unfocusedTextColor = TripifyDarkGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                TextField(
                    value = expiration,
                    onValueChange = {
                        val digits = it.filter { char -> char.isDigit() }
                        if (digits.length <= 4) expiration = digits
                    },
                    label = { Text("Scadenza (MMAA)", fontWeight = FontWeight.SemiBold, color = Color.Gray) },
                    placeholder = { Text("1228", color = Color.LightGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF4F7F5),
                        unfocusedContainerColor = Color(0xFFF4F7F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TripifyDarkGreen,
                        unfocusedTextColor = TripifyDarkGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSave(provider, cardNumber, expiration) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                    shape = RoundedCornerShape(14.dp),
                    enabled = provider.isNotBlank() && cardNumber.length == 16 && expiration.length == 4
                ) {
                    Text("AGGIUNGI CARTA", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ANNULLA", color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun LivePremiumCard(provider: String, cardNumber: String, expiration: String) {
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF232526), Color(0xFF414345)),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val paddedNumber = cardNumber.padEnd(16, '•')
    val formattedNumber = paddedNumber.chunked(4).joinToString(" ")

    val displayExp = if (expiration.length >= 2) {
        expiration.substring(0, 2) + "/" + expiration.substring(2).padEnd(2, '•')
    } else {
        expiration.padEnd(2, '•') + "/••"
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush).padding(24.dp)) {

            Text(
                text = if (provider.isEmpty()) "CIRCUITO" else provider.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-10).dp)
                    .size(42.dp, 32.dp)
                    .background(Color(0xFFC0C0C0).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Memory, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.fillMaxSize())
            }

            Text(
                text = formattedNumber,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).offset(y = (-30).dp)
            )

            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("SCAD.", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Text(displayExp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
            }
        }
    }
}