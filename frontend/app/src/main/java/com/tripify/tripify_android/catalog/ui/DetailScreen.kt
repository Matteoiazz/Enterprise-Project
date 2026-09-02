package com.tripify.tripify_android.catalog.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.model.FareClassUi
import com.tripify.tripify_android.catalog.model.RoomTypeUi
import com.tripify.tripify_android.catalog.ui.components.*
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.util.CatalogPriceFormatter
import com.tripify.tripify_android.catalog.util.formattedPrice
import com.tripify.tripify_android.catalog.util.rememberCatalogCurrency
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.catalog.viewmodel.HoldOutcome
import com.tripify.tripify_android.itinerary.data.ItineraryRetrofit
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
    cartViewModel: CartViewModel,
    onNavigateBack: () -> Unit,
    onBookNow: (String) -> Unit,
    onChatWithOrganizer: (String) -> Unit = {}
) {

    val id = remember(itemId) { itemId.toIntOrNull() }
    val cachedAtStart = remember(itemId) { id?.let { viewModel.itemCache.value[it] } }
    var item by remember(itemId) { mutableStateOf(cachedAtStart) }
    var isResolving by remember(itemId) { mutableStateOf(cachedAtStart == null && id != null) }
    var retryTrigger by remember(itemId) { mutableStateOf(0) }

    LaunchedEffect(itemId, retryTrigger) {
        if (item == null && id != null) {
            isResolving = true
            item = viewModel.getOrFetchItem(id)
            isResolving = false
        }
        id?.let { viewModel.loadReviewsAndBookingStatus(it.toLong()) }
    }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize().background(CatalogColors.Background), contentAlignment = Alignment.Center) {
            if (isResolving) {
                CircularProgressIndicator(color = CatalogColors.AccentDark)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Elemento non trovato", style = CatalogType.Body, color = CatalogColors.InkMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { retryTrigger++ }) {
                        Text("Riprova", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                    }
                }
            }
        }
        return
    }

    DetailContent(
        item = item!!,
        viewModel = viewModel,
        cartViewModel = cartViewModel,
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
    cartViewModel: CartViewModel,
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
    var showAddToItineraryDialog by remember { mutableStateOf(false) }
    val reviews by viewModel.itemReviews.collectAsState()
    val hasBooked by viewModel.hasBookedCurrentItem.collectAsState()
    val isLoggedIn by viewModel.isLoggedInState.collectAsState()

    val currency by rememberCatalogCurrency()
    val itineraryApi = remember { ItineraryRetrofit.create(com.tripify.tripify_android.data.TokenManager(context)) }
    LaunchedEffect(item.id) {
        try {
            val response = itineraryApi.getLikedCatalogItemIds()
            if (response.isSuccessful) {
                isFavorite = response.body()?.contains(item.id.toLong()) == true
            }
        } catch (e: Exception) {
        }
    }

    var currentUserId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val token = com.tripify.tripify_android.data.TokenManager(context).tokenFlow.first()
        currentUserId = token?.let { com.tripify.tripify_android.itinerary.util.extractUserIdFromToken(it) }
    }

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

    val maxPassengers = selectedFareClass?.let { fareClass -> seatAvailability[fareClass.id] ?: fareClass.totalSeats } ?: 9
    val maxRooms = selectedRoomType?.let { roomType ->
        if (checkInDate != null && checkOutDate != null) roomAvailability[roomType.id] ?: roomType.totalRooms
        else roomType.totalRooms
    } ?: 9
    LaunchedEffect(maxPassengers, maxRooms) {
        val max = when (item) {
            is CatalogItem.Flight -> maxPassengers
            is CatalogItem.Hotel -> maxRooms
            is CatalogItem.Excursion -> 9
        }
        if (quantity > max) quantity = maxOf(1, max)
    }

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

    val totalLabel: String
    val totalPriceText: String
    when (item) {
        is CatalogItem.Hotel -> {
            val roomType = selectedRoomType
            if (roomType != null && nights != null) {
                totalLabel = if (nights == 1L) "TOTALE · 1 NOTTE" else "TOTALE · $nights NOTTI"
                totalPriceText = CatalogPriceFormatter.format(roomType.price * quantity * nights, currency)
            } else {
                totalLabel = "PREZZO A NOTTE"
                totalPriceText = roomType?.let { CatalogPriceFormatter.format(it.price, currency) } ?: item.formattedPrice(currency)
            }
        }
        is CatalogItem.Flight -> {
            totalLabel = "TOTALE"
            totalPriceText = selectedFareClass?.let { CatalogPriceFormatter.format(it.price * quantity, currency) } ?: item.formattedPrice(currency)
        }
        is CatalogItem.Excursion -> {
            totalLabel = "TOTALE"
            totalPriceText = item.formattedPrice(currency)
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
                    when (outcome) {
                        is HoldOutcome.Success -> {
                            cartViewModel.addItemToCart(
                                catalogItemId = item.id.toLong(),
                                quantity = quantity,
                                roomTypeId = roomType.id.toLong(),
                                checkIn = checkInDate.toString(),
                                checkOut = checkOutDate.toString()
                            ) { success ->
                                isBooking = false
                                scope.launch {
                                    if (success) onBookNow(item.id.toString())
                                    else snackbarHostState.showSnackbar("Camera bloccata, ma non è stato possibile aggiungerla al carrello")
                                }
                            }
                        }
                        is HoldOutcome.Unavailable -> { isBooking = false; snackbarHostState.showSnackbar(outcome.message) }
                        is HoldOutcome.Error -> { isBooking = false; snackbarHostState.showSnackbar(outcome.message) }
                        is HoldOutcome.RequiresLogin -> { isBooking = false; snackbarHostState.showSnackbar("Accedi per completare la prenotazione") }
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
                    when (outcome) {
                        is HoldOutcome.Success -> {
                            cartViewModel.addItemToCart(
                                catalogItemId = item.id.toLong(),
                                quantity = quantity,
                                fareClassId = fareClass.id.toLong()
                            ) { success ->
                                isBooking = false
                                scope.launch {
                                    if (success) onBookNow(item.id.toString())
                                    else snackbarHostState.showSnackbar("Posto bloccato, ma non è stato possibile aggiungerlo al carrello")
                                }
                            }
                        }
                        is HoldOutcome.Unavailable -> { isBooking = false; snackbarHostState.showSnackbar(outcome.message) }
                        is HoldOutcome.Error -> { isBooking = false; snackbarHostState.showSnackbar(outcome.message) }
                        is HoldOutcome.RequiresLogin -> { isBooking = false; snackbarHostState.showSnackbar("Accedi per completare la prenotazione") }
                    }
                }
                is CatalogItem.Excursion -> {
                    if (!viewModel.isLoggedIn()) {
                        snackbarHostState.showSnackbar("Accedi per completare la prenotazione")
                        return@launch
                    }
                    isBooking = true
                    cartViewModel.addItemToCart(catalogItemId = item.id.toLong(), quantity = quantity) { success ->
                        isBooking = false
                        scope.launch {
                            if (success) onBookNow(item.id.toString())
                            else snackbarHostState.showSnackbar("Impossibile aggiungere l'attività al carrello")
                        }
                    }
                }
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

                PhotoScrim(modifier = Modifier.fillMaxSize(), startY = 0f, maxAlpha = 0.55f)

                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(elevation = 3.dp, shape = CircleShape, ambientColor = Color.Black)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.38f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    val heartScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1f else 0.92f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "heartScale"
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                val token = com.tripify.tripify_android.data.TokenManager(context).tokenFlow.first()
                                if (token.isNullOrBlank()) {
                                    snackbarHostState.showSnackbar("Accedi per salvare tra i preferiti")
                                    return@launch
                                }
                                try {
                                    val response = itineraryApi.toggleCatalogItemLike(item.id.toLong())
                                    if (response.isSuccessful) {
                                        isFavorite = response.body()?.liked == true
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Impossibile salvare tra i preferiti")
                                }
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(elevation = if (isFavorite) 6.dp else 2.dp, shape = CircleShape, ambientColor = CatalogColors.Alert)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Preferito",
                            tint = if (isFavorite) CatalogColors.Alert else CatalogColors.AccentDark,
                            modifier = Modifier.size(20.dp).scale(heartScale)
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = innerPadding.calculateBottomPadding() + 60.dp)
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
                                    currency = currency,
                                    onClick = { selectedFareClass = fareClass }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        QuantityStepper(label = "Passeggeri", quantity = quantity, max = maxOf(1, maxPassengers), onChange = { quantity = it })
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
                                    currency = currency,
                                    onClick = { selectedRoomType = roomType }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        QuantityStepper(label = "Camere", quantity = quantity, max = maxOf(1, maxRooms), onChange = { quantity = it })

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

                if (item.isUserGenerated) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val tokenManager = com.tripify.tripify_android.data.TokenManager(context)
                                val token = tokenManager.tokenFlow.first()
                                if (token.isNullOrBlank()) {
                                    snackbarHostState.showSnackbar("Accedi per contattare l'organizzatore")
                                    return@launch
                                }

                                val hostUuid = item.hostId

                                try {
                                    // Chiamata di rete protetta dal try/catch
                                    val chatRoom = com.tripify.tripify_android.chat.repository.ChatRepository.getOrCreateChatRoom(
                                        hostId = hostUuid,
                                        title = "Organizzatore ${item.title}",
                                        authToken = token
                                    )

                                    if (chatRoom != null) {
                                        onChatWithOrganizer(chatRoom.id)
                                    } else {
                                        snackbarHostState.showSnackbar("Impossibile aprire la chat con l'organizzatore")
                                    }
                                } catch (e: Exception) {
                                    // Gestione dell'eccezione imprevista con feedback visivo
                                    snackbarHostState.showSnackbar("Errore di connessione: impossibile avviare la chat")
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val tokenManager = com.tripify.tripify_android.data.TokenManager(context)
                            val token = tokenManager.tokenFlow.first()
                            if (token.isNullOrBlank()) {
                                snackbarHostState.showSnackbar("Accedi per aggiungere a un itinerario")
                            } else {
                                showAddToItineraryDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = CatalogShapes.Field,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CatalogColors.Hairline)
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aggiungi a un itinerario", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CatalogColors.Hairline)
                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("Recensioni degli utenti")
                Spacer(modifier = Modifier.height(16.dp))

                if (reviews.isNotEmpty()) {
                    val avg = reviews.map { it.rating }.average()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = String.format(Locale.ITALY, "%.1f", avg),
                            style = CatalogType.PriceLarge,
                            color = CatalogColors.Ink
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            RatingRow(rating = avg)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (reviews.size == 1) "1 recensione" else "${reviews.size} recensioni",
                                style = CatalogType.Caption,
                                color = CatalogColors.InkMuted
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val total = reviews.size
                        (5 downTo 1).forEach { star ->
                            val count = reviews.count { it.rating == star }
                            RatingDistributionRow(
                                star = star,
                                count = count,
                                fraction = if (total > 0) count.toFloat() / total else 0f
                            )
                        }
                    }
                }

                val myReview = currentUserId?.let { uid -> reviews.find { it.travelerId == uid } }
                val isHost = currentUserId != null && item.hostId == currentUserId

                if (hasBooked && myReview == null) {
                    var myRating by remember { mutableIntStateOf(0) }
                    var myComment by remember { mutableStateOf("") }
                    var isSubmitting by remember { mutableStateOf(false) }
                    var shareName by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = CatalogShapes.Field,
                        colors = CardDefaults.cardColors(containerColor = CatalogColors.SurfaceMuted)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sei stato qui? Lascia una recensione!", style = CatalogType.LabelStrong, color = CatalogColors.Ink)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = if (i <= myRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = null,
                                        tint = CatalogColors.Gold,
                                        modifier = Modifier.size(36.dp).clickable { myRating = i }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = myComment,
                                onValueChange = { myComment = it },
                                placeholder = { Text("Racconta la tua esperienza...", style = CatalogType.Body, color = CatalogColors.InkSubtle) },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = CatalogShapes.Field,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CatalogColors.Accent,
                                    unfocusedBorderColor = CatalogColors.Hairline,
                                    focusedContainerColor = CatalogColors.Surface,
                                    unfocusedContainerColor = CatalogColors.Surface
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            ReviewNameToggle(
                                checked = shareName,
                                onCheckedChange = { shareName = it }
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (myRating > 0 && myComment.isNotBlank()) {
                                        isSubmitting = true
                                        viewModel.submitReview(
                                            itemId = item.id.toLong(),
                                            rating = myRating,
                                            comment = myComment,
                                            showName = shareName,
                                            onSuccess = {
                                                isSubmitting = false
                                                myRating = 0
                                                myComment = ""
                                                scope.launch { snackbarHostState.showSnackbar("Recensione pubblicata!") }
                                            },
                                            onError = { msg ->
                                                isSubmitting = false
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        )
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Inserisci un voto e un commento") }
                                    }
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                                shape = CatalogShapes.Pill,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CatalogColors.Surface, strokeWidth = 2.dp)
                                else Text("Pubblica", style = CatalogType.Button)
                            }
                        }
                    }
                } else if (myReview != null) {
                    var isEditingReview by remember(myReview.id) { mutableStateOf(false) }
                    var editRating by remember(myReview.id) { mutableIntStateOf(myReview.rating) }
                    var editComment by remember(myReview.id) { mutableStateOf(myReview.comment) }
                    var editShareName by remember(myReview.id) { mutableStateOf(!myReview.travelerName.isNullOrBlank()) }
                    var isSavingEdit by remember { mutableStateOf(false) }
                    var showDeleteReviewConfirm by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = CatalogShapes.Field,
                        colors = CardDefaults.cardColors(containerColor = CatalogColors.SurfaceMuted)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("La tua recensione", style = CatalogType.LabelStrong, color = CatalogColors.Ink)
                                if (!isEditingReview) {
                                    Row {
                                        IconButton(onClick = { isEditingReview = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Modifica recensione", tint = CatalogColors.InkMuted, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { showDeleteReviewConfirm = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Elimina recensione", tint = CatalogColors.Alert, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isEditingReview) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = if (i <= editRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                            contentDescription = null,
                                            tint = CatalogColors.Gold,
                                            modifier = Modifier.size(36.dp).clickable { editRating = i }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = editComment,
                                    onValueChange = { editComment = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    shape = CatalogShapes.Field,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CatalogColors.Accent,
                                        unfocusedBorderColor = CatalogColors.Hairline,
                                        focusedContainerColor = CatalogColors.Surface,
                                        unfocusedContainerColor = CatalogColors.Surface
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                ReviewNameToggle(
                                    checked = editShareName,
                                    onCheckedChange = { editShareName = it }
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                                    TextButton(onClick = {
                                        isEditingReview = false
                                        editRating = myReview.rating
                                        editComment = myReview.comment
                                        editShareName = !myReview.travelerName.isNullOrBlank()
                                    }) {
                                        Text("Annulla", style = CatalogType.Button, color = CatalogColors.InkMuted)
                                    }
                                    Button(
                                        onClick = {
                                            val reviewId = myReview.id
                                            if (reviewId == null) {
                                                scope.launch { snackbarHostState.showSnackbar("Errore: recensione non valida") }
                                            } else if (editRating > 0 && editComment.isNotBlank()) {
                                                isSavingEdit = true
                                                viewModel.updateReview(
                                                    reviewId = reviewId,
                                                    itemId = item.id.toLong(),
                                                    rating = editRating,
                                                    comment = editComment,
                                                    showName = editShareName,
                                                    onSuccess = {
                                                        isSavingEdit = false
                                                        isEditingReview = false
                                                        scope.launch { snackbarHostState.showSnackbar("Recensione aggiornata") }
                                                    },
                                                    onError = { msg ->
                                                        isSavingEdit = false
                                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                                    }
                                                )
                                            } else {
                                                scope.launch { snackbarHostState.showSnackbar("Inserisci un voto e un commento") }
                                            }
                                        },
                                        enabled = !isSavingEdit,
                                        colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                                        shape = CatalogShapes.Pill
                                    ) {
                                        if (isSavingEdit) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CatalogColors.Surface, strokeWidth = 2.dp)
                                        else Text("Salva", style = CatalogType.Button)
                                    }
                                }
                            } else {
                                RatingRow(rating = myReview.rating.toDouble())
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(myReview.comment, style = CatalogType.Body, color = CatalogColors.Ink)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (!myReview.travelerName.isNullOrBlank())
                                        "Visibile agli altri con il tuo nome (${myReview.travelerName})"
                                    else "Visibile agli altri come \"Utente verificato\"",
                                    style = CatalogType.Caption,
                                    color = CatalogColors.InkSubtle
                                )
                                if (myReview.helpfulCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (myReview.helpfulCount == 1) "1 persona ha trovato utile la tua recensione"
                                        else "${myReview.helpfulCount} persone hanno trovato utile la tua recensione",
                                        style = CatalogType.Caption,
                                        color = CatalogColors.InkMuted
                                    )
                                }
                                ReviewReplySection(review = myReview, isHost = false, onReply = { _, done -> done(true) })
                            }
                        }
                    }

                    if (showDeleteReviewConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteReviewConfirm = false },
                            containerColor = CatalogColors.Surface,
                            title = { Text("Eliminare la recensione?", style = CatalogType.LabelStrong, color = CatalogColors.Ink) },
                            text = { Text("L'operazione non è reversibile.", style = CatalogType.Body, color = CatalogColors.InkMuted) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteReviewConfirm = false
                                    val reviewId = myReview.id
                                    if (reviewId != null) {
                                        viewModel.deleteReview(
                                            reviewId = reviewId,
                                            itemId = item.id.toLong(),
                                            onSuccess = { scope.launch { snackbarHostState.showSnackbar("Recensione eliminata") } },
                                            onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                        )
                                    }
                                }) { Text("Elimina", style = CatalogType.Button, color = CatalogColors.Alert) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteReviewConfirm = false }) { Text("Annulla", style = CatalogType.Button, color = CatalogColors.InkMuted) }
                            }
                        )
                    }
                } else if (!isHost) {
                    Surface(
                        shape = CatalogShapes.Field,
                        color = CatalogColors.SurfaceMuted,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = CatalogColors.InkMuted, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (!isLoggedIn) "Accedi e prenota per poter lasciare una recensione."
                                else "Solo chi ha confermato la prenotazione può lasciare una recensione.",
                                style = CatalogType.Caption,
                                color = CatalogColors.InkMuted
                            )
                        }
                    }
                }

                val otherReviews = reviews.filter { it !== myReview }

                if (reviews.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).background(CatalogColors.SurfaceMuted, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Nessuna recensione", style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Prenota e sii il primo a raccontare la tua esperienza!", style = CatalogType.Caption, color = CatalogColors.InkMuted, textAlign = TextAlign.Center)
                    }
                } else {
                    var reviewStarFilter by remember(item.id) { mutableStateOf<Int?>(null) }
                    var reviewSort by remember(item.id) { mutableStateOf(ReviewSort.RECENT) }
                    LaunchedEffect(reviews.size) {
                        if (reviews.size < 3) reviewStarFilter = null
                    }

                    if (reviews.size >= 3) {
                        val countsByStar = (5 downTo 1).map { star -> star to reviews.count { it.rating == star } }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReviewFilterChip(
                                label = "Tutte",
                                count = reviews.size,
                                selected = reviewStarFilter == null,
                                onClick = { reviewStarFilter = null }
                            )
                            countsByStar.forEach { (star, count) ->
                                if (count > 0) {
                                    ReviewFilterChip(
                                        label = "$star",
                                        showStarIcon = true,
                                        count = count,
                                        selected = reviewStarFilter == star,
                                        onClick = { reviewStarFilter = if (reviewStarFilter == star) null else star }
                                    )
                                }
                            }
                        }
                    }

                    if (reviews.size >= 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ORDINA",
                                style = CatalogType.Overline,
                                color = CatalogColors.InkSubtle,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            ReviewSort.entries.forEach { opt ->
                                ReviewSortChip(
                                    label = opt.label,
                                    selected = reviewSort == opt,
                                    onClick = { reviewSort = opt }
                                )
                            }
                        }
                    }

                    val filteredReviews = reviewStarFilter?.let { star -> otherReviews.filter { it.rating == star } } ?: otherReviews
                    val visibleReviews = when (reviewSort) {
                        ReviewSort.RECENT -> filteredReviews.sortedByDescending { it.id ?: 0L }
                        ReviewSort.HELPFUL -> filteredReviews.sortedWith(
                            compareByDescending<com.tripify.tripify_android.data.model.ReviewDto> { it.helpfulCount }
                                .thenByDescending { it.id ?: 0L }
                        )
                        ReviewSort.RATING_DESC -> filteredReviews.sortedWith(
                            compareByDescending<com.tripify.tripify_android.data.model.ReviewDto> { it.rating }
                                .thenByDescending { it.id ?: 0L }
                        )
                        ReviewSort.RATING_ASC -> filteredReviews.sortedWith(
                            compareBy<com.tripify.tripify_android.data.model.ReviewDto> { it.rating }
                                .thenByDescending { it.id ?: 0L }
                        )
                    }

                    if (visibleReviews.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (reviewStarFilter != null) "Nessun'altra recensione a $reviewStarFilter stelle"
                                else "Ancora nessun'altra recensione",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted
                            )
                            if (reviewStarFilter != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { reviewStarFilter = null }) {
                                    Text("Mostra tutte", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                                }
                            }
                        }
                    } else {
                        visibleReviews.forEach { rev ->
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).background(CatalogColors.AccentSoft, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (!rev.travelerName.isNullOrBlank()) rev.travelerName!!
                                            else "Utente verificato",
                                            style = CatalogType.LabelStrong,
                                            color = CatalogColors.Ink
                                        )
                                        RatingRow(rating = rev.rating.toDouble())
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(rev.comment, style = CatalogType.Body, color = CatalogColors.Ink)
                                rev.id?.let { reviewId ->
                                    ReviewReplySection(
                                        review = rev,
                                        isHost = isHost,
                                        onReply = { text, done ->
                                            viewModel.replyToReview(
                                                reviewId = reviewId,
                                                itemId = item.id.toLong(),
                                                reply = text,
                                                onSuccess = {
                                                    done(true)
                                                    scope.launch { snackbarHostState.showSnackbar("Risposta pubblicata") }
                                                },
                                                onError = { msg ->
                                                    done(false)
                                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                                }
                                            )
                                        }
                                    )
                                }
                                if (rev.id != null && !isHost) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HelpfulButton(
                                        count = rev.helpfulCount,
                                        marked = rev.helpfulByMe,
                                        enabled = isLoggedIn,
                                        onClick = {
                                            viewModel.toggleReviewHelpful(rev.id) { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        }
                                    )
                                } else if (rev.helpfulCount > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (rev.helpfulCount == 1) "1 persona l'ha trovata utile"
                                        else "${rev.helpfulCount} persone l'hanno trovata utile",
                                        style = CatalogType.Caption,
                                        color = CatalogColors.InkMuted
                                    )
                                }
                                HorizontalDivider(color = CatalogColors.Hairline, modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showAddToItineraryDialog) {
            com.tripify.tripify_android.itinerary.ui.AddToItineraryDialog(
                catalogItem = item,
                onDismiss = { showAddToItineraryDialog = false },
                onAdded = {
                    showAddToItineraryDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Aggiunto all'itinerario") }
                }
            )
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

enum class ReviewSort(val label: String) {
    RECENT("Più recenti"),
    HELPFUL("Più utili"),
    RATING_DESC("Voto più alto"),
    RATING_ASC("Voto più basso")
}

@Composable
private fun ReviewSortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = CatalogType.Label,
        color = if (selected) Color.White else CatalogColors.Ink,
        modifier = Modifier
            .clip(CatalogShapes.Pill)
            .background(if (selected) CatalogColors.AccentDark else CatalogColors.SurfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ReviewNameToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CatalogShapes.Field)
            .background(CatalogColors.Surface)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Mostra il mio nome", style = CatalogType.Label, color = CatalogColors.Ink)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (checked) "Gli altri vedranno il tuo nome e cognome"
                else "Comparirai come \"Utente verificato\"",
                style = CatalogType.Caption,
                color = CatalogColors.InkSubtle
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CatalogColors.Surface,
                checkedTrackColor = CatalogColors.AccentDark,
                uncheckedThumbColor = CatalogColors.Surface,
                uncheckedTrackColor = CatalogColors.InkSubtle
            )
        )
    }
}

