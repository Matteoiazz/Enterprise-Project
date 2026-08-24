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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.model.FareClassUi
import com.tripify.tripify_android.catalog.model.RoomTypeUi
import com.tripify.tripify_android.catalog.ui.components.*
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.catalog.viewmodel.HoldOutcome
import kotlinx.coroutines.launch
import com.tripify.tripify_android.BuildConfig
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private fun LocalDate.toEpochMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onBookNow: (String) -> Unit,
    onChatWithOrganizer: (String) -> Unit = {}
) {

    val id = remember(itemId) { itemId.toIntOrNull() }
    val cachedAtStart = remember(itemId) { id?.let { viewModel.itemCache.value[it] } }
    var item by remember(itemId) { mutableStateOf(cachedAtStart) }
    var isResolving by remember(itemId) { mutableStateOf(cachedAtStart == null && id != null) }

    LaunchedEffect(itemId) {
        if (item == null && id != null) {
            item = viewModel.getOrFetchItem(id)
            isResolving = false
        }
    }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize().background(CatalogColors.Background), contentAlignment = Alignment.Center) {
            if (isResolving) {
                CircularProgressIndicator(color = CatalogColors.AccentDark)
            } else {
                Text("Elemento non trovato", style = CatalogType.Body, color = CatalogColors.InkMuted)
            }
        }
        return
    }

    DetailContent(
        item = item!!,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onBookNow = onBookNow,
        onChatWithOrganizer = onChatWithOrganizer
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DetailContent(
    item: CatalogItem,
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onBookNow: (String) -> Unit,
    onChatWithOrganizer: (String) -> Unit
) {
    val imageList = item.imageUrls.ifEmpty { listOf(item.imageUrl) }
    val pagerState = rememberPagerState(pageCount = { imageList.size })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    var selectedRoomType by remember(item) {
        mutableStateOf((item as? CatalogItem.Hotel)?.roomTypes?.minByOrNull { it.price })
    }
    var selectedFareClass by remember(item) {
        mutableStateOf((item as? CatalogItem.Flight)?.fareClasses?.minByOrNull { it.price })
    }

    val searchedHotelCheckIn by viewModel.hotelCheckIn.collectAsState()
    val searchedHotelCheckOut by viewModel.hotelCheckOut.collectAsState()
    val searchedHotelRooms by viewModel.hotelRooms.collectAsState()
    val searchedPassengers by viewModel.passengers.collectAsState()

    var checkInMillis by remember(item) {
        mutableStateOf(if (item is CatalogItem.Hotel) searchedHotelCheckIn?.toEpochMillisUtc() else null)
    }
    var checkOutMillis by remember(item) {
        mutableStateOf(if (item is CatalogItem.Hotel) searchedHotelCheckOut?.toEpochMillisUtc() else null)
    }
    var quantity by remember(item) {
        mutableIntStateOf(
            when (item) {
                is CatalogItem.Hotel -> searchedHotelRooms
                is CatalogItem.Flight -> searchedPassengers
                is CatalogItem.Excursion -> 1
            }
        )
    }
    var isBooking by remember { mutableStateOf(false) }
    var roomAvailability by remember(item) { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var seatAvailability by remember(item) { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    val checkInDate = checkInMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val checkOutDate = checkOutMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

    LaunchedEffect(item, checkInDate, checkOutDate) {
        if (item is CatalogItem.Hotel && checkInDate != null && checkOutDate != null && checkInDate.isBefore(checkOutDate)) {
            roomAvailability = item.roomTypes.associate { rt ->
                rt.id to (viewModel.fetchRoomAvailability(rt.id, checkInDate, checkOutDate) ?: rt.totalRooms)
            }
        }
    }
    LaunchedEffect(item) {
        if (item is CatalogItem.Flight) {
            seatAvailability = item.fareClasses.associate { fc ->
                fc.id to (viewModel.fetchSeatAvailability(fc.id) ?: fc.totalSeats)
            }
        }
    }

    val imageHeight = 320.dp

    val overviewText = remember(item) {
        when (item) {
            is CatalogItem.Flight -> buildString {
                append("Volo da ${item.departureCity} a ${item.arrivalCity}, partenza il ${item.departureTime}. ")
                if (item.fareClasses.isNotEmpty()) {
                    append(if (item.fareClasses.size == 1) "1 classe tariffaria disponibile." else "${item.fareClasses.size} classi tariffarie disponibili.")
                }
            }
            is CatalogItem.Hotel -> buildString {
                append("Sistemazione a ${item.city}")
                if (item.roomTypes.isNotEmpty()) {
                    append(if (item.roomTypes.size == 1) " con 1 tipologia di camera disponibile" else " con ${item.roomTypes.size} tipologie di camera disponibili")
                }
                append(". ")
                append(if (item.rating > 0) "Valutazione media degli ospiti: ${ratingLabel(item.rating)}." else "Nessuna recensione ancora disponibile.")
            }
            is CatalogItem.Excursion -> buildString {
                append("${item.activityType} della durata di ${item.duration}. ")
                append(if (item.guideIncluded) "Guida esperta locale inclusa per l'intera durata." else "Esplorazione libera, senza guida inclusa.")
            }
        }
    }

    val nights = if (checkInDate != null && checkOutDate != null && checkInDate.isBefore(checkOutDate))
        ChronoUnit.DAYS.between(checkInDate, checkOutDate) else null

    // Finché mancano le date non si può calcolare un vero totale: si mostra il prezzo a notte
    // della tipologia scelta, con l'etichetta coerente, invece di un "TOTALE" che in realtà
    // non tiene conto di camere/notti.
    val totalLabel: String
    val totalPriceText: String
    when (item) {
        is CatalogItem.Hotel -> {
            val roomType = selectedRoomType
            if (roomType != null && nights != null) {
                totalLabel = if (nights == 1L) "TOTALE · 1 NOTTE" else "TOTALE · $nights NOTTI"
                totalPriceText = "€ ${(roomType.price * quantity * nights).toInt()}"
            } else {
                totalLabel = "PREZZO A NOTTE"
                totalPriceText = roomType?.let { "€ ${it.price.toInt()}" } ?: item.price
            }
        }
        is CatalogItem.Flight -> {
            totalLabel = "TOTALE"
            totalPriceText = selectedFareClass?.let { "€ ${(it.price * quantity).toInt()}" } ?: item.price
        }
        is CatalogItem.Excursion -> {
            totalLabel = "TOTALE"
            totalPriceText = item.price
        }
    }

    fun onPrenotaOra() {
        scope.launch {
            when (item) {
                is CatalogItem.Hotel -> {
                    val roomType = selectedRoomType
                    if (roomType == null) {
                        snackbarHostState.showSnackbar("Scegli una tipologia di camera")
                        return@launch
                    }
                    if (checkInDate == null || checkOutDate == null || !checkInDate.isBefore(checkOutDate)) {
                        snackbarHostState.showSnackbar("Scegli le date del soggiorno")
                        return@launch
                    }
                    isBooking = true
                    val outcome = viewModel.holdRoomType(roomType.id, checkInDate, checkOutDate, quantity)
                    isBooking = false
                    when (outcome) {
                        is HoldOutcome.Success -> {
                            snackbarHostState.showSnackbar("Camera bloccata per te: completa la prenotazione entro 15 minuti.")
                            onBookNow(item.id.toString())
                        }
                        is HoldOutcome.Unavailable -> snackbarHostState.showSnackbar(outcome.message)
                        is HoldOutcome.Error -> snackbarHostState.showSnackbar(outcome.message)
                    }
                }
                is CatalogItem.Flight -> {
                    val fareClass = selectedFareClass
                    if (fareClass == null) {
                        snackbarHostState.showSnackbar("Scegli una classe tariffaria")
                        return@launch
                    }
                    isBooking = true
                    val outcome = viewModel.holdFareClass(fareClass.id, quantity)
                    isBooking = false
                    when (outcome) {
                        is HoldOutcome.Success -> {
                            snackbarHostState.showSnackbar("Posto bloccato per te: completa la prenotazione entro 15 minuti.")
                            onBookNow(item.id.toString())
                        }
                        is HoldOutcome.Unavailable -> snackbarHostState.showSnackbar(outcome.message)
                        is HoldOutcome.Error -> snackbarHostState.showSnackbar(outcome.message)
                    }
                }
                is CatalogItem.Excursion -> onBookNow(item.id.toString())
            }
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = CatalogColors.Surface,
                    shadowElevation = 14.dp,
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
                            Text(totalLabel, style = CatalogType.Overline, color = CatalogColors.InkMuted)
                            Text(totalPriceText, style = CatalogType.PriceLarge, color = CatalogColors.Ink)
                        }

                        Canvas(modifier = Modifier.width(1.dp).height(36.dp)) {
                            drawLine(color = CatalogColors.Hairline, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f))
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Button(
                            onClick = { onPrenotaOra() },
                            enabled = !isBooking,
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Field,
                            contentPadding = PaddingValues(horizontal = 26.dp),
                            modifier = Modifier.height(48.dp).pressScale { onPrenotaOra() },
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            if (isBooking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("PRENOTA ORA", style = CatalogType.Button, color = Color.White)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = (-9).dp).size(18.dp).clip(CircleShape).background(CatalogColors.Background))
                Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 9.dp).size(18.dp).clip(CircleShape).background(CatalogColors.Background))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(model = imageList[page], contentDescription = "Galleria", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }

                PhotoScrim(modifier = Modifier.fillMaxWidth().height(120.dp), startY = 0f, maxAlpha = 0.55f)

                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f))) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Preferito",
                            tint = if (isFavorite) CatalogColors.Alert else CatalogColors.AccentDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (imageList.size > 1) {
                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
                    .offset(y = (-20).dp)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(CatalogColors.Accent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (item) {
                            is CatalogItem.Hotel -> "HOTEL"
                            is CatalogItem.Flight -> "VOLO"
                            is CatalogItem.Excursion -> item.activityType.uppercase()
                        },
                        style = CatalogType.Overline,
                        color = CatalogColors.InkMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(item.title, style = CatalogType.DetailTitle, color = CatalogColors.Ink)

                Spacer(modifier = Modifier.height(22.dp))

                when (item) {
                    is CatalogItem.Flight -> {
                        SectionLabel("Itinerario")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth().border(1.dp, CatalogColors.Hairline, CatalogShapes.Field).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(item.departureAirport.take(3).uppercase(), style = CatalogType.AirportCode, color = CatalogColors.Ink)
                                Text(item.departureCity, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.departureTime, style = CatalogType.Meta.copy(fontWeight = FontWeight.SemiBold), color = CatalogColors.AccentDark)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Flight, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(16.dp))
                                HorizontalDivider(color = CatalogColors.Hairline, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(0.6f))
                                Text(
                                    text = if (item.isDirect) "Diretto" else "${item.stops} ${if (item.stops == 1) "scalo" else "scali"}",
                                    style = CatalogType.Caption,
                                    color = if (item.isDirect) CatalogColors.Accent else CatalogColors.InkMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(item.arrivalAirport.take(3).uppercase(), style = CatalogType.AirportCode, color = CatalogColors.Ink)
                                Text(item.arrivalCity, style = CatalogType.Caption, color = CatalogColors.InkMuted)
                            }
                        }

                        if (item.rating != null && item.rating > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            RatingRow(rating = item.rating)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        SectionLabel("Scegli la classe")
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item.fareClasses.forEach { fareClass ->
                                FareClassOption(
                                    fareClass = fareClass,
                                    isSelected = selectedFareClass?.id == fareClass.id,
                                    available = seatAvailability[fareClass.id],
                                    onClick = { selectedFareClass = fareClass }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        QuantityStepper(label = "Passeggeri", quantity = quantity, max = 9, onChange = { quantity = it })
                    }

                    is CatalogItem.Hotel -> {
                        if (item.rating > 0) {
                            RatingRow(rating = item.rating)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        SectionLabel("Date del soggiorno")
                        Spacer(modifier = Modifier.height(10.dp))
                        DateRangeRow(
                            checkInMillis = checkInMillis,
                            checkOutMillis = checkOutMillis,
                            onCheckInChange = { checkInMillis = it },
                            onCheckOutChange = { checkOutMillis = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        SectionLabel("Scegli la tipologia")
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item.roomTypes.forEach { roomType ->
                                RoomTypeOption(
                                    roomType = roomType,
                                    isSelected = selectedRoomType?.id == roomType.id,
                                    available = if (checkInDate != null && checkOutDate != null) roomAvailability[roomType.id] else null,
                                    onClick = { selectedRoomType = roomType }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        QuantityStepper(label = "Camere", quantity = quantity, max = selectedRoomType?.totalRooms ?: 9, onChange = { quantity = it })

                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRow(icon = Icons.Filled.LocationOn, title = "Indirizzo", subtitle = "${item.address}, ${item.city}")

                        if (item.amenities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionLabel("Servizi inclusi")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item.amenities.take(3).forEach { amenity ->
                                    Surface(color = CatalogColors.AccentSoft, shape = CatalogShapes.Pill) {
                                        Text(amenity, color = CatalogColors.AccentDark, style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                                if (item.amenities.size > 3) {
                                    Surface(color = CatalogColors.SurfaceMuted, shape = CatalogShapes.Pill) {
                                        Text("+${item.amenities.size - 3}", color = CatalogColors.InkMuted, style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
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
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(CatalogShapes.Field).border(1.dp, CatalogColors.Hairline, CatalogShapes.Field).pressScale { openMaps() }
                            ) {
                                AsyncImage(model = staticMapUrl, contentDescription = "Mappa della posizione", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Surface(color = CatalogColors.Surface.copy(alpha = 0.95f), shape = CatalogShapes.Pill, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Icon(Icons.Filled.Map, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apri in Maps", style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold), color = CatalogColors.Ink)
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
                        if (item.rating != null && item.rating > 0) {
                            RatingRow(rating = item.rating)
                        }
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
                        color = CatalogColors.AccentDark,
                        modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            // 1. Recuperiamo il token JWT salvato nel DataStore tramite il tuo TokenManager
                            val tokenManager = com.tripify.tripify_android.data.TokenManager(context)
                            val token = tokenManager.tokenFlow.first()

                            // 2. Usiamo l'UUID reale dell'organizzatore associato a questo elemento del catalogo
                            val hostUuid = item.hostId

                            // 3. Chiamiamo il repository passando l'hostId e il token reale dell'utente
                            val chatRoom = com.tripify.tripify_android.chat.repository.ChatRepository.getOrCreateChatRoom(
                                hostId = hostUuid,
                                authToken = token
                            )

                            if (chatRoom != null) {
                                // 4. Apriamo la chat passando l'ID della stanza restituito dal backend
                                onChatWithOrganizer(chatRoom.id)
                            } else {
                                snackbarHostState.showSnackbar("Impossibile aprire la chat con l'organizzatore")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = CatalogShapes.Field,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Chat", tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
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
private fun RatingRow(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        RatingStars(rating = rating, starSize = 14.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(ratingLabel(rating), style = CatalogType.Caption, color = CatalogColors.InkMuted)
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: Color = CatalogColors.Accent) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(38.dp).background(iconColor.copy(alpha = 0.12f), shape = CircleShape), contentAlignment = Alignment.Center) {
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
        Icon(icon, contentDescription = null, tint = CatalogColors.Accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = CatalogType.LabelStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = CatalogType.Caption, color = CatalogColors.InkMuted)
    }
}

/** Numero di rimanenze in sovraimpressione su foto/icona di una tipologia: rosso quando è agli ultimi posti. */
@Composable
private fun RemainingBadge(count: Int, modifier: Modifier = Modifier) {
    val isCritical = count in 1..3
    val soldOut = count <= 0
    Surface(
        shape = CatalogShapes.Pill,
        color = if (soldOut || isCritical) CatalogColors.Alert else CatalogColors.Scrim.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Text(
            text = "$count",
            style = CatalogType.Caption.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

/**
 * Una card di tipologia camera selezionabile, in stile Booking: foto, nome, benefit,
 * prezzo a notte e disponibilità vera per le date scelte (non un numero statico).
 */
@Composable
private fun RoomTypeOption(roomType: RoomTypeUi, isSelected: Boolean, available: Int?, onClick: () -> Unit) {
    val soldOut = available != null && available <= 0
    Surface(
        shape = CatalogShapes.Field,
        color = if (isSelected) CatalogColors.AccentSoft else CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().pressScale(enabled = !soldOut) { if (!soldOut) onClick() }
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp)) {
                AsyncImage(
                    model = roomType.imageUrls.firstOrNull(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CatalogShapes.Badge)
                )
                if (available != null) {
                    RemainingBadge(count = available, modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(roomType.name, style = CatalogType.BodyStrong, color = CatalogColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                roomType.maxOccupancy?.let { max ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (max == 1) "1 persona" else "Fino a $max persone", style = CatalogType.Caption, color = CatalogColors.InkMuted)
                    }
                }
                if (!roomType.description.isNullOrBlank()) {
                    Text(roomType.description, style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (roomType.benefits.isNotEmpty()) {
                    Text(roomType.benefits.take(2).joinToString(" · "), style = CatalogType.Caption, color = CatalogColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (soldOut) "Non disponibile per queste date" else "€ ${roomType.price.toInt()} / notte",
                    style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (soldOut) CatalogColors.Alert else CatalogColors.AccentDark
                )
            }
            RadioButton(selected = isSelected, onClick = { if (!soldOut) onClick() }, enabled = !soldOut, colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark))
        }
    }
}

@Composable
private fun FareClassOption(fareClass: FareClassUi, isSelected: Boolean, available: Int?, onClick: () -> Unit) {
    val soldOut = available != null && available <= 0
    Surface(
        shape = CatalogShapes.Field,
        color = if (isSelected) CatalogColors.AccentSoft else CatalogColors.Surface,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) CatalogColors.AccentDark else CatalogColors.Hairline),
        modifier = Modifier.fillMaxWidth().pressScale(enabled = !soldOut) { if (!soldOut) onClick() }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(CatalogColors.AccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AirlineSeatReclineNormal, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(18.dp))
                }
                if (available != null) {
                    RemainingBadge(count = available, modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fareClass.name, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                Text(
                    text = if (soldOut) "Esaurita" else "€ ${fareClass.price.toInt()}",
                    style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (soldOut) CatalogColors.Alert else CatalogColors.AccentDark
                )
            }
            Text("€ ${fareClass.price.toInt()}", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
            Spacer(modifier = Modifier.width(10.dp))
            RadioButton(selected = isSelected, onClick = { if (!soldOut) onClick() }, enabled = !soldOut, colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark))
        }
    }
}

@Composable
private fun QuantityStepper(label: String, quantity: Int, max: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (quantity > 1) onChange(quantity - 1) }, enabled = quantity > 1, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuisci", tint = if (quantity > 1) CatalogColors.AccentDark else CatalogColors.Hairline)
            }
            Text("$quantity", style = CatalogType.BodyStrong, color = CatalogColors.Ink, modifier = Modifier.padding(horizontal = 12.dp))
            IconButton(onClick = { if (quantity < max) onChange(quantity + 1) }, enabled = quantity < max, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Aumenta", tint = if (quantity < max) CatalogColors.AccentDark else CatalogColors.Hairline)
            }
        }
    }
}

