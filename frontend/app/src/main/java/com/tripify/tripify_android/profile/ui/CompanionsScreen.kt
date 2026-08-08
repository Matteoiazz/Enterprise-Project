package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel

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
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCompanions()
    }

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("COMPAGNI DI VIAGGIO", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp, color = TripifyDarkGreen) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripifyDarkGreen) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TripifyGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Compagno")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading && companions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TripifyGreen)
            } else if (companions.isEmpty()) {
                Text(
                    text = "Nessun compagno salvato.\nAggiungi i tuoi amici per velocizzare le prenotazioni!",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(companions) { companion ->
                        CompanionCard(
                            firstName = companion.firstName,
                            lastName = companion.lastName,
                            dob = companion.dateOfBirth
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCompanionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { nome, cognome, data ->
                    viewModel.addCompanion(nome, cognome, data)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CompanionCard(firstName: String, lastName: String, dob: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(TripifyGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TripifyGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "$firstName $lastName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TripifyDarkGreen)
                Text(text = "Nato il: $dob", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompanionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var dataNascita by remember { mutableStateOf("") } // Idealmente qui andrebbe un DatePicker!

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Compagno", fontWeight = FontWeight.Bold, color = TripifyDarkGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(value = cognome, onValueChange = { cognome = it }, label = { Text("Cognome") }, singleLine = true)
                OutlinedTextField(value = dataNascita, onValueChange = { dataNascita = it }, label = { Text("Data di Nascita (YYYY-MM-DD)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nome, cognome, dataNascita) },
                colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen)
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = Color.Gray) }
        }
    )
}