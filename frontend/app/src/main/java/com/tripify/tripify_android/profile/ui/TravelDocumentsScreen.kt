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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripify.tripify_android.data.model.TravelDocumentDto
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.launch

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
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Documenti di Viaggio", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi Documento")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && documents.isEmpty()) {
                CircularProgressIndicator(color = TripifyGreen, modifier = Modifier.align(Alignment.Center))
            } else if (documents.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nessun documento salvato", color = Color.Gray, fontSize = 16.sp)
                    Text("Aggiungi il tuo passaporto o carta d'identità", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents) { doc ->
                        DocumentCardPremium(doc) {
                            doc.id?.let { viewModel.deleteDocument(it) }
                        }
                    }
                }
            }

            // L'overlay di caricamento mentre si elimina/salva
            if (isLoading && documents.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = TripifyGreen
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                AddDocumentForm(
                    onSave = { type, number, exp, country ->
                        viewModel.addDocument(type, number, exp, country) {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showBottomSheet = false
                            }
                        }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showBottomSheet = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DocumentCardPremium(doc: TravelDocumentDto, onDeleteClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(TripifyGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Badge, contentDescription = null, tint = TripifyDarkGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = doc.documentType, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = "N°: ${doc.documentNumber}", fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Scade: ${doc.expirationDate} (${doc.issuingCountry})", fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Elimina", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun AddDocumentForm(
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var docType by remember { mutableStateOf("") }
    var docNumber by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var issuingCountry by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
        Text("Nuovo Documento", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TripifyDarkGreen)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = docType, onValueChange = { docType = it },
            label = { Text("Tipo (Passaporto, Carta d'Identità...)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = docNumber, onValueChange = { docNumber = it },
            label = { Text("Numero Documento") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = expirationDate, onValueChange = { expirationDate = it },
            label = { Text("Scadenza (es. 2030-12-31)") },
            placeholder = { Text("AAAA-MM-GG") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = issuingCountry, onValueChange = { issuingCountry = it },
            label = { Text("Nazione Emittente (es. ITA)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text("Annulla", color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(docType, docNumber, expirationDate, issuingCountry) },
                colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen)
            ) {
                Text("Salva")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}