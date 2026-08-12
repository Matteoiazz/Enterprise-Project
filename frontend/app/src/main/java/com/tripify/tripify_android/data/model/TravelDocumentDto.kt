package com.tripify.tripify_android.data.model // Adatta il package al tuo progetto

import java.time.LocalDate
import java.util.UUID

data class TravelDocumentDto(
    val id: String? = null,
    val documentType: String,
    val documentNumber: String,
    val expirationDate: String, // "yyyy-MM-dd"
    val issuingCountry: String
)