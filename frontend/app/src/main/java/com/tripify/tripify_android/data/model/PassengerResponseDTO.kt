package com.tripify.tripify_android.data.model

data class PassengerResponseDTO(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val taxCode: String,
    val documentType: String,
    val documentNumber: String,
    val qrCodeData: String?,
    val checkedIn: Boolean
)
