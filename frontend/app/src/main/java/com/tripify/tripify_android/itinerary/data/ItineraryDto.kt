package com.tripify.tripify_android.itinerary.data

data class FavoriteListDto(
    val id: Long,
    val name: String,
    val ownerId: String,
    val sharedUserIds: List<String> = emptyList(),
    val catalogItemIds: List<Long> = emptyList(),
    val visibility: String,
    val publicToken: String?,
    val city: String?,
    val likesCount: Int = 0,
    val bookingsCount: Int = 0,
    val createdAt: String? = null
)

data class CreateListRequest(val name: String)

data class UpdateVisibilityRequest(val visibility: String, val city: String? = null)

data class LikeResponse(val liked: Boolean)
