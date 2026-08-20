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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import kotlinx.coroutines.launch
import com.tripify.tripify_android.BuildConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onBookNow: (String) -> Unit,
    onChatWithOrganizer: (String) -> Unit = {}
) {
    val catalogItems by viewModel.catalogList.collectAsState()
    val item = catalogItems.find { it.id.toString() == itemId }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Elemento non trovato", style = CatalogType.Body, color = CatalogColors.InkMuted)
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
                append(if (item.availableSeats < 5) "Attenzione: rimangono solo ${item.availableSeats} posti a questa tariffa." else "${item.availableSeats} posti disponibili a questa tariffa.")
            }
            is CatalogItem.Hotel -> buildString {
                append("Sistemazione in ${item.roomType} a ${item.city}. ")
                append("Valutazione media degli ospiti: ${item.rating}/5.")
            }
            is CatalogItem.Excursion -> buildString {
                append("${item.activityType} della durata di ${item.duration}. ")
                append(if (item.guideIncluded) "Guida esperta locale inclusa per l'intera durata." else "Esplorazione libera, senza guida inclusa.")
            }
        }
    }

    Scaffold(
        containerColor = SfondoPremium,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = CatalogColors.Surface,
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
                            Text("TOTALE", style = CatalogType.Overline, color = CatalogColors.InkMuted)
                            Text(item.price, style = CatalogType.PriceLarge, color = CatalogColors.Ink)
                        }

                        Canvas(modifier = Modifier.width(1.dp).height(36.dp)) {
                            drawLine(color = CatalogColors.Hairline, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f))
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Button(
                            onClick = { onBookNow(item.id.toString()) },
                            colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen),
                            shape = CatalogShapes.Field,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier.height(46.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("PRENOTA ORA", style = CatalogType.Button, color = Color.White)
                        }
                    }
                }
                Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = (-9).dp).size(18.dp).clip(CircleShape).background(SfondoPremium))
                Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 9.dp).size(18.dp).clip(CircleShape).background(SfondoPremium))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(model = imageList[page], contentDescription = "Galleria", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }

                Box(modifier = Modifier.fillMaxWidth().height(84.dp).background(Brush.verticalGradient(colors = listOf(TripifyDarkGreen.copy(alpha = 0.55f), Color.Transparent))))

                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f))) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { /* TODO: Aggiungi ai Preferiti */ }, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White)) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = "Preferito", tint = TripifyDarkGreen, modifier = Modifier.size(18.dp))
                    }
                }

                if (imageList.size > 1) {
                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(imageList.size) { index ->
                            Box(modifier = Modifier.size(if (index == pagerState.currentPage) 6.dp else 5.dp).clip(CircleShape).background(if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.45f)))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CatalogColors.Surface, shape = CatalogShapes.Sheet)
                    .offset(y = (-18).dp)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(TripifyGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (item) {
                            is CatalogItem.Hotel -> "HOTEL"
                            is CatalogItem.Flight -> "VOLO"
                            is CatalogItem.Excursion -> item.activityType.uppercase()
                            else -> ""
                        },
                        style = CatalogType.Overline,
                        color = CatalogColors.InkMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(item.title, style = CatalogType.DetailTitle, color = CatalogColors.Ink)

                Spacer(modifier = Modifier.height(20.dp))

                when (item) {
                    is CatalogItem.Flight -> {
                        SectionLabel("Itinerario")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth().border(1.dp, CatalogColors.Hairline, CatalogShapes.Field).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(item.departureAirport.take(3).uppercase(), style = CatalogType.AirportCode, color = CatalogColors.Ink)
                                Text(item.departureCity, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.departureTime, style = CatalogType.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = TripifyDarkGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Flight, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(16.dp))
                                HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(0.6f))
                                Text(
                                    text = if (item.isDirect) "Diretto" else "${item.stops} ${if (item.stops == 1) "scalo" else "scali"}",
                                    style = CatalogType.Caption,
                                    color = if (item.isDirect) TripifyGreen else CatalogColors.InkMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(item.arrivalAirport.take(3).uppercase(), style = CatalogType.AirportCode, color = CatalogColors.Ink)
                                Text(item.arrivalCity, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow(icon = Icons.Filled.AirlineSeatReclineNormal, title = "Disponibilità", subtitle = "Rimangono ${item.availableSeats} posti a questo prezzo", iconColor = if (item.availableSeats < 5) CatalogColors.Alert else TripifyGreen)
                    }

                    is CatalogItem.Hotel -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HotelHighlight(icon = Icons.Filled.Star, title = "${item.rating}", subtitle = "Rating", modifier = Modifier.weight(1f))
                            HotelHighlight(icon = Icons.Filled.Bed, title = item.roomType.take(14), subtitle = "Camera", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow(icon = Icons.Filled.LocationOn, title = "Indirizzo", subtitle = "${item.address}, ${item.city}")

                        if (item.amenities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionLabel("Servizi inclusi")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item.amenities.take(3).forEach { amenity ->
                                    Surface(color = TripifyGreen.copy(alpha = 0.1f), shape = CatalogShapes.Badge) {
                                        Text(amenity, color = TripifyDarkGreen, style = CatalogType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                                if (item.amenities.size > 3) {
                                    Surface(color = CatalogColors.Hairline, shape = CatalogShapes.Badge) {
                                        Text("+${item.amenities.size - 3}", color = CatalogColors.InkMuted, style = CatalogType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (item.locationLat != null && item.locationLng != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap?center=${item.locationLat},${item.locationLng}&zoom=15&size=600x300&scale=2&markers=color:0x1B4332%7C${item.locationLat},${item.locationLng}&key=${BuildConfig.MAPS_API_KEY}"
                            fun openMaps() {
                                val geoUri = Uri.parse("geo:${item.locationLat},${item.locationLng}?q=${item.locationLat},${item.locationLng}(${item.title})")
                                try {
                                    val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply { setPackage("com.google.android.apps.maps") }
                                    context.startActivity(mapsIntent)
                                } catch (e: ActivityNotFoundException) {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, geoUri)) }
                                    catch (e: ActivityNotFoundException) { scope.launch { snackbarHostState.showSnackbar("Nessuna app per le mappe trovata sul dispositivo") } }
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(CatalogShapes.Field).border(1.dp, CatalogColors.Hairline, CatalogShapes.Field).clickable { openMaps() }
                            ) {
                                AsyncImage(model = staticMapUrl, contentDescription = "Mappa della posizione", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Surface(color = CatalogColors.Surface.copy(alpha = 0.95f), shape = CatalogShapes.Badge, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Icon(Icons.Filled.Map, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apri in Maps", style = CatalogType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CatalogColors.Ink)
                                    }
                                }
                            }
                        }
                    }

                    is CatalogItem.Excursion -> {
                        DetailRow(icon = Icons.Filled.Schedule, title = "Durata prevista", subtitle = item.duration)
                        DetailRow(icon = Icons.Filled.LocationOn, title = "Punto di ritrovo", subtitle = item.meetingPoint)
                        DetailRow(icon = Icons.Filled.Tour, title = "Guida e assistenza", subtitle = if (item.guideIncluded) "Guida esperta locale inclusa" else "Esplorazione libera")
                        item.maxParticipants?.let { max -> DetailRow(icon = Icons.Filled.Groups, title = "Partecipanti", subtitle = "Massimo $max persone") }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = CatalogColors.Hairline)
                Spacer(modifier = Modifier.height(20.dp))

                SectionLabel("Panoramica")
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.animateContentSize(animationSpec = tween(250))) {
                    Text(
                        text = overviewText, style = CatalogType.Body, color = CatalogColors.InkMuted,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Mostra meno" else "Leggi tutto",
                        style = CatalogType.LabelStrong,
                        color = TripifyDarkGreen,
                        modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { onChatWithOrganizer(item.id.toString()) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = CatalogShapes.Field,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Chat", tint = TripifyDarkGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chatta con l'organizzatore", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), style = CatalogType.Overline, color = CatalogColors.InkMuted)
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: Color = TripifyGreen) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(38.dp).background(iconColor.copy(alpha = 0.1f), shape = CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = CatalogType.Caption, color = CatalogColors.InkMuted)
            Text(subtitle, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
        }
    }
}

@Composable
fun HotelHighlight(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.border(1.dp, CatalogColors.Hairline, CatalogShapes.Field).padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = CatalogType.LabelStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = CatalogType.Caption, color = CatalogColors.InkMuted)
    }
}