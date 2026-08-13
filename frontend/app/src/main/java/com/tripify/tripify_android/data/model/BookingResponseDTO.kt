package com.tripify.tripify_android.data.model

import java.time.LocalDateTime

data class BookingResponseDTO(
    val id: Long,
    val totalAmount: Double,
    val bookingDate: String, // O LocalDateTime se usi un serializzatore, stringa è il top per evitare rogne con Retrofit/Gson
    val status: String,
    val isLeader: Boolean // IL CAMPO MAGICO: True se sei il creatore, False se sei un invitato
)