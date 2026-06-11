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

    // --- CAMPI SPECIFICI VOLO ---
    val departureAirport: String?,
    val arrivalAirport: String?,
    val departureTime: String?,
    val arrivalTime: String?,
    val availableSeats: Int?,

    // --- CAMPI SPECIFICI HOTEL ---
    val roomType: String?,
    val availableRooms: Int?,
    val locationLat: Double?,
    val locationLng: Double?
)