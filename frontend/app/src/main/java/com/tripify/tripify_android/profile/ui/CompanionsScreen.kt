package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.profile.model.CompanionDto
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionsScreen(
    viewModel: CompanionsViewModel,
    onNavigateBack: () -> Unit
) {
    val companions by viewModel.companions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var companionToDelete by remember { mutableStateOf<CompanionDto?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadCompanions()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "COMPAGNI DI VIAGGIO",
                            style = CatalogType.Wordmark,
                            color = CatalogColors.Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = CatalogColors.AccentDark,
                contentColor = CatalogColors.Surface,
                shape = CatalogShapes.Card,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading && companions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CatalogColors.AccentDark)
            } else if (companions.isEmpty()) {
                EmptyCompanionsState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 24.dp, start = CatalogSpacing.Gutter, end = CatalogSpacing.Gutter, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(companions) { companion ->
                        CompanionTicketCard(
                            firstName = companion.firstName,
                            lastName = companion.lastName,
                            dob = companion.dateOfBirth,
                            onDeleteClick = { companionToDelete = companion }
                        )
                    }
                }
            }

            if (isLoading && companions.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = CatalogColors.Accent,
                    trackColor = Color.Transparent
                )
            }
        }

        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                dragHandle = null
            ) {
                BoardingPassFormSheet(
                    onDismiss = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                    },
                    onConfirm = { nome, cognome, data ->
                        viewModel.addCompanion(nome, cognome, data)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                    }
                )
            }
        }

        companionToDelete?.let { toDelete ->
            AlertDialog(
                onDismissRequest = { companionToDelete = null },
                title = { Text("Rimuovere ${toDelete.firstName} ${toDelete.lastName}?", style = CatalogType.LabelStrong, color = CatalogColors.Ink) },
                text = { Text("Il compagno di viaggio verrà rimosso dal tuo Travel ID. L'operazione non è reversibile.", style = CatalogType.Body, color = CatalogColors.InkMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        toDelete.id?.let { id -> viewModel.deleteCompanion(id) }
                        companionToDelete = null
                    }) {
                        Text("RIMUOVI", style = CatalogType.Button, color = CatalogColors.Alert)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { companionToDelete = null }) {
                        Text("ANNULLA", style = CatalogType.Button, color = CatalogColors.InkMuted)
                    }
                },
                containerColor = CatalogColors.Surface
            )
        }
    }
}

@Composable
fun EmptyCompanionsState(modifier: Modifier = Modifier) {
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
            Icon(Icons.Outlined.GroupAdd, contentDescription = null, modifier = Modifier.size(40.dp), tint = CatalogColors.InkSubtle)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Porta chi vuoi",
            style = CatalogType.Section,
            color = CatalogColors.Ink
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aggiungi amici o familiari (solo maggiorenni) per velocizzare le prenotazioni.",
            style = CatalogType.Body,
            textAlign = TextAlign.Center,
            color = CatalogColors.InkMuted
        )
    }
}

@Composable
fun CompanionTicketCard(firstName: String, lastName: String, dob: String, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CatalogShapes.Card,
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(CatalogColors.Accent))

            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                Text(
                    text = "NOME PASSEGGERO",
                    style = CatalogType.Overline,
                    color = CatalogColors.InkSubtle
                )
                Text(
                    text = "$firstName $lastName".uppercase(),
                    style = CatalogType.CardTitle,
                    color = CatalogColors.Ink,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "DATA DI NASCITA",
                    style = CatalogType.Overline,
                    color = CatalogColors.InkSubtle
                )
                Text(
                    text = dob,
                    style = CatalogType.BodyStrong,
                    color = CatalogColors.Ink
                )
            }

            Canvas(modifier = Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp)) {
                drawLine(
                    color = CatalogColors.Hairline,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            Box(
                modifier = Modifier.width(70.dp).fillMaxHeight().clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Elimina",
                    tint = CatalogColors.Alert,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardingPassFormSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var dataNascita by remember { mutableStateOf("") }

    var isDateError by remember { mutableStateOf(false) }
    var dateErrorMsg by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            dataNascita = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                            val age = Period.between(localDate, LocalDate.now()).years
                            if (localDate.isAfter(LocalDate.now())) {
                                isDateError = true
                                dateErrorMsg = "La data non può essere nel futuro"
                            } else if (age < 18) {
                                isDateError = true
                                dateErrorMsg = "Il compagno deve essere maggiorenne (+18)"
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = CatalogSpacing.Gutter, vertical = 24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(CatalogColors.AccentDark).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TRIPIFY TRAVEL ID",
                        style = CatalogType.Overline,
                        color = CatalogColors.Surface
                    )
                    Icon(Icons.Outlined.FlightTakeoff, contentDescription = null, tint = CatalogColors.Surface)
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CompanionOutlinedTextField(value = nome, label = "Nome", onValueChange = { nome = it })
                    CompanionOutlinedTextField(value = cognome, label = "Cognome", onValueChange = { cognome = it })

                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        CompanionOutlinedTextField(
                            value = dataNascita,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = "Data di Nascita",
                            isError = isDateError,
                            supportingText = if (isDateError) dateErrorMsg else null,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Calendario",
                                    tint = if (isDateError) CatalogColors.Alert else CatalogColors.InkMuted
                                )
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(20.dp).background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                drawLine(
                    color = CatalogColors.InkSubtle,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                    strokeWidth = 3f
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isFormValid = nome.isNotBlank() && cognome.isNotBlank() && dataNascita.isNotBlank() && !isDateError
                Button(
                    onClick = { onConfirm(nome, cognome, dataNascita) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogColors.AccentDark,
                        disabledContainerColor = CatalogColors.SurfaceMuted,
                        disabledContentColor = CatalogColors.InkSubtle
                    ),
                    shape = CatalogShapes.Pill,
                    enabled = isFormValid
                ) {
                    Text("EMETTI TRAVEL ID", style = CatalogType.Button)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("ANNULLA", style = CatalogType.Button, color = CatalogColors.InkMuted)
                }

                Spacer(modifier = Modifier.height(24.dp))
                BarcodeVisual()
            }
        }
    }
}

@Composable
fun CompanionOutlinedTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
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
        modifier = Modifier.fillMaxWidth(),
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
fun BarcodeVisual() {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pattern = listOf(2, 4, 1, 3, 2, 1, 5, 2, 1, 3, 4, 1, 2, 2, 1, 3, 1, 4, 2)
        pattern.forEach { width ->
            Box(modifier = Modifier.width(width.dp).fillMaxHeight().background(CatalogColors.Ink))
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}