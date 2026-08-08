package com.tripify.tripify_android.data.model

data class CartDTO(
    val id: Long,
    val userId: String,
    val items: List<CartItemDTO>
)