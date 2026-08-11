package com.tripify.tripify_android.catalog.model

sealed class CatalogItem {
    abstract val id: Int
    abstract val title: String
    abstract val price: String
    abstract val priceValue: Int
    abstract val imageUrls: List<String>

    val imageUrl: String
        get() = imageUrls.firstOrNull() ?: "https://picsum.photos/seed/$id/600/800"

    data class Flight(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val departureAirport: String,
        val arrivalAirport: String,
        val departureCity: String,
        val arrivalCity: String,
        val departureTime: String,
        val availableSeats: Int,
        val stops: Int
    ) : CatalogItem() {
        val isDirect: Boolean get() = stops == 0
    }

    data class Hotel(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val address: String,
        val city: String,
        val rating: Double,
        val roomType: String,
        val amenities: List<String>,
        val locationLat: Double? = null,
        val locationLng: Double? = null
    ) : CatalogItem()

    data class Excursion(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val duration: String,
        val guideIncluded: Boolean,
        val activityType: String,
        val meetingPoint: String,
        val maxParticipants: Int?
    ) : CatalogItem()
}