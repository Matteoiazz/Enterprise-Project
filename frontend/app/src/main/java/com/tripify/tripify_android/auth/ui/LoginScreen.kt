package com.tripify.tripify_android.auth.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel

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
            Toast.makeText(context, "Login non completato: riprova", Toast.LENGTH_LONG).show()
            onNavigateToCatalog()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAttemptedLogin) {
            hasAttemptedLogin = true
            viewModel.errorMessage = null
            Log.d(TAG, "Avvio richiesta di autorizzazione verso Keycloak")
            val intent = viewModel.getAuthorizationIntent(context)
            loginLauncher.launch(intent)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            Log.e(TAG, "Errore di login: $message")
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(viewModel.isLoginSuccessful) {
        if (viewModel.isLoginSuccessful) {
            onNavigateToCatalog()
            viewModel.isLoginSuccessful = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}