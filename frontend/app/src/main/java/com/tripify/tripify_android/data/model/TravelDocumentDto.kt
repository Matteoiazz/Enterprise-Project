package com.tripify.tripify_android.data.model

data class TravelDocumentDto(
    val id: String? = null,
    val documentType: String,
    val documentNumber: String,
    val expirationDate: String, // "yyyy-MM-dd"
    val issuingCountry: String
)