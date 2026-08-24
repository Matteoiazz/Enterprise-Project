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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// I tuoi colori
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionsScreen(
    viewModel: CompanionsViewModel,
    onNavigateBack: () -> Unit
) {
    val companions by viewModel.companions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadCompanions()
    }

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "COMPAGNI DI VIAGGIO",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
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
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Compagno", modifier = Modifier.size(28.dp))
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
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showAddSheet = false
                        }
                    },
                    onConfirm = { nome, cognome, data ->
                        viewModel.addCompanion(nome, cognome, data)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showAddSheet = false
                        }
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
        Text("Nessun compagno salvato", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aggiungi i tuoi amici o familiari per velocizzare le prenotazioni dei tuoi prossimi voli.",
            textAlign = TextAlign.Center, color = Color.Gray, fontSize = 15.sp
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

            Column(
                modifier = Modifier.weight(1f).padding(20.dp)
            ) {
                Text(text = "NOME PASSEGGERO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Text(text = "$firstName $lastName".uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "DATA DI NASCITA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Text(text = dob, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TripifyDarkGreen)
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

    // Gestione visualizzazione DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            dataNascita = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("CONFERMA", color = TripifyDarkGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("ANNULLA", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = TripifyDarkGreen,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = TripifyGreen,
                    todayContentColor = TripifyDarkGreen
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // --- PARTE SUPERIORE BIGLIETTO ---
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
                    Text("TRIPIFY TRAVEL ID", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp)
                    Icon(Icons.Outlined.FlightTakeoff, contentDescription = null, tint = Color.White)
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumTextField(value = nome, label = "Nome", onValueChange = { nome = it })
                    PremiumTextField(value = cognome, label = "Cognome", onValueChange = { cognome = it })

                    // Campo Data cliccabile che apre il calendario
                    PremiumDatePickerField(
                        value = dataNascita,
                        label = "Data di Nascita",
                        placeholder = "Seleziona dal calendario",
                        onClick = { showDatePicker = true }
                    )
                }
            }
        }

        // --- LINEA TRATTEGGIATA (EFFETTO STRAPPO) ---
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

        // --- PARTE INFERIORE BIGLIETTO (Azioni e Codice a barre) ---
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
                Button(
                    onClick = { onConfirm(nome, cognome, dataNascita) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                    shape = RoundedCornerShape(14.dp),
                    enabled = nome.isNotBlank() && cognome.isNotBlank() && dataNascita.isNotBlank()
                ) {
                    Text("EMETTI TRAVEL ID", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("ANNULLA", color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
        label = { Text(label, fontWeight = FontWeight.SemiBold, color = Color.Gray) },
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
}

@Composable
fun PremiumDatePickerField(
    value: String,
    label: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, fontWeight = FontWeight.SemiBold, color = Color.Gray) },
            placeholder = { Text(placeholder, color = Color.LightGray) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Seleziona Data",
                    tint = TripifyDarkGreen
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color(0xFFF4F7F5),
                disabledIndicatorColor = Color.Transparent,
                disabledTextColor = TripifyDarkGreen,
                disabledLabelColor = Color.Gray,
                disabledPlaceholderColor = Color.LightGray,
                disabledTrailingIconColor = TripifyDarkGreen
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
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
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .fillMaxHeight()
                    .background(Color.Black)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}