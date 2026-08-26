package com.tripify.tripify_android.data.model

// Non c'è più userId: il carrello restituito da GET /api/v1/cart è sempre e
// solo quello dell'utente autenticato (identificato dal backend via JWT), non
// serve più specificarlo né leggerlo qui.
data class CartDTO(
    val id: Long,
    val items: List<CartItemDTO>,
    val totalAmount: Double
)
