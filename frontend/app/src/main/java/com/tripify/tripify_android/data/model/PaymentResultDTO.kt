package com.tripify.tripify_android.data.model

data class PaymentResultDTO(
    val success: Boolean,
    val message: String,
    val bookingId: Long? = null,
    val bookingStatus: String? = null
)
