package com.tripify.tripify_android.core.navigation

// Una sealed class per avere rotte sicure e auto-completate
sealed class Route(val path: String) {
    object Home : Route("home")
    object Auth : Route("auth")
    object Booking : Route("booking")
    object Register : Route("register")
    object Profile : Route("profile")
    object Detail : Route("detail/{itemId}")
    object Bookings : Route("bookings")

    object Companions : Route("companions")
}