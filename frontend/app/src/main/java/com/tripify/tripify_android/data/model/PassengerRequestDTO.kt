package com.tripify.tripify_android.data.model

data class PassengerRequestDTO(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val taxCode: String,
    val documentType: String,
    val documentNumber: String,
    val documentExpirationDate: String,
    val issuingCountry: String
)
