package com.tripify.tripify_android.chat.viewmodel

data class ChatMessage(
    val id: Long? = null,
    val roomId: String,       // <-- Adesso punta alla stanza
    val senderId: String,
    val content: String,
    val timestamp: String? = null
)