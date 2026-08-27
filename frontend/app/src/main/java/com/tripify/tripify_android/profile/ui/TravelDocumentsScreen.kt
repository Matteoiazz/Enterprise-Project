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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripify.tripify_android.data.model.TravelDocumentDto
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelDocumentsScreen(
    viewModel: TravelDocumentsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val documents by viewModel.documents.collectAsState()
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
        viewModel.loadDocuments()
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "DOCUMENTI",
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
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi Documento", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && documents.isEmpty()) {
                CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.align(Alignment.Center))
            } else if (documents.isEmpty()) {
                EmptyDocumentsState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, start = CatalogSpacing.Gutter, end = CatalogSpacing.Gutter, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(documents, key = { it.id ?: it.hashCode() }) { doc ->
                        DocumentCardPremium(
                            doc = doc,
                            onDeleteClick = {
                                doc.id?.let { id -> viewModel.deleteDocument(id) }
                            }
                        )
                    }
                }
            }

            if (isLoading && documents.isNotEmpty()) {
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
                AddDocumentForm(
                    onSave = { type, number, exp, country ->
                        viewModel.addDocument(type, number, exp, country) {
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
fun EmptyDocumentsState(modifier: Modifier = Modifier) {
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
            Icon(Icons.Outlined.Badge, contentDescription = null, modifier = Modifier.size(40.dp), tint = CatalogColors.InkSubtle)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Nessun documento", style = CatalogType.Section, color = CatalogColors.Ink)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aggiungi il tuo passaporto o carta d'identità in corso di validità per averli sempre a portata di mano.",
            style = CatalogType.Body,
            textAlign = TextAlign.Center,
            color = CatalogColors.InkMuted
        )
    }
}

@Composable
fun DocumentCardPremium(doc: TravelDocumentDto, onDeleteClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = CatalogShapes.Card,
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sideColor = if (doc.documentType.contains("Passaporto", true)) CatalogColors.AccentDark else CatalogColors.Accent
            Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(sideColor))

            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Public, contentDescription = null, tint = sideColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = doc.documentType.uppercase(), style = CatalogType.Overline, color = CatalogColors.InkSubtle)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = doc.documentNumber, style = CatalogType.CardTitle, color = CatalogColors.Ink)

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Scade: ${doc.expirationDate}", style = CatalogType.BodyStrong, color = CatalogColors.InkMuted)

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(Icons.Filled.Flag, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = doc.issuingCountry, style = CatalogType.BodyStrong, color = CatalogColors.InkMuted)
                }
            }

            Box(
                modifier = Modifier.width(60.dp).fillMaxHeight().clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Elimina", tint = CatalogColors.Alert, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentForm(
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var docType by remember { mutableStateOf("Passaporto") }
    var docNumber by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var issuingCountry by remember { mutableStateOf("ITA") }

    val docTypes = listOf("Passaporto", "Carta d'Identità", "Patente di Guida")
    val countries = listOf("ITA", "USA", "GBR", "FRA", "ESP", "DEU")

    var expandedType by remember { mutableStateOf(false) }
    var expandedCountry by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var isDateError by remember { mutableStateOf(false) }
    var dateErrorMsg by remember { mutableStateOf("") }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            expirationDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                            if (localDate.isBefore(LocalDate.now())) {
                                isDateError = true
                                dateErrorMsg = "Il documento è scaduto!"
                            } else {
                                isDateError = false
                                dateErrorMsg = ""
                            }
                        }
                        showDatePicker = false
                    }
                ) { Text("CONFERMA", style = CatalogType.Button, color = CatalogColors.AccentDark) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("ANNULLA", style = CatalogType.Button, color = CatalogColors.InkMuted) }
            },
            colors = DatePickerDefaults.colors(containerColor = CatalogColors.Surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = CatalogColors.AccentDark,
                    todayDateBorderColor = CatalogColors.Accent,
                    todayContentColor = CatalogColors.AccentDark
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CatalogSpacing.Gutter, vertical = 24.dp)
    ) {
        LiveIDCard(type = docType, number = docNumber, expiration = expirationDate, country = issuingCountry)

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
                ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = !expandedType }) {
                    DocumentOutlinedTextField(
                        value = docType, label = "Tipo Documento", onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }, modifier = Modifier.background(CatalogColors.Surface)) {
                        docTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type, style = CatalogType.LabelStrong, color = CatalogColors.Ink) }, onClick = { docType = type; expandedType = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = expandedCountry, onExpandedChange = { expandedCountry = !expandedCountry }) {
                    DocumentOutlinedTextField(
                        value = issuingCountry, label = "Nazione Emittente", onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountry) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedCountry, onDismissRequest = { expandedCountry = false }, modifier = Modifier.background(CatalogColors.Surface)) {
                        countries.forEach { c ->
                            DropdownMenuItem(text = { Text(c, style = CatalogType.LabelStrong, color = CatalogColors.Ink) }, onClick = { issuingCountry = c; expandedCountry = false })
                        }
                    }
                }

                DocumentOutlinedTextField(
                    value = docNumber,
                    label = "Numero Documento",
                    onValueChange = { docNumber = it.uppercase() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    DocumentOutlinedTextField(
                        value = expirationDate,
                        label = "Data di Scadenza",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        isError = isDateError,
                        supportingText = if (isDateError) dateErrorMsg else null,
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isDateError) CatalogColors.Alert else CatalogColors.InkMuted
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isFormValid = docNumber.isNotBlank() && expirationDate.isNotBlank() && !isDateError
                Button(
                    onClick = { onSave(docType, docNumber, expirationDate, issuingCountry) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogColors.AccentDark,
                        disabledContainerColor = CatalogColors.SurfaceMuted,
                        disabledContentColor = CatalogColors.InkSubtle
                    ),
                    shape = CatalogShapes.Pill,
                    enabled = isFormValid
                ) {
                    Text("SALVA DOCUMENTO", style = CatalogType.Button)
                }

                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("ANNULLA", style = CatalogType.Button, color = CatalogColors.InkMuted)
                }
            }
        }
    }
}

