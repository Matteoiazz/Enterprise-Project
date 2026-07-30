package com.tripify.tripify_android.catalog.model

sealed class CatalogItem {
    abstract val id: Int
    abstract val title: String
    abstract val price: String
    abstract val priceValue: Int
    abstract val imageUrls: List<String> // <-- ORA RICEVE LA LISTA VERA!

    // TRUCCHETTO: Questa variabile calcolata permette alle Card vecchie di funzionare
    // senza errori, prendendo in automatico la prima immagine della lista.
    val imageUrl: String
        get() = imageUrls.firstOrNull() ?: "https://picsum.photos/seed/$id/600/800"

    data class Flight(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val departureAirport: String,
        val arrivalAirport: String,
        val departureTime: String,
        val availableSeats: Int
    ) : CatalogItem()

    data class Hotel(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val address: String,
        val rating: Double,
        val roomType: String,
        val locationLat: Double? = null,
        val locationLng: Double? = null
    ) : CatalogItem()

    data class Excursion(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val duration: String,
        val guideIncluded: Boolean
    ) : CatalogItem()
}