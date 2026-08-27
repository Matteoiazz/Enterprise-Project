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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import kotlinx.coroutines.launch
import java.time.YearMonth

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
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "METODI DI PAGAMENTO",
                            style = CatalogType.Wordmark,
                            color = CatalogColors.Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = CatalogColors.AccentDark,
                contentColor = CatalogColors.Surface,
                shape = CatalogShapes.Card,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi Carta", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && methods.isEmpty()) {
                CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
            } else if (methods.isEmpty()) {
                EmptyPaymentState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, start = CatalogSpacing.Gutter, end = CatalogSpacing.Gutter, bottom = 100.dp),
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
                    color = CatalogColors.Accent,
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
                .background(CatalogColors.SurfaceMuted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.CreditCardOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = CatalogColors.InkSubtle)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Nessuna carta", style = CatalogType.Section, color = CatalogColors.Ink)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aggiungi il tuo metodo di pagamento principale per prenotazioni veloci e sicure in 1 click.",
            style = CatalogType.Body,
            textAlign = TextAlign.Center,
            color = CatalogColors.InkMuted
        )
    }
}

@Composable
fun CreditCardPremium(
    card: PaymentMethodDto,
    onDeleteClick: () -> Unit
) {
    val brush = Brush.linearGradient(
        colors = listOf(CatalogColors.AccentDark, CatalogColors.Ink),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = CatalogShapes.Card,
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush).padding(24.dp)) {

            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = card.cardProvider.uppercase(),
                    style = CatalogType.CardTitle.copy(fontStyle = FontStyle.Italic, letterSpacing = 2.sp),
                    color = CatalogColors.Surface
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CatalogColors.Surface.copy(alpha = 0.15f), CircleShape)
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Elimina Carta",
                        tint = CatalogColors.Surface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterStart).offset(y = (-10).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp, 32.dp)
                        .background(CatalogColors.Gold.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = "Chip",
                        tint = CatalogColors.Ink,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = "Contactless",
                    tint = CatalogColors.Surface.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp).offset(y = 2.dp)
                )
            }

            Text(
                text = "•••• •••• •••• ${card.lastFourDigits ?: "0000"}",
                style = CatalogType.CardTitle.copy(letterSpacing = 2.sp),
                color = CatalogColors.Surface,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).offset(y = (-30).dp)
            )

            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("SCAD.", style = CatalogType.Overline, color = CatalogColors.Surface.copy(alpha = 0.6f))
                Text(card.expirationMonthYear, style = CatalogType.BodyStrong, color = CatalogColors.Surface)
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

    var isExpError by remember { mutableStateOf(false) }
    var expErrorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CatalogSpacing.Gutter, vertical = 24.dp)
    ) {
        LivePremiumCard(provider = provider, cardNumber = cardNumber, expiration = expiration)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CatalogShapes.Sheet,
            colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
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
                    PaymentOutlinedTextField(
                        value = provider,
                        label = "Circuito",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CatalogColors.Surface)
                    ) {
                        providerOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, style = CatalogType.LabelStrong, color = CatalogColors.Ink) },
                                onClick = {
                                    provider = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                PaymentOutlinedTextField(
                    value = cardNumber,
                    label = "Numero Carta",
                    placeholder = "0000 0000 0000 0000",
                    onValueChange = {
                        val digits = it.filter { char -> char.isDigit() }
                        if (digits.length <= 16) cardNumber = digits
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                PaymentOutlinedTextField(
                    value = expiration,
                    label = "Scadenza (MMAA)",
                    placeholder = "1228",
                    isError = isExpError,
                    supportingText = if (isExpError) expErrorMsg else null,
                    onValueChange = {
                        val digits = it.filter { char -> char.isDigit() }
                        if (digits.length <= 4) {
                            expiration = digits
                            if (digits.length == 4) {
                                val month = digits.substring(0, 2).toIntOrNull() ?: 0
                                val year = digits.substring(2, 4).toIntOrNull() ?: 0
                                if (month !in 1..12) {
                                    isExpError = true
                                    expErrorMsg = "Mese non valido"
                                } else {
                                    val currentYM = YearMonth.now()
                                    val cardYM = YearMonth.of(2000 + year, month)
                                    if (cardYM.isBefore(currentYM)) {
                                        isExpError = true
                                        expErrorMsg = "La carta è scaduta"
                                    } else {
                                        isExpError = false
                                        expErrorMsg = ""
                                    }
                                }
                            } else {
                                isExpError = false
                                expErrorMsg = ""
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val isFormValid = provider.isNotBlank() && cardNumber.length == 16 && expiration.length == 4 && !isExpError

                Button(
                    onClick = { onSave(provider, cardNumber, expiration) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogColors.AccentDark,
                        disabledContainerColor = CatalogColors.SurfaceMuted,
                        disabledContentColor = CatalogColors.InkSubtle
                    ),
                    shape = CatalogShapes.Pill,
                    enabled = isFormValid
                ) {
                    Text("AGGIUNGI CARTA", style = CatalogType.Button)
                }

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ANNULLA", style = CatalogType.Button, color = CatalogColors.InkMuted)
                }
            }
        }
    }
}

@Composable
fun PaymentOutlinedTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        enabled = enabled,
        label = { Text(label, style = CatalogType.Label) },
        placeholder = { Text(placeholder, style = CatalogType.Body, color = CatalogColors.InkSubtle) },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { { Text(it, style = CatalogType.Caption) } },
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CatalogColors.Surface,
            unfocusedContainerColor = CatalogColors.Surface,
            disabledContainerColor = CatalogColors.SurfaceMuted,
            errorContainerColor = CatalogColors.AlertSoft,
            focusedBorderColor = CatalogColors.Accent,
            unfocusedBorderColor = CatalogColors.Hairline,
            errorBorderColor = CatalogColors.Alert,
            focusedTextColor = CatalogColors.Ink,
            unfocusedTextColor = CatalogColors.Ink,
            disabledTextColor = CatalogColors.InkSubtle,
            focusedLabelColor = CatalogColors.Accent,
            unfocusedLabelColor = CatalogColors.InkMuted,
            cursorColor = CatalogColors.Accent
        ),
        textStyle = CatalogType.BodyStrong,
        shape = CatalogShapes.Field,
        singleLine = true
    )
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
        shape = CatalogShapes.Card,
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush).padding(24.dp)) {

            Text(
                text = if (provider.isEmpty()) "CIRCUITO" else provider.uppercase(),
                style = CatalogType.CardTitle.copy(fontStyle = FontStyle.Italic, letterSpacing = 2.sp),
                color = CatalogColors.Surface,
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
                style = CatalogType.CardTitle.copy(letterSpacing = 2.sp),
                color = CatalogColors.Surface,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).offset(y = (-30).dp)
            )

            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("SCAD.", style = CatalogType.Overline, color = CatalogColors.Surface.copy(alpha = 0.6f))
                Text(displayExp, style = CatalogType.BodyStrong, color = CatalogColors.Surface)
            }
        }
    }
}