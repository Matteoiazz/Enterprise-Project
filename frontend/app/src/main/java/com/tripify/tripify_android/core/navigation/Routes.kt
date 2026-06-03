package com.tripify.tripify_android.core.navigation

// Una sealed class per avere rotte sicure e auto-completate
sealed class Route(val path: String) {
    object Home : Route("home")
    object Auth : Route("auth") // Qui collegheremo la schermata di Login
    object Booking : Route("booking") // Per le prenotazioni future
}