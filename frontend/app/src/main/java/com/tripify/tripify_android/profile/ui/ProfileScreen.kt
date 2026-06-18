package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutSuccess: () -> Unit
) {
    // 1. CARICA I DATI DAL DATABASE APPENA SI APRE LA SCHERMATA
    LaunchedEffect(viewModel) {
        viewModel.loadUserProfile()
    }
    if (viewModel.isLoggedOut) {
        LaunchedEffect(Unit) {
            onLogoutSuccess()
            viewModel.isLoggedOut = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotellina di caricamento mentre aspetta il backend
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Spacer(modifier = Modifier.height(40.dp))

            // Avatar circolare
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Profilo",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. DATI DINAMICI! Legge direttamente dal ViewModel
            Text(
                text = "${viewModel.name} ${viewModel.surname}".trim().ifEmpty { "Utente Tripify" },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = viewModel.email.ifEmpty { "Caricamento in corso..." },
                fontSize = 16.sp,
                color = Color.Gray
            )

            // Messaggio di errore se il backend fallisce
            if (viewModel.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Menu Opzioni
            ProfileMenuButton(
                icon = Icons.Default.Settings,
                text = "Impostazioni Account",
                onClick = { /* In futuro: Naviga alle impostazioni */ }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. TASTO LOGOUT CHE CHIAMA IL VIEWMODEL
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout")
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Esci dall'account", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Componente riutilizzabile per i bottoni del menu
@Composable
fun ProfileMenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}