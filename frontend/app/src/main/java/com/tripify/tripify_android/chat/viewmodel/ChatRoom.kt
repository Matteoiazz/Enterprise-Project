package com.tripify.tripify_android.chat.viewmodel

data class ChatRoom(
    val id: Long,
    val travelerId: Long,
    val hostId: Long,
    val createdAt: String? = null
)