package com.tripify.tripify_android.chat.repository

import android.util.Log
import com.google.gson.Gson
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.chat.viewmodel.ChatRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ChatRepository {

    private val gson = Gson()
    private val baseUrl: String
        get() = BuildConfig.BASE_URL

    // Sostituisci solo questa funzione nel tuo ChatRepository
    suspend fun getOrCreateChatRoom(hostId: String, title: String? = null, authToken: String?): ChatRoom? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedTitle = title?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                val urlString = if (encodedTitle.isNotEmpty()) {
                    "${BuildConfig.BASE_URL}/chat/room?hostId=$hostId&title=$encodedTitle"
                } else {
                    "${BuildConfig.BASE_URL}/chat/room?hostId=$hostId"
                }

                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"

                if (!authToken.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                if (connection.responseCode in 200..299) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    com.google.gson.Gson().fromJson(responseJson, ChatRoom::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getUserChatRooms(authToken: String?): List<ChatRoom> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/chat/rooms")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (!authToken.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : com.google.gson.reflect.TypeToken<List<ChatRoom>>() {}.type
                    gson.fromJson(responseJson, type)
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("ChatRepository", "Errore getRooms HTTP $responseCode: $errorStream")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Eccezione getRooms", e)
                emptyList()
            }
        }
    }
    suspend fun markChatAsRead(roomId: String, authToken: String?) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/chat/rooms/$roomId/read")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"

                if (!authToken.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                connection.responseCode // Esegue la chiamata al server
            } catch (e: Exception) {
                Log.e("ChatRepository", "Errore markAsRead", e)
            }
        }
    }
}