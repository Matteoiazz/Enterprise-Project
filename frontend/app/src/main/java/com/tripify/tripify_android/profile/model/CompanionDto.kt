package com.tripify.tripify_android.profile.model

data class CompanionDto(
    val id: String? = null, // Null quando lo inviamo per crearlo
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String // Formato "YYYY-MM-DD"
)