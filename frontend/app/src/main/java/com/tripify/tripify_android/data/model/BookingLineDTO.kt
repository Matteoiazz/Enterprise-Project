package com.tripify.tripify_android.data.model

data class BookingLineDTO(
    val id: Long,
    val catalogItemId: Long,
    val price: Double,
    val quantity: Int? = null,
    val roomTypeId: Long? = null,
    val fareClassId: Long? = null,
    val checkIn: String? = null, // "yyyy-MM-dd"
    val checkOut: String? = null,
    val passengerCount: Int = 0
)
