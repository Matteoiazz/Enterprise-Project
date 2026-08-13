package com.tripify.tripify_android.core.navigation

sealed class Route(val path: String) {
    object Home : Route("home")
    object Auth : Route("auth")
    object Booking : Route("booking")
    object Profile : Route("profile")
    object Detail : Route("detail/{itemId}")
    object Bookings : Route("bookings")
    object Companions : Route("companions")
    object TravelDocuments : Route("travel_documents")
    object PaymentMethods : Route("payment_methods")
}