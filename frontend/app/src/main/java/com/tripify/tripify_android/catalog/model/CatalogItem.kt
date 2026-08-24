package com.tripify.tripify_android.catalog.model

data class RoomTypeUi(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val totalRooms: Int,
    val maxOccupancy: Int?,
    val benefits: List<String>,
    val imageUrls: List<String>
)

data class FareClassUi(
    val id: Int,
    val name: String,
    val price: Double,
    val totalSeats: Int
)

sealed class CatalogItem {
    abstract val id: Int
    abstract val hostId: String?
    abstract val title: String
    abstract val price: String
    abstract val priceValue: Int
    abstract val imageUrls: List<String>

    val imageUrl: String
        get() = imageUrls.firstOrNull() ?: "https://picsum.photos/seed/$id/600/800"

    data class Flight(
        override val id: Int, override val hostId: String?, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val departureAirport: String,
        val arrivalAirport: String,
        val departureCity: String,
        val arrivalCity: String,
        val departureTime: String,

        val availableSeats: Int,
        val stops: Int,
        val rating: Double? = null,
        val fareClasses: List<FareClassUi> = emptyList()
    ) : CatalogItem() {
        val isDirect: Boolean get() = stops == 0
    }

    data class Hotel(
        override val id: Int, override val hostId: String?, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val address: String,
        val city: String,
        val rating: Double,
        val amenities: List<String>,
        val locationLat: Double? = null,
        val locationLng: Double? = null,
        val roomTypes: List<RoomTypeUi> = emptyList()
    ) : CatalogItem()

    data class Excursion(
        override val id: Int, override val hostId: String?, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrls: List<String>,
        val duration: String,
        val guideIncluded: Boolean,
        val activityType: String,
        val meetingPoint: String,
        val maxParticipants: Int?,
        val rating: Double? = null
    ) : CatalogItem()
}
