package com.tripify.tripify_android.data.model

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val last: Boolean
)
