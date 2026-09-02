package com.tripify.tripify_android.data.model

data class ReviewDto(
    val id: Long? = null,
    val rating: Int,
    val comment: String,
    val travelerId: String? = null,
    val catalogItemId: Long,
    val reply: String? = null,
    val repliedAt: String? = null,
    val helpfulCount: Int = 0,
    val helpfulByMe: Boolean = false
)
