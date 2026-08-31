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

                // 2. Chiamiamo il repository e assegniamo i dati REALI del server
                val rooms = ChatRepository.getUserChatRooms(authToken = token)
                _chatRooms.value = rooms

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun markAsRead(roomId: String) {
        // Aggiorna subito l'interfaccia (per essere fluidi)
        _chatRooms.value = _chatRooms.value.map { room ->
            if (room.id == roomId) {
                room.copy(unreadCount = 0)
            } else {
                room
            }
        }

        // Manda la richiesta di lettura al backend in background
        viewModelScope.launch {
            try {
                val token = tokenManager.tokenFlow.first()
                if (!token.isNullOrBlank()) {
                    ChatRepository.markChatAsRead(roomId, token)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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