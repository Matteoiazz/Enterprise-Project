package com.tripify.tripify_android.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel

// Importiamo i tuoi colori custom per mantenere lo stile della Home
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToCatalog: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    // Navigazione automatica se il login ha successo
    LaunchedEffect(viewModel.isLoginSuccessful) {
        if (viewModel.isLoginSuccessful) {
            onNavigateToCatalog()
        }
    }

    // Sfondo coerente con la HomeScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)), // Puoi usare SfondoPremium se preferisci
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header testuale
            Text(
                text = "Bentornato",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = TripifyDarkGreen, // Abbinato al tema
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Accedi per pianificare il tuo prossimo viaggio",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card Form
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // Centra tutto dentro la card
                ) {
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email", tint = TripifyGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TripifyGreen,
                            focusedLabelColor = TripifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password", tint = TripifyGreen) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TripifyGreen,
                            focusedLabelColor = TripifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Gestione Errore
                    if (viewModel.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bottone di Login (CHIUSO CORRETTAMENTE)
                    Button(
                        onClick = { viewModel.performLogin() },
                        colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                        shape = RoundedCornerShape(12.dp), // Angoli leggermente più smussati
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("ACCEDI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } // <-- CHIUSURA DEL BOTTONE

                    Spacer(modifier = Modifier.height(24.dp))

                    // Testo di registrazione correttamente separato dal bottone
                    Text(
                        text = "Non hai un account? Registrati",
                        color = TripifyDarkGreen, // Si abbina al verde scuro dei titoli
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp)) // Rende l'effetto click circolare
                            .clickable { onNavigateToRegister() }
                            .padding(8.dp) // Spazio extra per renderlo più facile da premere
                    )
                }
            }
        }
    }
}