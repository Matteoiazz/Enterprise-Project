package com.tripify.tripify_android.data.model

data class CreateFareClassRequest(
    val name: String,
    val price: Double,
    val totalSeats: Int
)

data class CreateFlightRequest(
    val title: String,
    val description: String?,
    val price: Double,
    val currency: String = "EUR",
    val category: String = "Voli",
    val departureAirport: String,
    val arrivalAirport: String,
    val departureCity: String,
    val arrivalCity: String,
    val departureTime: String,
    val arrivalTime: String,
    val totalSeats: Int,
    val stops: Int = 0,
    val fareClasses: List<CreateFareClassRequest>
)

data class CreateRoomTypeRequest(
    val name: String,
    val description: String?,
    val price: Double,
    val totalRooms: Int,
    val maxOccupancy: Int?
)

data class CreateHotelRequest(
    val title: String,
    val description: String?,
    val price: Double,
    val currency: String = "EUR",
    val category: String = "Hotel",
    val locationLat: Double,
    val locationLng: Double,
    val address: String,
    val city: String,
    val amenities: List<String> = emptyList(),
    val roomTypes: List<CreateRoomTypeRequest>
)

data class CreateActivityRequest(
    val title: String,
    val description: String?,
    val price: Double,
    val currency: String = "EUR",
    val category: String = "Attività",
    val activityType: String,
    val duration: String,
    val meetingPoint: String?,
    val city: String,
    val maxParticipants: Int?,
    val guideIncluded: Boolean = false
)

data class OrganizerItemDto(
    val id: Int,
    val itemType: String,
    val title: String,
    val price: Double,
    val currency: String,
    @com.google.gson.annotations.SerializedName("active")
    val isActive: Boolean = true
)

data class ReceivedBookingLineDto(
    val bookingId: Long,
    val buyerUserId: String,
    val catalogItemId: Long,
    val quantity: Int?,
    val price: Double,
    val checkIn: String?,
    val checkOut: String?,
    val status: String,
    val bookingDate: String
)
