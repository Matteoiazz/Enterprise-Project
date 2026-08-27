package com.tripify.tripify_android.data.model

// Esattamente uno tra cardNumber (nuova carta) e paymentMethodId (un metodo
// già salvato in Impostazioni) va valorizzato: vedi CartViewModel.payWithNewCard/payWithSavedMethod.
data class PaymentRequestDTO(
    val bookingId: Long,
    val amount: Double,
    val cardNumber: String? = null,
    val paymentMethodId: String? = null
)
