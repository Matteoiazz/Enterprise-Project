package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.booking.component.CartItemCard
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.booking.model.PaymentState
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

// Raggruppa il numero carta a blocchi di 4 solo per la visualizzazione: il valore
// memorizzato resta la sequenza di sole cifre inviata al backend.
private class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = digits.chunked(4).joinToString(" ")
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + offset / 4
            override fun transformedToOriginal(offset: Int): Int = offset - (offset / 5).coerceAtMost(digits.length / 4)
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// Riepilogo ordine + pagamento simulato (vedi PaymentService lato booking-service:
// non è un vero PSP, basta una card "plausibile" lunga almeno 12 cifre). Scadenza,
// CVV e intestatario sono validati solo lato client: il mock non li richiede, ma un
// form di pagamento senza questi controlli non sembra affidabile a un utente vero.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CartViewModel,
    catalogViewModel: CatalogViewModel,
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: (bookingId: Long) -> Unit = {}
) {
    val cartState by viewModel.uiState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    var cardNumber by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val expiryMonth = expiry.take(2).toIntOrNull()
    val expiryValid = expiry.length == 4 && expiryMonth != null && expiryMonth in 1..12
    val formValid = cardNumber.length in 12..19 && cardholderName.isNotBlank() && expiryValid && cvv.length in 3..4

    LaunchedEffect(Unit) {
        viewModel.fetchCart()
        viewModel.resetPaymentState()
    }

    LaunchedEffect(paymentState) {
        val state = paymentState
        if (state is PaymentState.Success) {
            onPaymentSuccess(state.bookingId)
        }
    }

    val cart = (cartState as? CartState.Success)?.cart

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pagamento", style = CatalogType.TitleCompact, color = CatalogColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
            )
        },
        bottomBar = {
            if (cart != null && cart.items.isNotEmpty()) {
                Surface(color = CatalogColors.Surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale da pagare", style = CatalogType.Body, color = CatalogColors.InkMuted)
                            Text(
                                text = "€${"%.2f".format(cart.totalAmount)}",
                                style = CatalogType.PriceLarge,
                                color = CatalogColors.AccentDark
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.payCart(cardNumber) },
                            enabled = formValid && paymentState !is PaymentState.Processing,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Field
                        ) {
                            if (paymentState is PaymentState.Processing) {
                                CircularProgressIndicator(
                                    color = CatalogColors.Surface,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Paga €${"%.2f".format(cart.totalAmount)}", style = CatalogType.Button)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (cart == null || cart.items.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (cartState is CartState.Loading) {
                    CircularProgressIndicator(color = CatalogColors.AccentDark)
                } else {
                    Text("Il carrello è vuoto", style = CatalogType.Body, color = CatalogColors.InkMuted)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                item {
                    Text(
                        text = "Riepilogo ordine",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                items(cart.items, key = { it.id }) { item ->
                    CartItemCard(item = item, catalogViewModel = catalogViewModel)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dati di pagamento",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cardholderName,
                        onValueChange = { cardholderName = it },
                        label = { Text("Intestatario carta") },
                        placeholder = { Text("Es. Mario Rossi") },
                        singleLine = true,
                        shape = CatalogShapes.Field,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CatalogColors.Accent,
                            unfocusedBorderColor = CatalogColors.Hairline
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { value -> cardNumber = value.filter { it.isDigit() }.take(19) },
                        label = { Text("Numero carta") },
                        placeholder = { Text("Es. 4111 1111 1111 1111") },
                        leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null, tint = CatalogColors.Accent) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CardNumberVisualTransformation(),
                        singleLine = true,
                        shape = CatalogShapes.Field,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CatalogColors.Accent,
                            unfocusedBorderColor = CatalogColors.Hairline
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { value -> expiry = value.filter { it.isDigit() }.take(4) },
                            label = { Text("Scadenza") },
                            placeholder = { Text("MM/AA") },
                            visualTransformation = { text ->
                                val digits = text.text
                                val formatted = if (digits.length > 2) "${digits.take(2)}/${digits.drop(2)}" else digits
                                val offsetMapping = object : OffsetMapping {
                                    override fun originalToTransformed(offset: Int): Int = if (offset > 2) offset + 1 else offset
                                    override fun transformedToOriginal(offset: Int): Int = if (offset > 3) offset - 1 else offset.coerceAtMost(2)
                                }
                                TransformedText(AnnotatedString(formatted), offsetMapping)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = CatalogShapes.Field,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatalogColors.Accent,
                                unfocusedBorderColor = CatalogColors.Hairline
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { value -> cvv = value.filter { it.isDigit() }.take(4) },
                            label = { Text("CVV") },
                            placeholder = { Text("123") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = CatalogShapes.Field,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatalogColors.Accent,
                                unfocusedBorderColor = CatalogColors.Hairline
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val paymentError = paymentState
                    if (paymentError is PaymentState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = paymentError.message,
                            style = CatalogType.Body,
                            color = CatalogColors.Alert,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
