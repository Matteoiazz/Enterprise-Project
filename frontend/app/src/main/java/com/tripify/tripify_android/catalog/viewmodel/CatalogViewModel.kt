package com.tripify.tripify_android.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.model.FareClassUi
import com.tripify.tripify_android.catalog.model.RoomTypeUi
import com.tripify.tripify_android.catalog.ui.components.NO_PRICE_LIMIT
import com.tripify.tripify_android.data.CatalogApi
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.CatalogItemDto
import com.tripify.tripify_android.data.model.PagedResponse
import com.tripify.tripify_android.data.model.RoomHoldRequest
import com.tripify.tripify_android.data.model.SeatHoldRequest
import com.tripify.tripify_android.itinerary.data.ItineraryRetrofit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class HoldOutcome {
    data class Success(val holdId: String, val expiresAt: String) : HoldOutcome()
    data class Unavailable(val message: String) : HoldOutcome()
    data class Error(val message: String) : HoldOutcome()
    data object RequiresLogin : HoldOutcome()
}

class CatalogViewModel(
    private val api: CatalogApi,
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Tutti")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxPrice = MutableStateFlow(NO_PRICE_LIMIT)
    val maxPrice: StateFlow<Float> = _maxPrice.asStateFlow()

    private val _minRating = MutableStateFlow(0)
    val minRating: StateFlow<Int> = _minRating.asStateFlow()

    private val _directOnly = MutableStateFlow(false)
    val directOnly: StateFlow<Boolean> = _directOnly.asStateFlow()

    private val _guideOnly = MutableStateFlow(false)
    val guideOnly: StateFlow<Boolean> = _guideOnly.asStateFlow()

    private val _selectedAmenities = MutableStateFlow<List<String>>(emptyList())
    val selectedAmenities: StateFlow<List<String>> = _selectedAmenities.asStateFlow()

    private val _destination = MutableStateFlow("")
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _departure = MutableStateFlow("")
    val departure: StateFlow<String> = _departure.asStateFlow()

    private val _departureDate = MutableStateFlow<LocalDate?>(null)
    val departureDate: StateFlow<LocalDate?> = _departureDate.asStateFlow()

    private val _passengers = MutableStateFlow(1)
    val passengers: StateFlow<Int> = _passengers.asStateFlow()
    private val _hotelCheckIn = MutableStateFlow<LocalDate?>(null)
    val hotelCheckIn: StateFlow<LocalDate?> = _hotelCheckIn.asStateFlow()

    private val _hotelCheckOut = MutableStateFlow<LocalDate?>(null)
    val hotelCheckOut: StateFlow<LocalDate?> = _hotelCheckOut.asStateFlow()

    private val _hotelRooms = MutableStateFlow(1)
    val hotelRooms: StateFlow<Int> = _hotelRooms.asStateFlow()

    private val _catalogList = MutableStateFlow<List<CatalogItem>>(emptyList())
    val catalogList: StateFlow<List<CatalogItem>> = _catalogList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLastPage = MutableStateFlow(true)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()
    private var currentPage = 0

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _recommendedItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val recommendedItems: StateFlow<List<CatalogItem>> = _recommendedItems.asStateFlow()

    private val _recommendationsLabel = MutableStateFlow("In base alle tue ultime ricerche")
    val recommendationsLabel: StateFlow<String> = _recommendationsLabel.asStateFlow()

    // Accumula ogni item mai visto (risultati di ricerca + raccomandazioni + fetch singoli),
    // così DetailScreen può trovare un item anche se non è più nella lista risultati corrente.
    private val _itemCache = MutableStateFlow<Map<Int, CatalogItem>>(emptyMap())
    val itemCache: StateFlow<Map<Int, CatalogItem>> = _itemCache.asStateFlow()

    private val _itemReviews = MutableStateFlow<List<com.tripify.tripify_android.data.model.ReviewDto>>(emptyList())
    val itemReviews: StateFlow<List<com.tripify.tripify_android.data.model.ReviewDto>> = _itemReviews.asStateFlow()

    private val _hasBookedCurrentItem = MutableStateFlow(false)
    val hasBookedCurrentItem: StateFlow<Boolean> = _hasBookedCurrentItem.asStateFlow()

    private var fetchJob: Job? = null
    private var searchDebounceJob: Job? = null
    private var nextPageJob: Job? = null

    /** L'identità reale (JWT) la legge il backend: qui serve solo per decidere se mostrare la UI di prenotazione. */
    suspend fun isLoggedIn(): Boolean = !tokenManager?.tokenFlow?.first().isNullOrBlank()

    /** Versione osservabile di isLoggedIn(), per aggiornare la UI (es. il bottone Accedi) quando cambia il token. */
    val isLoggedInState: StateFlow<Boolean> = (tokenManager?.tokenFlow?.map { !it.isNullOrBlank() } ?: MutableStateFlow(false))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        fetchCatalogData(isUserSearch = false)
        viewModelScope.launch { fetchRecommendations(category = "Tutti", excludeIds = emptySet()) }

        // Il segnale di preferenza è calcolato una sola volta (vedi resolvePreferenceSignal),
        // ma va ricalcolato se cambia l'utente loggato (logout/login con account diverso
        // nella stessa sessione), altrimenti resterebbero i "consigliati" di qualcun altro.
        tokenManager?.let { tm ->
            viewModelScope.launch {
                tm.tokenFlow.drop(1).distinctUntilChanged().collect {
                    preferenceSignalResolved = false
                    preferenceSignalCache = null
                    fetchRecommendations(category = "Tutti", excludeIds = emptySet())
                }
            }
        }
    }

    fun searchNow() {
        searchDebounceJob?.cancel()
        fetchCatalogData(isUserSearch = true)
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _minRating.value = 0
        fetchCatalogData(isUserSearch = true)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(500)
            fetchCatalogData(isUserSearch = true)
        }
    }

    fun searchFlightRoute(departure: String, destination: String, date: LocalDate?, passengers: Int) {
        _departure.value = departure
        _destination.value = destination
        _departureDate.value = date
        _passengers.value = passengers.coerceIn(1, 9)
        fetchCatalogData(isUserSearch = true)
    }

    fun searchHotels(city: String, checkIn: LocalDate?, checkOut: LocalDate?) {
        _destination.value = city
        _hotelCheckIn.value = checkIn
        _hotelCheckOut.value = checkOut
        _hotelRooms.value = 1
        fetchCatalogData(isUserSearch = true)
    }

    fun applyAdvancedFilters(
        price: Float,
        rating: Int,
        amenities: List<String>,
        direct: Boolean,
        guide: Boolean,
        destination: String,
        departure: String
    ) {
        _maxPrice.value = price
        _minRating.value = rating
        _selectedAmenities.value = amenities
        _directOnly.value = direct
        _guideOnly.value = guide
        _destination.value = destination
        _departure.value = departure
        fetchCatalogData(isUserSearch = true)
    }

    fun clearErrorMessage() { _errorMessage.value = null }

    private fun mapDtoToItem(dto: CatalogItemDto): CatalogItem {
        val priceString = "€ ${dto.price.toInt()}"
        val immaginiReali = if (!dto.imageUrls.isNullOrEmpty()) dto.imageUrls else listOf(
            "https://picsum.photos/seed/${dto.id}A/600/800",
            "https://picsum.photos/seed/${dto.id}B/600/800"
        )
        val rating = dto.rating?.toDouble()

        return when (dto.itemType.uppercase()) {
            "FLIGHT" -> CatalogItem.Flight(
                id = dto.id, title = dto.title, price = "Da $priceString", priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                hostId = dto.hostId ?: "",
                isUserGenerated = dto.isUserGenerated,
                departureAirport = dto.departureAirport ?: "N/D",
                arrivalAirport = dto.arrivalAirport ?: "N/D",
                departureCity = dto.departureCity ?: "N/D",
                arrivalCity = dto.arrivalCity ?: "N/D",
                departureTime = dto.departureTime?.take(10) ?: "Data da def.",
                availableSeats = dto.totalSeats ?: 0,
                stops = dto.stops ?: 0,
                rating = rating,
                fareClasses = dto.fareClasses?.map {
                    FareClassUi(id = it.id, name = it.name, price = it.price, totalSeats = it.totalSeats)
                } ?: emptyList()
            )
            "HOTEL" -> CatalogItem.Hotel(
                id = dto.id, title = dto.title, price = "Da $priceString/notte", priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                hostId = dto.hostId ?: "",
                isUserGenerated = dto.isUserGenerated,
                address = dto.address ?: "Indirizzo non disponibile",
                city = dto.city ?: "N/D",
                rating = rating ?: 0.0,
                amenities = dto.amenities ?: emptyList(),
                locationLat = dto.locationLat,
                locationLng = dto.locationLng,
                roomTypes = dto.roomTypes?.map {
                    RoomTypeUi(
                        id = it.id, name = it.name, description = it.description, price = it.price,
                        totalRooms = it.totalRooms, maxOccupancy = it.maxOccupancy,
                        benefits = it.benefits ?: emptyList(), imageUrls = it.imageUrls ?: emptyList()
                    )
                } ?: emptyList()
            )
            "ACTIVITY" -> CatalogItem.Excursion(
                id = dto.id, title = dto.title, price = priceString, priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                hostId = dto.hostId ?: "",
                isUserGenerated = dto.isUserGenerated,
                duration = dto.duration ?: "Da definire",
                guideIncluded = dto.guideIncluded ?: false,
                activityType = dto.activityType ?: "Esperienza",
                meetingPoint = dto.meetingPoint ?: "Da definire",
                maxParticipants = dto.maxParticipants,
                rating = rating
            )
            else -> CatalogItem.Excursion(
                id = dto.id, title = dto.title, price = priceString, priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                hostId = dto.hostId ?: "",
                isUserGenerated = dto.isUserGenerated,
                duration = dto.duration ?: "Da definire",
                guideIncluded = dto.guideIncluded ?: false,
                activityType = dto.activityType ?: "Esperienza",
                meetingPoint = dto.meetingPoint ?: "Da definire",
                maxParticipants = dto.maxParticipants,
                rating = rating
            )
        }
    }

    private fun cacheItems(items: List<CatalogItem>) {
        if (items.isEmpty()) return
        _itemCache.value = _itemCache.value + items.associateBy { it.id }
    }

    private suspend fun performSearch(page: Int): PagedResponse<CatalogItemDto> {

        val maxPriceParam = _maxPrice.value.toInt().takeIf { _maxPrice.value < NO_PRICE_LIMIT }
        return api.searchCatalog(
            category = _selectedCategory.value,
            query = _searchQuery.value.trim(),
            maxPrice = maxPriceParam,
            minRating = _minRating.value,
            destination = _destination.value.trim().ifBlank { null },
            departure = _departure.value.trim().ifBlank { null },
            guideIncluded = if (_guideOnly.value) true else null,
            amenities = _selectedAmenities.value.takeIf { it.isNotEmpty() },
            directOnly = if (_directOnly.value) true else null,
            departureDate = _departureDate.value?.format(DateTimeFormatter.ISO_LOCAL_DATE),

            minSeats = _passengers.value.takeIf { it > 1 },

            checkIn = _hotelCheckIn.value?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            checkOut = _hotelCheckOut.value?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            rooms = _hotelRooms.value.takeIf { it > 1 },
            page = page,
            size = 20
        )
    }

    private fun fetchCatalogData(isUserSearch: Boolean) {
        fetchJob?.cancel()

        nextPageJob?.cancel()
        fetchJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val pageResult = performSearch(page = 0)
                val mappedItems = pageResult.content.map { mapDtoToItem(it) }
                cacheItems(mappedItems)

                _catalogList.value = mappedItems
                currentPage = 0
                _isLastPage.value = pageResult.last

                if (isUserSearch) {
                    _hasSearched.value = true
                    fetchRecommendations(category = _selectedCategory.value, excludeIds = mappedItems.map { it.id }.toSet())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _catalogList.value = emptyList()
                _isLastPage.value = true
                _errorMessage.value = "Impossibile collegarsi al server. Riprova più tardi."
                _recommendedItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || _isLoadingMore.value || _isLastPage.value) return
        nextPageJob = viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val pageResult = performSearch(page = currentPage + 1)
                val mappedItems = pageResult.content.map { mapDtoToItem(it) }
                cacheItems(mappedItems)

                _catalogList.value = _catalogList.value + mappedItems
                currentPage += 1
                _isLastPage.value = pageResult.last
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()

                _isLastPage.value = true
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun categoryLabelOf(item: CatalogItem) = when (item) {
        is CatalogItem.Flight -> "Voli"
        is CatalogItem.Hotel -> "Hotel"
        is CatalogItem.Excursion -> "Attività"
    }

    private fun cityLabelOf(item: CatalogItem): String? = when (item) {
        is CatalogItem.Flight -> item.arrivalCity
        is CatalogItem.Hotel -> item.city
        is CatalogItem.Excursion -> null
    }

    private fun ratingOf(item: CatalogItem): Double = when (item) {
        is CatalogItem.Hotel -> item.rating
        is CatalogItem.Flight -> item.rating ?: 0.0
        is CatalogItem.Excursion -> item.rating ?: 0.0
    }

    private data class PreferenceSignal(val category: String, val city: String?, val signalIds: Set<Int>)

    private var preferenceSignalResolved = false
    private var preferenceSignalCache: PreferenceSignal? = null

    // Dedotto una sola volta per sessione da like (itinerary-service) e prenotazioni
    // passate (booking-service, sola lettura): categoria e città più ricorrenti tra
    // quegli item. Calcolarlo una volta sola evita che i "consigliati" cambino ad
    // ogni ricerca dell'utente. Nessun segnale -> null, si ricade sulla categoria
    // dell'ultima ricerca (comportamento per utenti anonimi o senza storico).
    private suspend fun resolvePreferenceSignal(): PreferenceSignal? {
        if (preferenceSignalResolved) return preferenceSignalCache
        preferenceSignalResolved = true

        val tm = tokenManager ?: return null
        if (!isLoggedIn()) return null

        // Entrambi gli endpoint restituiscono già "più recenti prima".
        val likedIds = runCatching {
            ItineraryRetrofit.create(tm).getLikedCatalogItemIds().takeIf { it.isSuccessful }?.body()
        }.getOrNull() ?: emptyList()

        val bookedIds = runCatching {
            RetrofitClient.createBookingApi(tm).getUserBookings(size = 20).takeIf { it.isSuccessful }?.body()?.content
        }.getOrNull()?.flatMap { it.lines.map { line -> line.catalogItemId } } ?: emptyList()

        val signalIds = (likedIds + bookedIds).distinct().take(15).map { it.toInt() }
        if (signalIds.isEmpty()) return null

        val resolvedItems = signalIds.mapNotNull { id ->
            _itemCache.value[id] ?: runCatching { mapDtoToItem(api.getItemById(id)) }.getOrNull()?.also { cacheItems(listOf(it)) }
        }
        if (resolvedItems.isEmpty()) return null

        val topCategory = resolvedItems.groupingBy { categoryLabelOf(it) }.eachCount().maxByOrNull { it.value }?.key
            ?: return null
        val topCity = resolvedItems.mapNotNull { cityLabelOf(it) }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        return PreferenceSignal(topCategory, topCity, signalIds.toSet()).also { preferenceSignalCache = it }
    }

    private suspend fun queryRecommendations(category: String, city: String?, excludeIds: Set<Int>): List<CatalogItem> {
        val dtos = api.searchCatalog(
            category = category,
            query = "",
            maxPrice = null,
            minRating = 0,
            destination = city,
            departure = null,
            guideIncluded = null,
            amenities = null,
            directOnly = null,
            page = 0,
            size = 20
        )
        return dtos.content
            .filter { it.id !in excludeIds }
            .map { mapDtoToItem(it) }
            .sortedByDescending { ratingOf(it) }
            .take(6)
    }

    private suspend fun fetchRecommendations(category: String, excludeIds: Set<Int>) {
        try {
            val preference = resolvePreferenceSignal()
            val allExcludeIds = excludeIds + (preference?.signalIds ?: emptySet())
            _recommendationsLabel.value = when {
                preference != null -> "Consigliati per te"
                _hasSearched.value -> "In base alle tue ultime ricerche"
                else -> "I più apprezzati"
            }

            var recommendations = queryRecommendations(
                category = preference?.category ?: category.ifBlank { "Tutti" },
                city = preference?.city,
                excludeIds = allExcludeIds
            )
            // Filtro categoria+città troppo stretto: allarga prima alla sola
            // categoria, poi a "Tutti", così la sezione non sparisce mai per un
            // utente con storico reale.
            if (recommendations.size < 3 && preference?.city != null) {
                recommendations = queryRecommendations(preference.category, null, allExcludeIds)
            }
            if (recommendations.isEmpty() && preference != null) {
                recommendations = queryRecommendations("Tutti", null, allExcludeIds)
            }

            cacheItems(recommendations)
            _recommendedItems.value = recommendations
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            _recommendedItems.value = emptyList()
        }
    }

    fun onItemViewed(item: CatalogItem) {
        _hasSearched.value = true
        val category = when (item) {
            is CatalogItem.Flight -> "Voli"
            is CatalogItem.Hotel -> "Hotel"
            is CatalogItem.Excursion -> "Attività"
        }
        viewModelScope.launch {
            fetchRecommendations(category = category, excludeIds = setOf(item.id))
        }
    }


    suspend fun getOrFetchItem(id: Int): CatalogItem? {
        _itemCache.value[id]?.let { return it }
        return try {
            val item = mapDtoToItem(api.getItemById(id))
            cacheItems(listOf(item))
            item
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchCitySuggestions(query: String): List<String> {
        if (query.trim().length < 2) return emptyList()
        return try {
            api.getCitySuggestions(query.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Disponibilità vera per una tipologia di camera nelle date scelte (per notte, non un numero statico). */
    suspend fun fetchRoomAvailability(roomTypeId: Int, checkIn: LocalDate, checkOut: LocalDate): Int? {
        return try {
            api.getRoomAvailability(roomTypeId, checkIn.format(DateTimeFormatter.ISO_LOCAL_DATE), checkOut.format(DateTimeFormatter.ISO_LOCAL_DATE)).available
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchSeatAvailability(fareClassId: Int): Int? {
        return try {
            api.getSeatAvailability(fareClassId).available
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Blocca temporaneamente N camere di una tipologia per il range di notti scelto. Richiede login. */
    suspend fun holdRoomType(roomTypeId: Int, checkIn: LocalDate, checkOut: LocalDate, rooms: Int): HoldOutcome {
        if (!isLoggedIn()) return HoldOutcome.RequiresLogin
        return runHold {
            api.holdRoom(
                roomTypeId,
                RoomHoldRequest(
                    checkIn = checkIn.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    checkOut = checkOut.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    rooms = rooms
                )
            )
        }
    }

    /** Blocca temporaneamente N posti di una classe tariffaria. Richiede login. */
    suspend fun holdFareClass(fareClassId: Int, seats: Int): HoldOutcome {
        if (!isLoggedIn()) return HoldOutcome.RequiresLogin
        return runHold { api.holdSeats(fareClassId, SeatHoldRequest(seats = seats)) }
    }

    private suspend fun runHold(block: suspend () -> com.tripify.tripify_android.data.model.HoldResultDto): HoldOutcome {
        return try {
            val result = block()
            HoldOutcome.Success(result.holdId, result.expiresAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when (e.code()) {
                409 -> HoldOutcome.Unavailable("Non è più disponibile per queste date/quantità: qualcun altro potrebbe averlo appena prenotato.")
                401 -> HoldOutcome.RequiresLogin
                else -> HoldOutcome.Error("Errore durante la richiesta. Riprova.")
            }
        } catch (e: Exception) {
            HoldOutcome.Error("Impossibile completare la richiesta. Controlla la connessione.")
        }
    }

    fun loadReviewsAndBookingStatus(itemId: Long) {
        viewModelScope.launch {
            tokenManager?.let { tm ->
                try {
                    val rApi = com.tripify.tripify_android.data.RetrofitClient.createReviewApi(tm)
                    val res = rApi.getReviewsForItem(itemId)
                    if (res.isSuccessful) {
                        _itemReviews.value = res.body() ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val bApi = com.tripify.tripify_android.data.RetrofitClient.createBookingApi(tm)
                    val bookedRes = bApi.hasUserBookedItem(itemId)
                    if (bookedRes.isSuccessful) {
                        _hasBookedCurrentItem.value = bookedRes.body() ?: false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun submitReview(itemId: Long, rating: Int, comment: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                tokenManager?.let { tm ->
                    val rApi = com.tripify.tripify_android.data.RetrofitClient.createReviewApi(tm)
                    val res = rApi.addReview(rating, comment, itemId)
                    if (res.isSuccessful) {
                        loadReviewsAndBookingStatus(itemId)
                        onSuccess()
                    } else {
                        val serverMessage = try {
                            res.errorBody()?.string()
                        } catch (e: Exception) {
                            null
                        }
                        onError(
                            if (!serverMessage.isNullOrBlank()) serverMessage
                            else "Impossibile inviare la recensione. Assicurati di aver prenotato."
                        )
                    }
                } ?: onError("Devi accedere per recensire.")
            } catch (e: Exception) {
                onError("Errore di rete. Controlla la connessione.")
            }
        }
    }

    fun updateReview(reviewId: Long, itemId: Long, rating: Int, comment: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                tokenManager?.let { tm ->
                    val rApi = com.tripify.tripify_android.data.RetrofitClient.createReviewApi(tm)
                    val res = rApi.updateReview(reviewId, rating, comment)
                    if (res.isSuccessful) {
                        loadReviewsAndBookingStatus(itemId)
                        onSuccess()
                    } else {
                        val serverMessage = try { res.errorBody()?.string() } catch (e: Exception) { null }
                        onError(if (!serverMessage.isNullOrBlank()) serverMessage else "Impossibile modificare la recensione.")
                    }
                } ?: onError("Devi accedere per modificare la recensione.")
            } catch (e: Exception) {
                onError("Errore di rete. Controlla la connessione.")
            }
        }
    }

    fun deleteReview(reviewId: Long, itemId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                tokenManager?.let { tm ->
                    val rApi = com.tripify.tripify_android.data.RetrofitClient.createReviewApi(tm)
                    val res = rApi.deleteReview(reviewId)
                    if (res.isSuccessful) {
                        loadReviewsAndBookingStatus(itemId)
                        onSuccess()
                    } else {
                        val serverMessage = try { res.errorBody()?.string() } catch (e: Exception) { null }
                        onError(if (!serverMessage.isNullOrBlank()) serverMessage else "Impossibile eliminare la recensione.")
                    }
                } ?: onError("Devi accedere per eliminare la recensione.")
            } catch (e: Exception) {
                onError("Errore di rete. Controlla la connessione.")
            }
        }
    }

    suspend fun getItemsByOrganizer(hostId: String): List<CatalogItem> {
        return try {
            val dtos = api.getItemsByHostId(hostId)
            val mappedItems = dtos.map { mapDtoToItem(it) }
            cacheItems(mappedItems)
            mappedItems
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
