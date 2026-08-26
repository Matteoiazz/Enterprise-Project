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
    val checkOut: String? = null
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
    val likedByMe: Boolean = false
)

data class CreateListRequest(val name: String)

data class AddListItemRequest(
    val catalogItemId: Long,
    val quantity: Int = 1,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null,
    val checkOut: String? = null
)

data class UpdateVisibilityRequest(val visibility: String, val city: String? = null)

data class LikeResponse(val liked: Boolean)

data class BookAllResultDto(val successCount: Int, val total: Int, val errors: List<String> = emptyList())
