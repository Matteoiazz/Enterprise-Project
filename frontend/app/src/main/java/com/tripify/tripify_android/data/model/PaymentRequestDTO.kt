package com.tripify.tripify_android.data.model

data class PaymentRequestDTO(
    val bookingId: Long,
    val amount: Double,
    val cardNumber: String? = null,
    val paymentMethodId: String? = null
)
