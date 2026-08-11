package com.tripify.tripify_android.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.data.CatalogApi
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
    private val _guideOnly = MutableStateFlow(false)
    private val _selectedAmenities = MutableStateFlow<List<String>>(emptyList())

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

    private var searchJob: Job? = null

    init {
        fetchCatalogData()
    }

    fun setCategory(category: String) { _selectedCategory.value = category; fetchCatalogData() }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            fetchCatalogData()
        }
    }

    fun searchFlightRoute(departure: String, destination: String) {
        _departure.value = departure
        _destination.value = destination
        fetchCatalogData()
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
        fetchCatalogData()
    }

    fun clearErrorMessage() { _errorMessage.value = null }

    private fun fetchCatalogData() {
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

                val mappedItems = dtos.map { dto ->
                    val priceString = "€ ${dto.price.toInt()}"
                    val immaginiReali = if (!dto.imageUrls.isNullOrEmpty()) dto.imageUrls else listOf(
                        "https://picsum.photos/seed/${dto.id}A/600/800",
                        "https://picsum.photos/seed/${dto.id}B/600/800"
                    )

                    when (dto.itemType.uppercase()) {
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

                _catalogList.value = mappedItems

            } catch (e: Exception) {
                e.printStackTrace()
                _catalogList.value = emptyList()
                _errorMessage.value = "Impossibile collegarsi al server. Riprova più tardi."
            } finally {
                _isLoading.value = false
            }
        }
    }
}