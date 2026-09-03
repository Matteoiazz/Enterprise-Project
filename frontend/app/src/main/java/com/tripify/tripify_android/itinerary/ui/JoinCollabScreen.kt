package com.tripify.tripify_android.itinerary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel
import kotlinx.coroutines.flow.first

@Composable
private fun JoinStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(CatalogColors.SurfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = CatalogType.Caption.copy(fontWeight = FontWeight.Bold), color = CatalogColors.AccentDark)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, style = CatalogType.Body, color = CatalogColors.InkMuted, textAlign = TextAlign.Start)
    }
}

private sealed class JoinState {
    data object Checking : JoinState()
    data object NeedsLogin : JoinState()
    data class Joined(val listId: Long) : JoinState()
    data class Error(val message: String) : JoinState()
}

/** Schermata di transito per il link di invito a collaborare: verifica il login, accetta l'invito, poi passa al dettaglio. */
@Composable
fun JoinCollabScreen(
    token: String,
    viewModel: ItineraryViewModel,
    tokenManager: TokenManager,
    onJoined: (listId: Long) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var state by remember(token) { mutableStateOf<JoinState>(JoinState.Checking) }

    LaunchedEffect(token) {
        val hasToken = !tokenManager.tokenFlow.first().isNullOrBlank()
        if (!hasToken) {
            state = JoinState.NeedsLogin
            return@LaunchedEffect
        }
        viewModel.joinAsCollaborator(token) { listId, error ->
            state = if (listId != null) JoinState.Joined(listId) else JoinState.Error(error ?: "Invito non valido")
        }
    }

    LaunchedEffect(state) {
        (state as? JoinState.Joined)?.let { onJoined(it.listId) }
    }

    Scaffold(containerColor = CatalogColors.Background) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is JoinState.Checking, is JoinState.Joined -> {
                    CircularProgressIndicator(color = CatalogColors.AccentDark)
                }
                is JoinState.NeedsLogin -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(CatalogColors.AccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Accedi per unirti a questo itinerario",
                            style = CatalogType.Section, color = CatalogColors.Ink, textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        JoinStep(number = "1", text = "Accedi con il tuo account Tripify.")
                        Spacer(modifier = Modifier.height(10.dp))
                        JoinStep(number = "2", text = "Poi riapri questo stesso link: da loggato ti aggiungerà subito come collaboratore.")
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Accedi", color = Color.White, style = CatalogType.Button)
                        }
                    }
                }
                is JoinState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, style = CatalogType.Body, color = CatalogColors.InkMuted, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onNavigateBack) {
                            Text("Torna indietro", style = CatalogType.LabelStrong, color = CatalogColors.AccentDark)
                        }
                    }
                }
            }
        }
    }
}
