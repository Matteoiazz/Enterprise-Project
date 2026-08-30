package com.tripify.tripify_android.core.navigation

sealed class Route(val path: String) {
    object Home : Route("home")
    object Auth : Route("auth")
    object Booking : Route("booking")
    object Profile : Route("profile")
    object Detail : Route("detail/{itemId}")
    object Bookings : Route("bookings")
    object Cart : Route("cart")
    object Checkout : Route("checkout")
    object AddPassengers : Route("add_passengers/{bookingId}") {
        fun path(bookingId: Long) = "add_passengers/$bookingId"
    }
    object BoardingPass : Route("boarding_pass/{bookingId}") {
        fun path(bookingId: Long) = "boarding_pass/$bookingId"
    }
    object Companions : Route("companions")
    object TravelDocuments : Route("travel_documents")
    object PaymentMethods : Route("payment_methods")
    object SearchResults : Route("search_results")

    object Settings : Route("settings")

    object OrganizerSearch : Route("organizer_search")
    object OrganizerShowcase : Route("organizer_showcase/{hostId}") {
        fun path(hostId: String) = "organizer_showcase/$hostId"
    }

}