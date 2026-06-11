package com.tripify.tripify_android.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.data.CatalogApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val api: CatalogApi // <-- Riceviamo l'API di Retrofit dal MainActivity
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Tutti")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxPrice = MutableStateFlow(1000f)
    val maxPrice: StateFlow<Float> = _maxPrice.asStateFlow()

    private val _minRating = MutableStateFlow(0)
    val minRating: StateFlow<Int> = _minRating.asStateFlow()

    // Partiamo con una lista vuota finché il server non risponde
    private val _catalogList = MutableStateFlow<List<CatalogItem>>(emptyList())
    val catalogList: StateFlow<List<CatalogItem>> = _catalogList.asStateFlow()

    init {
        // Appena il ViewModel nasce, fa la prima chiamata al backend
        fetchCatalogData()
    }

    // Quando un filtro cambia, ri-eseguiamo la chiamata al server!
    fun setCategory(category: String) { _selectedCategory.value = category; fetchCatalogData() }
    fun updateSearchQuery(query: String) { _searchQuery.value = query; fetchCatalogData() }
    fun updateMaxPrice(price: Float) { _maxPrice.value = price; fetchCatalogData() }
    fun updateMinRating(rating: Int) { _minRating.value = rating; fetchCatalogData() }

    // --- IL MOTORE DI RICERCA REALE CON SPRING BOOT ---
    private fun fetchCatalogData() {
        viewModelScope.launch {
            try {
                // Facciamo la chiamata HTTP passando i filtri correnti
                val dtos = api.searchCatalog(
                    category = _selectedCategory.value,
                    query = _searchQuery.value.trim(),
                    maxPrice = _maxPrice.value.toInt(),
                    minRating = _minRating.value
                )

                // Mappiamo i DTO che arrivano dal backend sulle nostre Card grafiche
                val mappedItems = dtos.map { dto ->
                    val priceString = "€ ${dto.price.toInt()}"

                    when (dto.itemType.uppercase()) {
                        "VOLO" -> CatalogItem.Flight(
                            id = dto.id,
                            title = dto.title,
                            price = priceString,
                            priceValue = dto.price.toInt(),
                            imageUrl = "https://picsum.photos/seed/${dto.id}/600/800",
                            // Usiamo l'Elvis operator (?:) per evitare crash se il server manda null
                            departureAirport = dto.departureAirport ?: "N/D",
                            arrivalAirport = dto.arrivalAirport ?: "N/D",
                            departureTime = dto.departureTime?.take(10) ?: "Data da def.",
                            availableSeats = dto.availableSeats ?: 0
                        )
                        "HOTEL" -> CatalogItem.Hotel(
                            id = dto.id,
                            title = dto.title,
                            price = "$priceString/notte",
                            priceValue = dto.price.toInt(),
                            imageUrl = "https://picsum.photos/seed/${dto.id}/600/800",
                            address = dto.description ?: "Indirizzo non disponibile",
                            rating = (dto.rating ?: 0).toDouble(),
                            roomType = dto.roomType ?: "Camera Standard"
                        )
                        else -> CatalogItem.Excursion(
                            id = dto.id,
                            title = dto.title,
                            price = priceString,
                            priceValue = dto.price.toInt(),
                            imageUrl = "https://picsum.photos/seed/${dto.id}/600/800",
                            duration = dto.description ?: "Da definire",
                            guideIncluded = true
                        )
                    }
                }

                // Aggiorniamo la UI!
                _catalogList.value = mappedItems

            } catch (e: Exception) {
                // Se Spring Boot è spento o c'è un errore, per ora stampiamo l'errore ed evitiamo crash
                e.printStackTrace()
                _catalogList.value = emptyList()
            }
        }
    }
}