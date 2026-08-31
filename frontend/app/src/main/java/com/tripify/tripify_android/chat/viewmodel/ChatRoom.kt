package com.tripify.tripify_android.chat.viewmodel

data class ChatRoom(
    val id: String,
    val travelerId: String,
    val hostId: String,
    val title: String? = null,
    val createdAt: String? = null,
    val unreadCount: Int = 0
)