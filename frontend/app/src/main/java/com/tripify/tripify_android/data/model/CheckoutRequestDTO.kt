package com.tripify.tripify_android.data.model

// cartItemIds nullo o vuoto = checkout dell'intero carrello; valorizzato =
// checkout solo di quegli articoli, lasciando gli altri nel carrello.
data class CheckoutRequestDTO(
    val cartItemIds: List<Long>? = null
)
