package com.tripify.tripify_android.data.model

data class PaymentMethodDto(
    val id: String? = null,
    val cardProvider: String,
    val cardNumber: String? = null,
    val lastFourDigits: String? = null,
    val expirationMonthYear: String,
    val defaultCard: Boolean = false
) {
    override fun toString(): String =
        "PaymentMethodDto(id=$id, cardProvider=$cardProvider, cardNumber=${if (cardNumber == null) "null" else "***"}, " +
            "lastFourDigits=$lastFourDigits, expirationMonthYear=$expirationMonthYear, defaultCard=$defaultCard)"
}
