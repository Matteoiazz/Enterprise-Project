package com.tripify.tripify_android.data.model

data class BookingResponseDTO(
    val id: Long,
    val totalAmount: Double,
    val bookingDate: String,
    val status: String,
    val isLeader: Boolean,
    val participantIds: List<String> = emptyList(),
    val lines: List<BookingLineDTO> = emptyList()
)
