package com.tripify.tripify_android.data.model

data class CartDTO(
    val id: Long,
    val items: List<CartItemDTO>,
    val totalAmount: Double
)
