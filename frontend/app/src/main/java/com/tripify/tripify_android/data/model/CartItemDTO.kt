package com.tripify.tripify_android.data.model

data class CartItemDTO(
    val id: Long,
    val catalogItemId: Long,
    val quantity: Int,
    val priceAtAdded: Double
)
