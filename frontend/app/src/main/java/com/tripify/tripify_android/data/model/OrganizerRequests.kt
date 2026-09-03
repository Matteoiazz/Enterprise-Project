package com.tripify.tripify_android.data.model

// Corpi delle richieste di creazione/modifica annuncio: rispecchiano solo i campi
// che un organizzatore può impostare (hostId/isUserGenerated li valorizza il
// backend dal JWT, non vanno mandati dal client).

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
    val departureTime: String, // "yyyy-MM-ddTHH:mm:ss"
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

// Vista minima di un annuncio nella lista "I miei annunci": basta a mostrare la
// riga; per i dettagli completi/modifica si può ancora chiamare getItemById.
// Un annuncio cancellato (soft delete, @SQLRestriction lato catalog-service) non
// compare più qui né altrove: non serve un campo isActive, semplicemente sparisce.
data class OrganizerItemDto(
    val id: Int,
    val itemType: String,
    val title: String,
    val price: Double,
    val currency: String
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