@Composable
private fun RatingDistributionRow(star: Int, count: Int, fraction: Float) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "ratingBar"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$star",
            style = CatalogType.Caption,
            color = CatalogColors.InkMuted,
            modifier = Modifier.width(10.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Filled.Star, contentDescription = null, tint = CatalogColors.Gold, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CatalogShapes.Pill)
                .background(CatalogColors.SurfaceMuted)
        ) {
            if (animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction)
                        .clip(CatalogShapes.Pill)
                        .background(CatalogColors.Gold)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = CatalogType.Caption,
            color = CatalogColors.InkMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
private fun HelpfulButton(
    count: Int,
    marked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CatalogShapes.Pill)
            .background(if (marked) CatalogColors.AccentSoft else CatalogColors.SurfaceMuted)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (marked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = null,
            tint = if (marked) CatalogColors.AccentDark else CatalogColors.InkMuted,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (count > 0) "Utile · $count" else "Utile",
            style = CatalogType.Label,
            color = if (marked) CatalogColors.AccentDark else CatalogColors.InkMuted
        )
    }
}

@Composable
private fun ReviewFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    showStarIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CatalogShapes.Pill)
            .background(if (selected) CatalogColors.AccentDark else CatalogColors.SurfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showStarIcon) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (selected) Color.White else CatalogColors.Gold,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = "$label ($count)",
            style = CatalogType.LabelStrong,
            color = if (selected) Color.White else CatalogColors.Ink
        )
    }
}

