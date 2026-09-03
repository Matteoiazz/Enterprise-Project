package com.tripify.tripify_android.data

import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response

/**
 * Parser unico per il corpo di errore del backend. I controller rispondono in
 * tre formati diversi:
 *  - `{"error": "..."}`                         (ReviewController, gateway)
 *  - `{"message": "...", "status": ..., ...}`   (GlobalExceptionHandler di user-auth)
 *  - `{"messages": {"campo": "msg", ...}}`       (alcune validazioni)
 *  - stringa pura                               (ProfileController.handleBadRequest)
 *
 * Prima di questa classe c'erano tre parser leggermente diversi sparsi nei
 * ViewModel, ognuno che ne gestiva solo uno: con gli altri formati l'utente si
 * vedeva il JSON grezzo.
 */
private fun extractMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val json = JSONObject(raw)
        val messages = json.optJSONObject("messages")
        when {
            messages != null && messages.length() > 0 ->
                messages.keys().asSequence().map { messages.getString(it) }.joinToString("\n")
            json.optString("error").isNotBlank() -> json.getString("error")
            json.optString("message").isNotBlank() -> json.getString("message")
            else -> raw.trim().takeIf { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        // non era JSON: e' la stringa pura di ProfileController
        raw.trim().takeIf { it.isNotEmpty() }
    }
}

fun Response<*>.parseApiError(fallback: String): String {
    val raw = try { errorBody()?.string() } catch (e: Exception) { null }
    return extractMessage(raw) ?: fallback
}

fun HttpException.parseApiError(fallback: String): String {
    val raw = try { response()?.errorBody()?.string() } catch (e: Exception) { null }
    return extractMessage(raw) ?: fallback
}
