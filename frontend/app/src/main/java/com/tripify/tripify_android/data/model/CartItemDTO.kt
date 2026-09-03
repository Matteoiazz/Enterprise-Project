package com.tripify.tripify_android.data.model

data class CartItemDTO(
    val id: Long,
    val catalogItemId: Long,
    val quantity: Int,
    val priceAtAdded: Double,

    val currency: String? = null,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null,
    val checkOut: String? = null
)
