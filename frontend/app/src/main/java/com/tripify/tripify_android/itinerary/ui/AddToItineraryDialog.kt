package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import kotlinx.coroutines.launch

/** Piccolo dialog per aggiungere un CatalogItem a una delle proprie liste/itinerari. */
@Composable
fun AddToItineraryDialog(
    catalogItemId: Int,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val api = remember { com.tripify.tripify_android.itinerary.data.ItineraryRetrofit.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var lists by remember { mutableStateOf<List<FavoriteListDto>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = api.getMyLists()
            lists = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            errorMessage = "Impossibile caricare le tue liste"
            lists = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi a un itinerario", style = CatalogType.Section, color = CatalogColors.Ink) },
        text = {
            when {
                errorMessage != null -> Text(errorMessage!!, style = CatalogType.Body, color = CatalogColors.InkMuted)
                lists == null -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CatalogColors.AccentDark, modifier = Modifier.size(28.dp))
                }
                lists!!.isEmpty() -> Text(
                    "Non hai ancora nessuna lista. Creane una dalla tab Itinerari.",
                    style = CatalogType.Body, color = CatalogColors.InkMuted
                )
                else -> Column {
                    lists!!.forEach { list ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        try {
                                            val response = api.addItem(list.id, catalogItemId.toLong())
                                            if (response.isSuccessful) onAdded()
                                        } catch (e: Exception) {
                                            errorMessage = "Impossibile aggiungere l'elemento"
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(list.name, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
                        }
                        HorizontalDivider(color = CatalogColors.Hairline)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi", color = CatalogColors.InkMuted) }
        }
    )
}
