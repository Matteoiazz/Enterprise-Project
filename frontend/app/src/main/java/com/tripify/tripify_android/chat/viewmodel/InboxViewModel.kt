package com.tripify.tripify_android.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.chat.repository.ChatRepository
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InboxViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    init {
        loadChatRooms()
    }

    fun loadChatRooms() {
        viewModelScope.launch {
            try {
                // 1. Leggiamo il token JWT reale dal DataStore
                val token = tokenManager.tokenFlow.first()

                // 2. Chiamiamo il repository passando solo il token.
                // Nessun ID finto: il server legge l'UUID dal token!
                val rooms = ChatRepository.getUserChatRooms(authToken = token)
                val demoRooms = rooms.map {
                    if (it.unreadCount == 0) it.copy(unreadCount = 1) else it
                }

                _chatRooms.value = demoRooms

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun markAsRead(roomId: String) {
        _chatRooms.value = _chatRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(unreadCount = 0)
            } else {
                room
            }
        }
    }
}
class InboxViewModelFactory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InboxViewModel(tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}