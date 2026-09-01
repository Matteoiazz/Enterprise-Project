package com.tripify.tripify_android.data.model

data class CreateReviewRequest(
    val rating: Int,
    val comment: String,
    val catalogItemId: Long
)

data class UpdateReviewRequest(
    val rating: Int,
    val comment: String
)

data class ReplyReviewRequest(
    val reply: String
)
