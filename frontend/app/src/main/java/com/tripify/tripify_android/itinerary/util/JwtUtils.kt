package com.tripify.tripify_android.itinerary.util

import android.util.Base64
import org.json.JSONObject

/** Estrae il campo "sub" (UUID Keycloak dell'utente) dal payload di un JWT, senza verificarne la firma. */
fun extractUserIdFromToken(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
        JSONObject(payload).optString("sub", null.toString()).takeIf { it.isNotBlank() && it != "null" }
    } catch (e: Exception) {
        null
    }
}

/** Estrae i ruoli realm ("realm_access": {"roles": [...]}) dal payload di un JWT. */
fun extractRolesFromToken(token: String): List<String> {
    return try {
        val parts = token.split(".")
        if (parts.size != 3) return emptyList()
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
        val realmAccess = JSONObject(payload).optJSONObject("realm_access") ?: return emptyList()
        val roles = realmAccess.optJSONArray("roles") ?: return emptyList()
        (0 until roles.length()).map { roles.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}
