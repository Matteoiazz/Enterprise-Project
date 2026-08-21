package com.tripify.tripify_android.booking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// I tuoi colori
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onNavigateBack: () -> Unit
) {
    // Gestione dello stato delle Tab
    val tabs = listOf("Voli", "Hotel", "Escursioni")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "LE MIE PRENOTAZIONI",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
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
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // LA BARRA DELLE TABS
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = TripifyGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = TripifyGreen,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Medium,
                                color = if (selectedTabIndex == index) TripifyDarkGreen else Color.Gray
                            )
                        }
                    )
                }
            }

            // IL CONTENUTO DELLE TABS
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> { // VOLI
                        item {
                            BookingCard(
                                title = "Volo per Tokyo (HND)",
                                subtitle = "15 Ago 2026 • 14:30",
                                icon = Icons.Default.FlightTakeoff,
                                isLeader = true, // L'utente è il leader, può modificare
                                status = "Confermato"
                            )
                        }
                        item {
                            BookingCard(
                                title = "Volo per Londra (LHR)",
                                subtitle = "22 Set 2026 • 09:00",
                                icon = Icons.Default.FlightTakeoff,
                                isLeader = false, // L'utente è stato invitato
                                status = "Check-in aperto"
                            )
                        }
                    }
                    1 -> { // HOTEL
                        item {
                            BookingCard(
                                title = "Shinjuku Granbell Hotel",
                                subtitle = "16 Ago - 30 Ago 2026",
                                icon = Icons.Default.Hotel,
                                isLeader = true,
                                status = "Pagato"
                            )
                        }
                    }
                    2 -> { // ESCURSIONI
                        item {
                            BookingCard(
                                title = "Tour del Monte Fuji",
                                subtitle = "18 Ago 2026 • Intera Giornata",
                                icon = Icons.Default.Map,
                                isLeader = false,
                                status = "In attesa"
                            )
                        }
                    }
                }
            }
        }
    }
}

// UI Component: La singola card della prenotazione
@Composable
fun BookingCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isLeader: Boolean,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(TripifyGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = TripifyGreen)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
                        Text(text = subtitle, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }

                // Badge di Stato
                Box(
                    modifier = Modifier
                        .background(TripifyDarkGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripifyDarkGreen)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = SfondoPremium, thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // LOGICA LEADER vs PARTECIPANTE
            if (isLeader) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TripifyDarkGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TripifyGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dettagli")
                    }
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Gestisci", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TripifyDarkGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TripifyGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Visualizza Dettagli (Invitato)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}