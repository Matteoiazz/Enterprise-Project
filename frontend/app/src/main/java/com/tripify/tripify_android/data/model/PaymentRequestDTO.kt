package com.tripify.tripify_android.data.model

data class PaymentRequestDTO(
    val bookingId: Long,
    val cardNumber: String,
    val amount: Double
)
