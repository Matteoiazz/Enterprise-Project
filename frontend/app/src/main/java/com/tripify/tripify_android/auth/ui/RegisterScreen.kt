package com.tripify.tripify_android.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.auth.viewmodel.RegisterViewModel

// Importiamo i colori del tuo tema
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToCatalog: () -> Unit,
    onNavigateBackToLogin: () -> Unit
) {
    // Osserva il successo: se va a buon fine, spara l'utente nel catalogo
    LaunchedEffect(viewModel.isRegistrationSuccessful) {
        if (viewModel.isRegistrationSuccessful) {
            onNavigateToCatalog()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)) // Sfondo coerente con il Login
            .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // Permette lo scroll se lo schermo è piccolo
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header testuale
            Text(
                text = "Unisciti a Tripify",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TripifyDarkGreen, // Abbinato al tema
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crea un account per iniziare a viaggiare",
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // NOME
                    OutlinedTextField(
                        value = viewModel.firstName,
                        onValueChange = { viewModel.firstName = it },
                        label = { Text("Nome") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Nome", tint = TripifyGreen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TripifyGreen,
                            focusedLabelColor = TripifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // COGNOME
                    OutlinedTextField(
                        value = viewModel.lastName,
                        onValueChange = { viewModel.lastName = it },
                        label = { Text("Cognome") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Cognome", tint = TripifyGreen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TripifyGreen,
                            focusedLabelColor = TripifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // EMAIL
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

                    // PASSWORD
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
                    Spacer(modifier = Modifier.height(16.dp))

                    // CONFERMA PASSWORD
                    OutlinedTextField(
                        value = viewModel.confirmPassword,
                        onValueChange = { viewModel.confirmPassword = it },
                        label = { Text("Conferma Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Conferma", tint = TripifyGreen) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TripifyGreen,
                            focusedLabelColor = TripifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // MESSAGGIO DI ERRORE
                    if (viewModel.errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // BOTTONE REGISTRATI
                    Button(
                        onClick = { viewModel.performRegistration() },
                        colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
                        shape = RoundedCornerShape(12.dp),
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
                            Text("REGISTRATI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // TASTO PER TORNARE INDIETRO
                    Text(
                        text = "Hai già un account? Accedi",
                        color = TripifyDarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateBackToLogin() }
                            .padding(8.dp)
                    )
                }
            }
            // Aggiungiamo uno spazio in fondo per far respirare lo scroll
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}