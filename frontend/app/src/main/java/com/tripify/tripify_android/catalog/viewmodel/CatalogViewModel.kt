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
    private val api: CatalogApi
) : ViewModel() {

    // --- STATI DEI FILTRI ---
    private val _selectedCategory = MutableStateFlow("Tutti")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxPrice = MutableStateFlow(1000f)
    val maxPrice: StateFlow<Float> = _maxPrice.asStateFlow()

    private val _minRating = MutableStateFlow(0)
    val minRating: StateFlow<Int> = _minRating.asStateFlow()

    // Nuovi stati per i filtri avanzati (BottomSheet)
    private val _directOnly = MutableStateFlow(false)
    private val _guideOnly = MutableStateFlow(false)
    private val _selectedAmenities = MutableStateFlow<List<String>>(emptyList())

    // --- STATI DELLA UI ---
    private val _catalogList = MutableStateFlow<List<CatalogItem>>(emptyList())
    val catalogList: StateFlow<List<CatalogItem>> = _catalogList.asStateFlow()

    // Stato di caricamento per la rotellina!
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchCatalogData()
    }

    // Filtri rapidi dalla UI (Chiamano subito il backend)
    fun setCategory(category: String) { _selectedCategory.value = category; fetchCatalogData() }
    fun updateSearchQuery(query: String) { _searchQuery.value = query; fetchCatalogData() }

    // --- FUNZIONE UNIFICATA PER IL BOTTOM SHEET ---
    // Invece di chiamare il server 5 volte, salviamo tutti gli stati e facciamo UNA singola chiamata
    fun applyAdvancedFilters(price: Float, rating: Int, amenities: List<String>, direct: Boolean, guide: Boolean) {
        _maxPrice.value = price
        _minRating.value = rating
        _selectedAmenities.value = amenities
        _directOnly.value = direct
        _guideOnly.value = guide

        fetchCatalogData()
    }

    // --- IL MOTORE DI RICERCA IBRIDO ---
    private fun fetchCatalogData() {
        viewModelScope.launch {
            _isLoading.value = true // 1. Accendiamo la rotellina di caricamento!

            try {
                // 2. Chiamiamo Spring Boot con i parametri che lui capisce
                val dtos = api.searchCatalog(
                    category = _selectedCategory.value,
                    query = _searchQuery.value.trim(),
                    maxPrice = _maxPrice.value.toInt(),
                    minRating = _minRating.value
                )

                // 3. Mappiamo i DTOs
                var mappedItems = dtos.map { dto ->
                    val priceString = "€ ${dto.price.toInt()}"

                    // Creiamo la lista di immagini: se Spring Boot manda quelle vere, le usiamo.
                    // Altrimenti generiamo 3 immagini mock per il carosello.
                    val immaginiReali = if (!dto.imageUrls.isNullOrEmpty()) {
                        dto.imageUrls
                    } else {
                        listOf(
                            "https://picsum.photos/seed/${dto.id}A/600/800",
                            "https://picsum.photos/seed/${dto.id}B/600/800",
                            "https://picsum.photos/seed/${dto.id}C/600/800"
                        )
                    }

                    when (dto.itemType.uppercase()) {
                        "FLIGHT" -> CatalogItem.Flight(
                            id = dto.id,
                            title = dto.title,
                            price = priceString,
                            priceValue = dto.price.toInt(),
                            imageUrls = immaginiReali, // <-- Usiamo la lista!
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
                            imageUrls = immaginiReali, // <-- Usiamo la lista!
                            address = dto.description ?: "Indirizzo non disponibile",
                            rating = (dto.rating ?: 0).toDouble(),
                            roomType = dto.roomType ?: "Camera Standard"
                        )
                        else -> CatalogItem.Excursion(
                            id = dto.id,
                            title = dto.title,
                            price = priceString,
                            priceValue = dto.price.toInt(),
                            imageUrls = immaginiReali, // <-- Usiamo la lista!
                            duration = dto.description ?: "Da definire",
                            guideIncluded = true // Per ora simuliamo che alcune ce l'abbiano, altre no in base all'id
                        )
                    }
                }

                // 4. FILTRAGGIO LOCALE (Finché Spring Boot non viene aggiornato)
                if (_directOnly.value) {
                    // Escludiamo finti voli con scalo (per ora togliamo quelli con pochi posti come test)
                    mappedItems = mappedItems.filter { it !is CatalogItem.Flight || it.availableSeats > 5 }
                }

                // Mettiamo i risultati nella lista che la UI osserva
                _catalogList.value = mappedItems

            } catch (e: Exception) {
                e.printStackTrace()
                _catalogList.value = emptyList() // Se c'è errore (es. Ngrok spento), svuota la lista
            } finally {
                _isLoading.value = false // 5. Spegniamo la rotellina sia in caso di successo che di errore
            }
        }
    }
}