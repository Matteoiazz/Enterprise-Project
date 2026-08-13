package com.tripify.tripify_android.data.model

data class PaymentMethodDto(
    val id: String? = null,
    val cardProvider: String,
    val cardNumber: String? = null,
    val lastFourDigits: String? = null,
    val expirationMonthYear: String
)