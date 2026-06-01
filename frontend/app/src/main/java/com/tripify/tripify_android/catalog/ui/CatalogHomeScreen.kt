package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.TripMock

@Composable
fun HybridHomeScreen() {
    val viaggiInEvidenza = List(3) {
        TripMock(
            id = it,
            destinazione = if (it % 2 == 0) "NEW YORK" else "TOKYO",
            prezzo = "€ ${800 + it * 250}",
            imageUrl = "https://picsum.photos/seed/${it * 99}/600/800"
        )
    }

    Scaffold(
        containerColor = Color(0xFFF0F2F5) // Sfondo grigio chiarissimo
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. HEADER EMOZIONALE (Stile SiVola)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/travel/800/600",
                        contentDescription = "Sfondo ispirazionale",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradiente per far staccare la Card di ricerca
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                    startY = 200f
                                )
                            )
                    )
                    Text(
                        text = "Dove ti portiamo oggi?",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-40).dp)
                    )
                }
            }

            // 2. MOTORE DI RICERCA (Stile Ryanair)
            item {
                RyanairSearchForm(modifier = Modifier.offset(y = (-60).dp))
            }

            // 3. CATALOGO IN EVIDENZA (Stile SiVola)
            item {
                Text(
                    text = "Offerte in evidenza",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .offset(y = (-20).dp) // Aggiustiamo l'offset per compensare il form
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(viaggiInEvidenza) { viaggio ->
                Box(modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .offset(y = (-20).dp)
                ) {
                    SivolaTripCard(viaggio) // La funzione che abbiamo creato prima
                }
            }
        }
    }
}

@Composable
fun RyanairSearchForm(modifier: Modifier = Modifier) {
    var da by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Origine e Destinazione
            OutlinedTextField(
                value = da,
                onValueChange = { da = it },
                label = { Text("Da dove parti?") },
                leadingIcon = { Icon(Icons.Filled.FlightTakeoff, contentDescription = "Origine") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("Dove vuoi andare?") },
                leadingIcon = { Icon(Icons.Filled.FlightLand, contentDescription = "Destinazione") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Date e Passeggeri (Sulla stessa riga per risparmiare spazio)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* TODO: Apri DatePicker */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F2F5), contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Date")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { /* TODO: Apri selettore passeggeri */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F2F5), contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Passeggeri")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1 Pax", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pulsante di Ricerca Enorme
            Button(
                onClick = { /* TODO: Lancia la ricerca verso l'API */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("CERCA VOLI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

}
@Composable
fun SivolaTripCard(trip: TripMock) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // IMMAGINE DI SFONDO
            AsyncImage(
                model = trip.imageUrl,
                contentDescription = "Immagine di ${trip.destinazione}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // GRADIENTE SCURO
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 300f
                        )
                    )
            )

            // BADGE PREZZO IN ALTO A DESTRA
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = trip.prezzo,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // TESTO IN BASSO A SINISTRA
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = trip.destinazione.uppercase(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "8 Giorni • Turno unico",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}