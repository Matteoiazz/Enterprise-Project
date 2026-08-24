package com.tripify.tripify_android.profile.ui

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onSaveProfile: (String, String, String, String, String, String) -> Unit
) {
    // Dati base
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Sicurezza
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // --- LOGICA DI VALIDAZIONE IN TEMPO REALE ---

    val isEmailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email).matches()

    val hasMinLength = newPassword.length >= 8
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val isPasswordValid = newPassword.isBlank() || (hasMinLength && hasUpper && hasDigit && hasSpecial)

    val passwordsMatch = newPassword.isBlank() || newPassword == confirmPassword

    val isFormValid = isEmailValid && isPasswordValid && passwordsMatch

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Profilo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dati Personali", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Cognome") },
                modifier = Modifier.fillMaxWidth()
            )

            // CAMPO EMAIL CON CONTROLLO LIVE
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                isError = !isEmailValid,
                supportingText = {
                    if (!isEmailValid) {
                        Text("Formato email non valido", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefono") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Indirizzo") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Sicurezza", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))

            // CAMPO NUOVA PASSWORD CON CHECKLIST LIVE
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Nuova Password") },
                isError = !isPasswordValid,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = "Mostra/Nascondi password")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (newPassword.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    PasswordRequirement("Almeno 8 caratteri", hasMinLength)
                    PasswordRequirement("Almeno una lettera maiuscola", hasUpper)
                    PasswordRequirement("Almeno un numero", hasDigit)
                    PasswordRequirement("Almeno un carattere speciale", hasSpecial)
                }
            }

            // CAMPO CONFERMA PASSWORD LIVE
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Conferma Nuova Password") },
                isError = !passwordsMatch,
                supportingText = {
                    if (!passwordsMatch) {
                        Text("Le password non coincidono", color = MaterialTheme.colorScheme.error)
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(image, contentDescription = "Mostra/Nascondi password")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // BOTTONE INTELLIGENTE
            Button(
                onClick = {
                    onSaveProfile(name, surname, phone, address, email, newPassword)
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text("Salva Modifiche", color = if (isFormValid) Color.White else Color.DarkGray)
            }
        }
    }
}

// COMPONENTE PER LA CHECKLIST
@Composable
fun PasswordRequirement(text: String, isMet: Boolean) {
    val color = if (isMet) Color(0xFF2E7D32) else Color.Gray
    val icon = if (isMet) Icons.Filled.CheckCircle else Icons.Filled.Cancel

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 12.sp)
    }
}