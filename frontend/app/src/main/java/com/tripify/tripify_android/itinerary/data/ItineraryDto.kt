package com.tripify.tripify_android.itinerary.data

// roomTypeId/fareClassId/checkIn/checkOut sono valorizzati solo per componenti
// hotel/volo: servono a booking-service per aprire l'hold quando si preme
// "prenota tutto" (vedi ItineraryViewModel.bookAll).
data class FavoriteListItemDto(
    val catalogItemId: Long,
    val quantity: Int = 1,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null, // "yyyy-MM-dd"
    val checkOut: String? = null,
    val activityDate: String? = null,
    val price: java.math.BigDecimal? = null
)

data class FavoriteListDto(
    val id: Long,
    val name: String,
    val ownerId: String,
    val sharedUserIds: List<String> = emptyList(),
    val items: List<FavoriteListItemDto> = emptyList(),
    val visibility: String,
    val publicToken: String?,
    val city: String?,
    val likesCount: Int = 0,
    val bookingsCount: Int = 0,
    val createdAt: String? = null,
    val likedByMe: Boolean = false,
    val totalPrice: java.math.BigDecimal? = null
)

data class CreateListRequest(val name: String)

data class AddListItemRequest(
    val catalogItemId: Long,
    val quantity: Int = 1,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val activityDate: String? = null
)

data class UpdateVisibilityRequest(val visibility: String, val city: String? = null)

data class LikeResponse(val liked: Boolean)

data class BookAllResultDto(val successCount: Int, val total: Int, val errors: List<String> = emptyList())

// alsoRemoved: titoli dei componenti troncati insieme a quello richiesto perché
// non erano più coerenti senza di esso (es. hotel rimasto senza il volo di andata).
data class RemoveItemResultDto(val alsoRemoved: List<String> = emptyList())
