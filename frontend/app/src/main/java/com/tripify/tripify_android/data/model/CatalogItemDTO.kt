package com.tripify.tripify_android.data.model

data class CatalogItemDto(
    val id: Int,
    val title: String,
    val description: String?,
    val price: Double,
    val currency: String?,
    val itemType: String,
    val category: String?,
    val rating: Int?,
    val imageUrls: List<String>?,

    // --- CAMPI SPECIFICI VOLO ---
    val departureAirport: String?,
    val arrivalAirport: String?,
    val departureCity: String?,
    val arrivalCity: String?,
    val departureTime: String?,
    val arrivalTime: String?,
    val availableSeats: Int?,
    val stops: Int?,

    // --- CAMPI SPECIFICI HOTEL ---
    val roomType: String?,
    val availableRooms: Int?,
    val locationLat: Double?,
    val locationLng: Double?,
    val address: String?,
    val city: String?,
    val amenities: List<String>?,

    // --- CAMPI SPECIFICI ATTIVITÀ ---
    val activityType: String?,
    val duration: String?,
    val meetingPoint: String?,
    val maxParticipants: Int?,
    val guideIncluded: Boolean?
)