@Composable
private fun ReviewReplySection(
    review: com.tripify.tripify_android.data.model.ReviewDto,
    isHost: Boolean,
    onReply: (String, (Boolean) -> Unit) -> Unit
) {
    var editing by remember(review.id) { mutableStateOf(false) }
    var text by remember(review.id) { mutableStateOf(review.reply ?: "") }
    var sending by remember(review.id) { mutableStateOf(false) }

    LaunchedEffect(review.reply) {
        if (!editing) text = review.reply ?: ""
    }

    val existing = review.reply

    if (existing != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 10.dp)
                .clip(CatalogShapes.Field)
                .background(CatalogColors.AccentSoft)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Risposta dell'organizzatore", style = CatalogType.Overline, color = CatalogColors.AccentDark)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(existing, style = CatalogType.Body, color = CatalogColors.Ink)
        }
        if (isHost && !editing) {
            TextButton(onClick = { editing = true }) {
                Text("Modifica risposta", style = CatalogType.Label, color = CatalogColors.InkMuted)
            }
        }
    } else if (isHost && !editing) {
        TextButton(
            onClick = { editing = true },
            contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 8.dp, bottom = 0.dp)
        ) {
            Text("Rispondi", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
        }
    }

    if (isHost && editing) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Scrivi una risposta pubblica...", style = CatalogType.Body, color = CatalogColors.InkSubtle) },
                modifier = Modifier.fillMaxWidth(),
                shape = CatalogShapes.Field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CatalogColors.Accent,
                    unfocusedBorderColor = CatalogColors.Hairline,
                    focusedContainerColor = CatalogColors.Surface,
                    unfocusedContainerColor = CatalogColors.Surface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = { editing = false; text = existing ?: "" }) {
                    Text("Annulla", style = CatalogType.Button, color = CatalogColors.InkMuted)
                }
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            sending = true
                            onReply(text.trim()) { success ->
                                sending = false
                                if (success) editing = false
                            }
                        }
                    },
                    enabled = !sending && text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                    shape = CatalogShapes.Pill
                ) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CatalogColors.Surface, strokeWidth = 2.dp)
                    else Text("Invia", style = CatalogType.Button)
                }
            }
        }
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

@Composable
private fun RoomTypeOption(roomType: RoomTypeUi, isSelected: Boolean, available: Int?, currency: String, onClick: () -> Unit) {
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
                    text = if (soldOut) "Non disponibile per queste date" else "${CatalogPriceFormatter.format(roomType.price, currency)} / notte",
                    style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (soldOut) CatalogColors.Alert else CatalogColors.AccentDark
                )
            }
            RadioButton(selected = isSelected, onClick = { if (!soldOut) onClick() }, enabled = !soldOut, colors = RadioButtonDefaults.colors(selectedColor = CatalogColors.AccentDark))
        }
    }
}

@Composable
private fun FareClassOption(fareClass: FareClassUi, isSelected: Boolean, available: Int?, currency: String, onClick: () -> Unit) {
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
                    text = if (soldOut) "Esaurita" else CatalogPriceFormatter.format(fareClass.price, currency),
                    style = CatalogType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = if (soldOut) CatalogColors.Alert else CatalogColors.AccentDark
                )
            }
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