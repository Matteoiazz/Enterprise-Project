package com.tripify.tripify_android.data.model

data class UpdateProfileRequest(
    val name: String? = null,
    val surname: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val email: String? = null,
    val newPassword: String? = null,
    val currentPassword: String? = null
)