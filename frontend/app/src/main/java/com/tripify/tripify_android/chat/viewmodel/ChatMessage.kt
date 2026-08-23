package com.tripify.tripify_android.chat.viewmodel

data class ChatMessage(
    val id: Long? = null,
    val roomId: Long,       // <-- Adesso punta alla stanza
    val senderId: Long,
    val content: String,
    val timestamp: String? = null
)