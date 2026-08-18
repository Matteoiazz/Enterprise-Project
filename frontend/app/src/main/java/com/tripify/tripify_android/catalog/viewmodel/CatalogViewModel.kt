package com.tripify.tripify_android.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.data.CatalogApi
import com.tripify.tripify_android.data.model.CatalogItemDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val api: CatalogApi
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Tutti")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxPrice = MutableStateFlow(1000f)
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

    private val _catalogList = MutableStateFlow<List<CatalogItem>>(emptyList())
    val catalogList: StateFlow<List<CatalogItem>> = _catalogList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _recommendedItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val recommendedItems: StateFlow<List<CatalogItem>> = _recommendedItems.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchCatalogData(isUserSearch = false)
    }
    fun searchNow() {
        searchJob?.cancel()
        fetchCatalogData(isUserSearch = true)
    }
    fun setCategory(category: String) {
        _selectedCategory.value = category
        fetchCatalogData(isUserSearch = true)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            fetchCatalogData(isUserSearch = true)
        }
    }

    fun searchFlightRoute(departure: String, destination: String) {
        _departure.value = departure
        _destination.value = destination
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

        return when (dto.itemType.uppercase()) {
            "FLIGHT" -> CatalogItem.Flight(
                id = dto.id, title = dto.title, price = priceString, priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                departureAirport = dto.departureAirport ?: "N/D",
                arrivalAirport = dto.arrivalAirport ?: "N/D",
                departureCity = dto.departureCity ?: "N/D",
                arrivalCity = dto.arrivalCity ?: "N/D",
                departureTime = dto.departureTime?.take(10) ?: "Data da def.",
                availableSeats = dto.availableSeats ?: 0,
                stops = dto.stops ?: 0
            )
            "HOTEL" -> CatalogItem.Hotel(
                id = dto.id, title = dto.title, price = "$priceString/notte", priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                address = dto.address ?: "Indirizzo non disponibile",
                city = dto.city ?: "N/D",
                rating = (dto.rating ?: 0).toDouble(),
                roomType = dto.roomType ?: "Camera Standard",
                amenities = dto.amenities ?: emptyList(),
                locationLat = dto.locationLat,
                locationLng = dto.locationLng
            )
            "ACTIVITY" -> CatalogItem.Excursion(
                id = dto.id, title = dto.title, price = priceString, priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                duration = dto.duration ?: "Da definire",
                guideIncluded = dto.guideIncluded ?: false,
                activityType = dto.activityType ?: "Esperienza",
                meetingPoint = dto.meetingPoint ?: "Da definire",
                maxParticipants = dto.maxParticipants
            )
            else -> CatalogItem.Excursion(
                id = dto.id, title = dto.title, price = priceString, priceValue = dto.price.toInt(),
                imageUrls = immaginiReali,
                duration = dto.duration ?: "Da definire",
                guideIncluded = dto.guideIncluded ?: false,
                activityType = dto.activityType ?: "Esperienza",
                meetingPoint = dto.meetingPoint ?: "Da definire",
                maxParticipants = dto.maxParticipants
            )
        }
    }

    private fun fetchCatalogData(isUserSearch: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val dtos = api.searchCatalog(
                    category = _selectedCategory.value,
                    query = _searchQuery.value.trim(),
                    maxPrice = _maxPrice.value.toInt(),
                    minRating = _minRating.value,
                    destination = _destination.value.trim().ifBlank { null },
                    departure = _departure.value.trim().ifBlank { null },
                    guideIncluded = if (_guideOnly.value) true else null,
                    amenities = _selectedAmenities.value.takeIf { it.isNotEmpty() },
                    directOnly = if (_directOnly.value) true else null
                )

                val mappedItems = dtos.map { mapDtoToItem(it) }
                _catalogList.value = mappedItems

                if (isUserSearch) {
                    _hasSearched.value = true
                    fetchRecommendations(category = _selectedCategory.value, excludeIds = mappedItems.map { it.id }.toSet())
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _catalogList.value = emptyList()
                _errorMessage.value = "Impossibile collegarsi al server. Riprova più tardi."
                _recommendedItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }


    private suspend fun fetchRecommendations(category: String, excludeIds: Set<Int>) {
        try {
            if (category == "Tutti") {
                _recommendedItems.value = emptyList()
                return
            }

            val dtos = api.searchCatalog(
                category = category,
                query = "",
                maxPrice = _maxPrice.value.toInt(),
                minRating = 0,
                destination = null,
                departure = null,
                guideIncluded = null,
                amenities = null,
                directOnly = null
            )

            val recommendations = dtos
                .filter { it.id !in excludeIds }
                .map { mapDtoToItem(it) }
                .sortedByDescending {
                    when (it) {
                        is CatalogItem.Hotel -> it.rating
                        else -> 0.0
                    }
                }
                .take(6)

            _recommendedItems.value = recommendations

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
    suspend fun fetchCitySuggestions(query: String): List<String> {
        if (query.trim().length < 2) return emptyList()
        return try {
            api.getCitySuggestions(query.trim())
        } catch (e: Exception) {
            emptyList()
        }
    }
}