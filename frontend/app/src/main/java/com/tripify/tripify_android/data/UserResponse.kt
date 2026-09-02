package com.tripify.tripify_android.data

data class UserResponse(
    val id: String? = null,
    val name: String?,
    val surname: String?,
    val email: String? = null,
    val profilePictureUrl: String?,
    val phone: String?,
    val address: String?,
    val companyName: String?,
    val vatNumber: String?,
    val pec: String?
)