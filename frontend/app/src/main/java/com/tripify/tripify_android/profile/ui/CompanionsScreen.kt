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
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

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
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "COMPAGNI DI VIAGGIO",
                            style = CatalogType.Wordmark,
                            color = TripifyDarkGreen
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripifyDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = TripifyDarkGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading && companions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TripifyDarkGreen)
            } else if (companions.isEmpty()) {
                EmptyCompanionsState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(companions) { companion ->
                        CompanionTicketCard(
                            firstName = companion.firstName,
                            lastName = companion.lastName,
                            dob = companion.dateOfBirth,
                            onDeleteClick = {
                                companion.id?.let { id -> viewModel.deleteCompanion(id) }
                            }
                        )
                    }
                }
            }

            if (isLoading && companions.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = TripifyGreen,
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
                .background(TripifyGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.GroupAdd, contentDescription = null, modifier = Modifier.size(50.dp), tint = TripifyDarkGreen)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Porta chi vuoi",
            style = CatalogType.Section,
            color = TripifyDarkGreen
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aggiungi amici o familiari (solo maggiorenni) per velocizzare le prenotazioni.",
            style = CatalogType.Body,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun CompanionTicketCard(firstName: String, lastName: String, dob: String, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(TripifyGreen))

            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                Text(
                    text = "NOME PASSEGGERO",
                    style = CatalogType.Overline,
                    color = Color.Gray
                )
                Text(
                    text = "$firstName $lastName".uppercase(),
                    style = CatalogType.CardTitle,
                    color = TripifyDarkGreen,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "DATA DI NASCITA",
                    style = CatalogType.Overline,
                    color = Color.Gray
                )
                Text(
                    text = dob,
                    style = CatalogType.BodyStrong,
                    color = TripifyDarkGreen
                )
            }

            Canvas(modifier = Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp)) {
                drawLine(
                    color = Color.LightGray,
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
                    tint = Color(0xFFD14343),
                    modifier = Modifier.size(28.dp)
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
                ) { Text("CONFERMA", style = CatalogType.Button, color = TripifyDarkGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("ANNULLA", style = CatalogType.Button, color = Color.Gray) }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = TripifyDarkGreen,
                    todayDateBorderColor = TripifyGreen,
                    todayContentColor = TripifyDarkGreen
                )
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(TripifyDarkGreen).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TRIPIFY TRAVEL ID",
                        style = CatalogType.Overline,
                        color = Color.White
                    )
                    Icon(Icons.Outlined.FlightTakeoff, contentDescription = null, tint = Color.White)
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumTextField(value = nome, label = "Nome", onValueChange = { nome = it })
                    PremiumTextField(value = cognome, label = "Cognome", onValueChange = { cognome = it })

                    Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        TextField(
                            value = dataNascita,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Data di Nascita", style = CatalogType.LabelStrong, color = if (isDateError) MaterialTheme.colorScheme.error else Color.Gray) },
                            placeholder = { Text("Seleziona dal calendario", style = CatalogType.Body, color = Color.LightGray) },
                            isError = isDateError,
                            supportingText = if (isDateError) { { Text(dateErrorMsg, style = CatalogType.Caption, color = MaterialTheme.colorScheme.error) } } else null,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Calendario",
                                    tint = if (isDateError) MaterialTheme.colorScheme.error else TripifyDarkGreen
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                disabledContainerColor = if (isDateError) Color(0xFFFFF0F0) else Color(0xFFF4F7F5),
                                disabledIndicatorColor = Color.Transparent,
                                disabledTextColor = if (isDateError) MaterialTheme.colorScheme.error else TripifyDarkGreen,
                                disabledLabelColor = if (isDateError) MaterialTheme.colorScheme.error else Color.Gray,
                                disabledTrailingIconColor = if (isDateError) MaterialTheme.colorScheme.error else TripifyDarkGreen,
                                errorContainerColor = Color(0xFFFFF0F0),
                                errorIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
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
                    color = Color.DarkGray,
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        containerColor = TripifyGreen,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isFormValid
                ) {
                    Text("EMETTI TRAVEL ID", style = CatalogType.Button)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("ANNULLA", style = CatalogType.Button, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))
                BarcodeVisual()
            }
        }
    }
}

@Composable
fun PremiumTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = CatalogType.LabelStrong, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF4F7F5),
            unfocusedContainerColor = Color(0xFFF4F7F5),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = TripifyDarkGreen,
            unfocusedTextColor = TripifyDarkGreen
        ),
        textStyle = CatalogType.BodyStrong,
        shape = RoundedCornerShape(12.dp),
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
            Box(modifier = Modifier.width(width.dp).fillMaxHeight().background(Color.Black))
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}