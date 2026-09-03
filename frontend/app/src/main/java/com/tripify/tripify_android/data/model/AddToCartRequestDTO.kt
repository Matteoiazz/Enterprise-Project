package com.tripify.tripify_android.data.model

data class AddToCartRequestDTO(
    val catalogItemId: Long,
    val quantity: Int,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null,
    val checkOut: String? = null
)
