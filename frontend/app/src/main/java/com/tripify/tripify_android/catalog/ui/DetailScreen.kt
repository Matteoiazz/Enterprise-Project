package com.tripify.tripify_android.catalog.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.launch

private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF7A7A73)
private val Hairline = Color(0xFFE6E2D8)
private val CardSurface = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onBookNow: (String) -> Unit
) {
    val catalogItems by viewModel.catalogList.collectAsState()
    val item = catalogItems.find { it.id.toString() == itemId }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Elemento non trovato", fontSize = 14.sp, color = InkMuted)
        }
        return
    }

    val imageList = item.imageUrls.ifEmpty { listOf(item.imageUrl) }
    val pagerState = rememberPagerState(pageCount = { imageList.size })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val imageHeight = 300.dp

    val overviewText = remember(item) {
        when (item) {
            is CatalogItem.Flight -> buildString {
                append("Volo da ${item.departureCity} a ${item.arrivalCity}, ")
                append("partenza il ${item.departureTime}. ")
                append(
                    if (item.availableSeats < 5)
                        "Attenzione: rimangono solo ${item.availableSeats} posti a questa tariffa."
                    else
                        "${item.availableSeats} posti disponibili a questa tariffa."
                )
            }
            is CatalogItem.Hotel -> buildString {
                append("Sistemazione in ${item.roomType} a ${item.city}. ")
                append("Valutazione media degli ospiti: ${item.rating}/5.")
            }
            is CatalogItem.Excursion -> buildString {
                append("${item.activityType} della durata di ${item.duration}. ")
                append(
                    if (item.guideIncluded) "Guida esperta locale inclusa per l'intera durata."
                    else "Esplorazione libera, senza guida inclusa."
                )
            }
        }
    }

    Scaffold(
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = CardSurface,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 22.dp, vertical = 14.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "TOTALE",
                                color = InkMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                item.price,
                                color = Ink,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }

                        Canvas(modifier = Modifier.width(1.dp).height(36.dp)) {
                            drawLine(
                                color = Hairline,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f)
                            )
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Button(
                            onClick = { onBookNow(item.id.toString()) },
                            colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier.height(46.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                "PRENOTA ORA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-9).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(SfondoPremium)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 9.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(SfondoPremium)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = imageList[page],
                        contentDescription = "Galleria",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(TripifyDarkGreen.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.28f))
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* TODO: Aggiungi ai Preferiti */ },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            contentDescription = "Preferito",
                            tint = TripifyDarkGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (imageList.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(imageList.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == pagerState.currentPage) 6.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == pagerState.currentPage) Color.White
                                        else Color.White.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .offset(y = (-18).dp)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(TripifyGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (item) {
                            is CatalogItem.Hotel -> "HOTEL"
                            is CatalogItem.Flight -> "VOLO"
                            is CatalogItem.Excursion -> item.activityType.uppercase()
                            else -> ""
                        },
                        color = InkMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                when (item) {
                    is CatalogItem.Flight -> {
                        SectionLabel("Itinerario")
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(item.departureAirport.take(3).uppercase(), fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Ink)
                                Text(item.departureCity, fontSize = 10.sp, color = InkMuted)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.departureTime, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TripifyDarkGreen)
                            }

                            // Ora un dato reale: "Diretto" solo se stops == 0, altrimenti mostra il numero di scali
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Flight, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(16.dp))
                                Divider(color = Hairline, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(0.6f))
                                Text(
                                    text = if (item.isDirect) "Diretto" else "${item.stops} ${if (item.stops == 1) "scalo" else "scali"}",
                                    fontSize = 10.sp,
                                    color = if (item.isDirect) TripifyGreen else InkMuted,
                                    fontWeight = if (item.isDirect) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(item.arrivalAirport.take(3).uppercase(), fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Ink)
                                Text(item.arrivalCity, fontSize = 10.sp, color = InkMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow(
                            icon = Icons.Filled.AirlineSeatReclineNormal,
                            title = "Disponibilità",
                            subtitle = "Rimangono ${item.availableSeats} posti a questo prezzo",
                            iconColor = if (item.availableSeats < 5) Color(0xFFB3261E) else TripifyGreen
                        )
                    }

                    is CatalogItem.Hotel -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HotelHighlight(icon = Icons.Filled.Star, title = "${item.rating}", subtitle = "Rating", modifier = Modifier.weight(1f))
                            HotelHighlight(icon = Icons.Filled.Bed, title = item.roomType.take(14), subtitle = "Camera", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DetailRow(icon = Icons.Filled.LocationOn, title = "Indirizzo", subtitle = "${item.address}, ${item.city}")

                        // Amenities reali, non più inventate — mostrate solo se ce ne sono
                        if (item.amenities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionLabel("Servizi inclusi")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item.amenities.take(3).forEach { amenity ->
                                    Surface(
                                        color = TripifyGreen.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            amenity,
                                            color = TripifyDarkGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                if (item.amenities.size > 3) {
                                    Surface(
                                        color = Hairline,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "+${item.amenities.size - 3}",
                                            color = InkMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (item.locationLat != null && item.locationLng != null) {
                            OutlinedButton(
                                onClick = {
                                    val geoUri = Uri.parse("geo:${item.locationLat},${item.locationLng}?q=${item.locationLat},${item.locationLng}(${item.title})")
                                    try {
                                        val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply { setPackage("com.google.android.apps.maps") }
                                        context.startActivity(mapsIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        try {
                                            val genericIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                            context.startActivity(genericIntent)
                                        } catch (e: ActivityNotFoundException) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Nessuna app per le mappe trovata sul dispositivo")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)
                            ) {
                                Icon(Icons.Filled.Map, contentDescription = "Mappa", tint = TripifyGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Esplora i dintorni", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }

                    is CatalogItem.Excursion -> {
                        DetailRow(icon = Icons.Filled.Schedule, title = "Durata prevista", subtitle = item.duration)
                        DetailRow(icon = Icons.Filled.LocationOn, title = "Punto di ritrovo", subtitle = item.meetingPoint)
                        DetailRow(
                            icon = Icons.Filled.Tour,
                            title = "Guida e assistenza",
                            subtitle = if (item.guideIncluded) "Guida esperta locale inclusa" else "Esplorazione libera"
                        )
                        item.maxParticipants?.let { max ->
                            DetailRow(icon = Icons.Filled.Groups, title = "Partecipanti", subtitle = "Massimo $max persone")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Hairline)
                Spacer(modifier = Modifier.height(20.dp))

                SectionLabel("Panoramica")
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.animateContentSize(animationSpec = tween(250))) {
                    Text(
                        text = overviewText,
                        color = InkMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Mostra meno" else "Leggi tutto",
                        color = TripifyDarkGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        color = InkMuted
    )
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: Color = TripifyGreen) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(iconColor.copy(alpha = 0.1f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = InkMuted, fontSize = 11.sp)
            Text(subtitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun HotelHighlight(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = InkMuted, fontSize = 10.sp)
    }
}