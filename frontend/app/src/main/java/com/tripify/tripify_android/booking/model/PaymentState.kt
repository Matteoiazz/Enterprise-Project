package com.tripify.tripify_android.booking.model

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val bookingId: Long) : PaymentState()
    data class Error(val message: String) : PaymentState()
}
