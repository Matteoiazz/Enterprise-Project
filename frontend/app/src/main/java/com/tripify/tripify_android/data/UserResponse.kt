package com.tripify.tripify_android.data

data class UserResponse(
    val id: String,
    val name: String?,
    val surname: String?,
    val email: String,
    val profilePictureUrl: String?,
    val phone: String?,
    val address: String?,
    val companyName: String?,
    val vatNumber: String?,
    val pec: String?
)