package com.tripify.tripify_android.data.model

// Passeggero da associare a una riga di prenotazione (booking-service congela
// questi dati così come arrivano, vedi PassengerRequestDTO lato backend).
data class PassengerRequestDTO(
    val firstName: String,
    val lastName: String,
    val taxCode: String,
    val documentType: String,
    val documentNumber: String,
    val documentExpirationDate: String, // "yyyy-MM-dd"
    val issuingCountry: String
)
