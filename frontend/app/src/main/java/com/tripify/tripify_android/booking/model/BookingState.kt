package com.tripify.tripify_android.booking.model

import com.tripify.tripify_android.data.model.BookingResponseDTO

sealed class BookingState {
    object Loading : BookingState()
    data class Success(val bookings: List<BookingResponseDTO>) : BookingState()
    data class Error(val message: String) : BookingState()
    // Non e' un errore del server: l'utente non ha ancora effettuato il login,
    // quindi non c'e' nessun token da mandare (vedi BookingViewModel.fetchUserBookings).
    object NotLoggedIn : BookingState()
}