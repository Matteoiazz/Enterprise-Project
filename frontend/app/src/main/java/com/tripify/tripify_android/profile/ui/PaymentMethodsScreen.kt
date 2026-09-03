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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreditCardOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.tripify.tripify_android.booking.component.CardNumberVisualTransformation
import com.tripify.tripify_android.booking.component.cardProviderOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import kotlinx.coroutines.flow.first
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
    var cardToDelete by remember { mutableStateOf<PaymentMethodDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.clearError()
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
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
        PullToRefreshList(
            onRefresh = {
                viewModel.loadPaymentMethods()
                viewModel.isLoading.first { !it }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (isLoading && methods.isEmpty()) {
                CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, start = CatalogSpacing.Gutter, end = CatalogSpacing.Gutter, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (methods.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyPaymentState()
                            }
                        }
                    } else {
                        items(methods, key = { it.id ?: it.hashCode() }) { card ->
                            CreditCardPremium(
                                card = card,
                                showDefaultControl = methods.size > 1,
                                onDeleteClick = { cardToDelete = card },
                                onSetDefaultClick = { card.id?.let { viewModel.setDefault(it) } }
                            )
                        }
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

        cardToDelete?.let { toDelete ->
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = { Text("Eliminare questa carta?", style = CatalogType.LabelStrong, color = CatalogColors.Ink) },
                text = { Text("La carta ${toDelete.cardProvider} che termina con ${toDelete.lastFourDigits ?: "0000"} verrà rimossa. L'operazione non è reversibile.", style = CatalogType.Body, color = CatalogColors.InkMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        toDelete.id?.let { id -> viewModel.deletePaymentMethod(id) }
                        cardToDelete = null
                    }) {
                        Text("ELIMINA", style = CatalogType.Button, color = CatalogColors.Alert)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToDelete = null }) {
                        Text("ANNULLA", style = CatalogType.Button, color = CatalogColors.InkMuted)
                    }
                },
                containerColor = CatalogColors.Surface
            )
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
    showDefaultControl: Boolean,
    onDeleteClick: () -> Unit,
    onSetDefaultClick: () -> Unit
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

            if (card.defaultCard) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(CatalogShapes.Badge)
                        .background(CatalogColors.Gold.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = CatalogColors.Gold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PREDEFINITA",
                        style = CatalogType.Overline,
                        color = CatalogColors.Surface
                    )
                }
            }

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

                if (showDefaultControl && !card.defaultCard) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CatalogColors.Surface.copy(alpha = 0.15f), CircleShape)
                            .clickable { onSetDefaultClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = "Imposta come predefinita",
                            tint = CatalogColors.Surface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

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
    var provider by remember { mutableStateOf(cardProviderOptions.first()) }
    var cardNumber by remember { mutableStateOf("") }
    var expiration by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }

    val expiryMonth = expiration.take(2).toIntOrNull()
    val expiryYear = expiration.drop(2).toIntOrNull()
    val expiryValid = expiration.length == 4 && expiryMonth != null && expiryMonth in 1..12 &&
        expiryYear != null && !YearMonth.of(2000 + expiryYear, expiryMonth).isBefore(YearMonth.now())

    val cardNumberError: String? = when {
        cardNumber.isBlank() -> if (submitAttempted) "Il numero della carta è obbligatorio" else null
        cardNumber.length != 16 -> "Il numero della carta deve avere 16 cifre"
        else -> null
    }
    val expiryError: String? = when {
        expiration.isBlank() -> if (submitAttempted) "La data di scadenza è obbligatoria" else null
        expiration.length < 4 -> null
        expiryMonth == null || expiryMonth !in 1..12 -> "Mese non valido"
        !expiryValid -> "La carta è scaduta"
        else -> null
    }
    val isFormValid = cardNumber.length == 16 && expiryValid

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
                        cardProviderOptions.forEach { selectionOption ->
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
                    isError = cardNumberError != null,
                    supportingText = cardNumberError,
                    onValueChange = { cardNumber = it.filter { char -> char.isDigit() }.take(16) },
                    visualTransformation = CardNumberVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                PaymentOutlinedTextField(
                    value = expiration,
                    label = "Scadenza (MM/AA)",
                    placeholder = "MM/AA",
                    isError = expiryError != null,
                    supportingText = expiryError,
                    onValueChange = { expiration = it.filter { char -> char.isDigit() }.take(4) },
                    visualTransformation = { text ->
                        val digits = text.text
                        val formatted = if (digits.length > 2) "${digits.take(2)}/${digits.drop(2)}" else digits
                        val mapping = object : OffsetMapping {
                            override fun originalToTransformed(offset: Int): Int = if (offset > 2) offset + 1 else offset
                            override fun transformedToOriginal(offset: Int): Int = if (offset > 3) offset - 1 else offset.coerceAtMost(2)
                        }
                        TransformedText(AnnotatedString(formatted), mapping)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        submitAttempted = true
                        if (isFormValid) onSave(provider, cardNumber, expiration)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogColors.AccentDark,
                        disabledContainerColor = CatalogColors.SurfaceMuted,
                        disabledContentColor = CatalogColors.InkSubtle
                    ),
                    shape = CatalogShapes.Pill
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
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
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
        visualTransformation = visualTransformation,
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
