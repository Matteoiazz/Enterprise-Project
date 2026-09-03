package com.tripify.tripify_android.data.model

data class RoomTypeDto(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val totalRooms: Int,
    val maxOccupancy: Int?,
    val benefits: List<String>?,
    val imageUrls: List<String>?
)
