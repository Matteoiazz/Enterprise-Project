package com.tripify.tripify_android.catalog.model

sealed class CatalogItem {
    abstract val id: Int
    abstract val title: String
    abstract val price: String
    abstract val priceValue: Int
    abstract val imageUrl: String

    data class Flight(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrl: String,
        val departureAirport: String,
        val arrivalAirport: String,
        val departureTime: String,
        val availableSeats: Int
    ) : CatalogItem()

    data class Hotel(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrl: String,
        val address: String,
        val rating: Double,
        val roomType: String
    ) : CatalogItem()

    data class Excursion(
        override val id: Int, override val title: String, override val price: String,
        override val priceValue: Int, override val imageUrl: String,
        val duration: String,
        val guideIncluded: Boolean
    ) : CatalogItem()
}