package com.tripify.tripify_android.chat.viewmodel

data class ChatMessage(
    val id: Long? = null,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val timestamp: String? = null
)