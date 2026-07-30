package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onBookNow: (String) -> Unit // <-- IL GANCIO PER MATTIA!
) {
    val catalogItems by viewModel.catalogList.collectAsState()
    val item = catalogItems.find { it.id.toString() == itemId }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Elemento non trovato", fontSize = 18.sp, color = Color.Gray)
        }
        return
    }

    // SIMULAZIONE IMMAGINI MULTIPLE (Finché il backend non manda una List<String>)
    // Prendo l'URL base e aggiungo dei parametri random per forzare Picsum a darmi foto diverse
    val mockImageGallery = listOf(
        item.imageUrl,
        "${item.imageUrl}?random=1",
        "${item.imageUrl}?random=2"
    )

    val pagerState = rememberPagerState(pageCount = { mockImageGallery.size })
    val context = LocalContext.current

    Scaffold(
        containerColor = SfondoPremium,
        bottomBar = {
            // IL BOTTONE DI MATTIA
            Surface(
                color = Color.White,
                shadowElevation = 24.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Prezzo totale", color = Color.Gray, fontSize = 14.sp)
                        Text(item.price, color = TripifyDarkGreen, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                    Button(
                        onClick = { onBookNow(item.id.toString()) }, // Passiamo l'ID a Mattia
                        colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(56.dp).width(180.dp)
                    ) {
                        Text("PRENOTA", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 1. CAROSELLO IMMAGINI (SWIPE) ---
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = mockImageGallery[page],
                        contentDescription = "Galleria",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Sfumature per leggibilità
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 0f, endY = 1200f
                        )
                    )
                )

                // Pulsante Indietro
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                }

                // Puntini del Pager (Indicatori)
                Row(
                    Modifier.wrapContentHeight().fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(mockImageGallery.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) TripifyGreen else Color.White.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                        )
                    }
                }
            }

            // --- 2. CONTENUTO E DETTAGLI ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-20).dp) // Fa sovrapporre la card bianca sull'immagine
                    .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                // Tipo di item (Chip)
                Surface(
                    color = TripifyGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when(item) { is CatalogItem.Hotel -> "HOTEL"; is CatalogItem.Flight -> "VOLO"; is CatalogItem.Excursion -> "ESCURSIONE"; else -> "" },
                        color = TripifyGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = TripifyDarkGreen,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(24.dp))

                // SCHEDE SPECIFICHE
                when (item) {
                    is CatalogItem.Hotel -> {
                        DetailRow(icon = Icons.Filled.LocationOn, title = "Indirizzo", subtitle = item.address)
                        DetailRow(icon = Icons.Filled.Star, title = "Valutazione", subtitle = "${item.rating} Stelle - Eccellente", iconColor = Color(0xFFFFD700))
                        DetailRow(icon = Icons.Filled.Bed, title = "Tipologia", subtitle = item.roomType)
                        // IL TASTO MAPPA! Apre Google Maps nativamente
                        if (item.locationLat != null && item.locationLng != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("geo:${item.locationLat},${item.locationLng}?q=${item.locationLat},${item.locationLng}(${item.title})")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps") // Forza l'uso di Maps se installato
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Map, contentDescription = "Mappa", tint = TripifyGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vedi sulla mappa", color = TripifyDarkGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Cosa ti aspetta", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TripifyDarkGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Situato nel cuore pulsante della destinazione, questo hotel offre un mix perfetto di comfort moderno e fascino locale. Con Wi-Fi super veloce, servizio in camera h24 e una vista mozzafiato, è il luogo ideale per riposarsi dopo una giornata di esplorazione.", color = Color.Gray, fontSize = 16.sp, lineHeight = 24.sp)
                    }
                    is CatalogItem.Flight -> {
                        DetailRow(icon = Icons.Filled.FlightTakeoff, title = "Partenza", subtitle = "${item.departureAirport} - ${item.departureTime}")
                        DetailRow(icon = Icons.Filled.FlightLand, title = "Arrivo", subtitle = item.arrivalAirport)
                        DetailRow(icon = Icons.Filled.AirlineSeatReclineNormal, title = "Disponibilità", subtitle = "Rimangono solo ${item.availableSeats} posti", iconColor = if(item.availableSeats < 5) Color.Red else TripifyGreen)

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Regole del volo", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TripifyDarkGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Il biglietto base include un bagaglio a mano da riporre sotto il sedile. Modifiche al volo consentite fino a 24 ore prima della partenza. È richiesto il check-in online.", color = Color.Gray, fontSize = 16.sp, lineHeight = 24.sp)
                    }
                    is CatalogItem.Excursion -> {
                        DetailRow(icon = Icons.Filled.Schedule, title = "Durata", subtitle = item.duration)
                        DetailRow(icon = Icons.Filled.Tour, title = "Guida", subtitle = if(item.guideIncluded) "Guida esperta inclusa" else "Esplorazione libera")

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Descrizione dell'attività", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TripifyDarkGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preparati per un'esperienza indimenticabile. Attraversa paesaggi iconici, scopri la storia nascosta e scatta fotografie spettacolari. L'attrezzatura necessaria è fornita sul posto.", color = Color.Gray, fontSize = 16.sp, lineHeight = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Spazio vuoto per lo scrolling
            }
        }
    }
}

// Componente di utilità per le righe con icona
@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: Color = TripifyGreen) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(iconColor.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.Gray, fontSize = 14.sp)
            Text(subtitle, color = TripifyDarkGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}