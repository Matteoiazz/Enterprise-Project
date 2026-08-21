package com.tripify.tripify_android.data.model

/**
 * Rispecchia il payload di Spring Data `Page<T>` restituito da /items/search.
 * Gson ignora i campi extra (pageable, sort, first, empty, ...) che non servono qui.
 */
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val last: Boolean
)