@Composable
fun DocumentOutlinedTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
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
            disabledLabelColor = if (isError) CatalogColors.Alert else CatalogColors.InkMuted,
            cursorColor = CatalogColors.Accent
        ),
        textStyle = CatalogType.BodyStrong,
        shape = CatalogShapes.Field,
        singleLine = true
    )
}

@Composable
fun LiveIDCard(type: String, number: String, expiration: String, country: String) {
    val isPassport = type.contains("Passaporto", true)

    val gradientColors = if (isPassport) {
        listOf(CatalogColors.Ink, CatalogColors.AccentDark)
    } else {
        listOf(CatalogColors.AccentDark, CatalogColors.Accent)
    }

    val displayNum = number.ifBlank { "AB1234567" }
    val displayExp = expiration.ifBlank { "YYYY-MM-DD" }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        shape = CatalogShapes.Card,
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors)).padding(24.dp)) {
            Row(modifier = Modifier.align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Public, contentDescription = null, tint = CatalogColors.Surface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(country.uppercase(), style = CatalogType.CardTitle.copy(letterSpacing = 2.sp), color = CatalogColors.Surface.copy(alpha = 0.8f))
            }

            Text(
                text = type.uppercase(),
                style = CatalogType.Overline,
                color = CatalogColors.Surface,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = (-10).dp)
                    .size(40.dp)
                    .background(CatalogColors.Gold.copy(alpha = 0.2f), CatalogShapes.Badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric", tint = CatalogColors.Gold.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text("DOCUMENT NO.", style = CatalogType.Overline, color = CatalogColors.Surface.copy(alpha = 0.5f))
                Text(displayNum, style = CatalogType.Hero.copy(fontSize = 22.sp, letterSpacing = 2.sp), color = CatalogColors.Surface, maxLines = 1)
            }

            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("EXPIRY DATE", style = CatalogType.Overline, color = CatalogColors.Surface.copy(alpha = 0.5f))
                Text(displayExp, style = CatalogType.BodyStrong, color = CatalogColors.Surface)
            }
        }
    }
}