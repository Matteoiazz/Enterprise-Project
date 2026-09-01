package com.tripify.tripify_android.data.model

data class CartItemDTO(
    val id: Long,
    val catalogItemId: Long,
    val quantity: Int,
    val priceAtAdded: Double,
    // Valuta di priceAtAdded (es. "EUR"/"USD"), congelata quando l'articolo è
    // stato aggiunto al carrello. Null solo per righe salvate prima di questa
    // modifica: in quel caso si assume EUR (vedi CurrencyConverter).
    val currency: String? = null,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null, // "yyyy-MM-dd"
    val checkOut: String? = null
)
