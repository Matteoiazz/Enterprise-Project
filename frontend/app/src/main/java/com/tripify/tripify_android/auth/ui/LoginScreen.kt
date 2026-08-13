package com.tripify.tripify_android.auth.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToCatalog: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current

    var hasAttemptedLogin by remember { mutableStateOf(false) }

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleAuthorizationResponse(context, result.data)
        } else {

            onNavigateToCatalog()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAttemptedLogin) {
            hasAttemptedLogin = true
            viewModel.errorMessage = null
            val intent = viewModel.getAuthorizationIntent(context)
            loginLauncher.launch(intent)
        }
    }

    LaunchedEffect(viewModel.isLoginSuccessful) {
        if (viewModel.isLoginSuccessful) {
            onNavigateToCatalog()
            viewModel.isLoginSuccessful = false
        }
    }
}