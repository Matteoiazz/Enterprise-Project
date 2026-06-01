package com.tripify.tripify_android.catalog.model

// Questa data class rappresenta la struttura dei dati che poi vi arriverà dal backend
data class TripMock(
    val id: Int,
    val destinazione: String,
    val prezzo: String,
    val imageUrl: String
)