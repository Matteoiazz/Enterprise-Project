package com.tripify.tripify_android.auth.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

private const val TAG = "TripifyAuth"

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToCatalog: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current

    var hasAttemptedLogin by rememberSaveable { mutableStateOf(false) }

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Risultato ricevuto dal browser: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleAuthorizationResponse(result.data)
        } else {
            Log.w(TAG, "Login annullato o fallito: il browser non ha restituito RESULT_OK (resultCode=${result.resultCode})")
            viewModel.errorMessage = "Login non completato: riprova"
        }
    }

    fun startLogin() {
        viewModel.errorMessage = null
        Log.d(TAG, "Avvio richiesta di autorizzazione verso Keycloak")
        loginLauncher.launch(viewModel.getAuthorizationIntent(context))
    }

    LaunchedEffect(Unit) {
        if (!hasAttemptedLogin) {
            hasAttemptedLogin = true
            startLogin()
        }
    }

    LaunchedEffect(viewModel.isLoginSuccessful) {
        if (viewModel.isLoginSuccessful) {
            onNavigateToCatalog()
            viewModel.isLoginSuccessful = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val error = viewModel.errorMessage
        if (error == null) {
            CircularProgressIndicator(color = CatalogColors.AccentDark, strokeWidth = 3.dp)
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = CatalogColors.InkMuted,
                    modifier = Modifier.height(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Accesso non riuscito",
                    style = CatalogType.LabelStrong,
                    color = CatalogColors.Ink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    style = CatalogType.Body,
                    color = CatalogColors.InkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { startLogin() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CatalogShapes.Pill,
                    colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark)
                ) {
                    Text("Riprova", style = CatalogType.Button)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateToCatalog,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CatalogShapes.Pill
                ) {
                    Text("Continua come ospite", style = CatalogType.Button, color = CatalogColors.AccentDark)
                }
            }
        }
    }
}
