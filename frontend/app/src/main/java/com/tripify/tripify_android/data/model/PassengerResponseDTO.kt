package com.tripify.tripify_android.data.model

// Vista di un passeggero già registrato, con il suo "biglietto": qrCodeData
// resta null finché il check-in non si apre per quella riga (vedi
// CheckInService lato backend), quindi va sempre trattato come opzionale.
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
