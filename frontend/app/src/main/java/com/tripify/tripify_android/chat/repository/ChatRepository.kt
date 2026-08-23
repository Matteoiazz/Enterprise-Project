package com.tripify.tripify_android.chat.repository

import android.util.Log
import com.google.gson.Gson
import com.tripify.tripify_android.chat.viewmodel.ChatRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ChatRepository {

    private val gson = Gson()
    private const val BASE_URL = "http://172.20.10.2:8084"

    // Funzione che chiama il backend per ottenere o creare la stanza tra viaggiatore e host
    suspend fun getOrCreateChatRoom(travelerId: Long, hostId: Long): ChatRoom? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/chat/room?travelerId=$travelerId&hostId=$hostId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    gson.fromJson(responseJson, ChatRoom::class.java)
                } else {
                    Log.e("ChatRepository", "Errore server: $responseCode")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    suspend fun getUserChatRooms(userId: Long): List<ChatRoom> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/chat/rooms/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : com.google.gson.reflect.TypeToken<List<ChatRoom>>() {}.type
                    gson.fromJson(responseJson, type)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}