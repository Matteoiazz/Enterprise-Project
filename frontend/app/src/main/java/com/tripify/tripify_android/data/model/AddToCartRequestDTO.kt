package com.tripify.tripify_android.data.model

// Corpo di POST /api/v1/cart/add. roomTypeId/fareClassId/checkIn/checkOut vanno
// valorizzati solo quando l'articolo è rispettivamente una camera d'hotel o un
// posto su un volo (vedi ShoppingCartService.addItem lato booking-service).
data class AddToCartRequestDTO(
    val catalogItemId: Long,
    val quantity: Int,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null, // "yyyy-MM-dd"
    val checkOut: String? = null
)
