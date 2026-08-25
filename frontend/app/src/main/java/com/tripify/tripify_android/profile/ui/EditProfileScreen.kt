package com.tripify.tripify_android.profile.ui

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// I tuoi colori core
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onSaveProfile: (String, String, String, String, String, String) -> Unit
) {
    // Dati base (Intatti)
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Sicurezza (Intatti)
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // --- LOGICA DI VALIDAZIONE IN TEMPO REALE (Intatta al 100%) ---
    val isEmailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val hasMinLength = newPassword.length >= 8
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val isPasswordValid = newPassword.isBlank() || (hasMinLength && hasUpper && hasDigit && hasSpecial)
    val passwordsMatch = newPassword.isBlank() || newPassword == confirmPassword
    val isFormValid = isEmailValid && isPasswordValid && passwordsMatch

    val cardOverlap = 32.dp

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "MODIFICA PROFILO",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            color = TripifyDarkGreen
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = TripifyDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // --- HERO HEADER (Stile fluttuante) ---
            item(key = "hero") {
                EditProfileHeroHeader()
            }

            // --- SEZIONE 1: DATI PERSONALI (Card fluttuante) ---
            item(key = "personal_data") {
                Box(modifier = Modifier.offset(y = -cardOverlap).padding(horizontal = 20.dp)) {
                    PremiumCard {
                        Column(modifier = Modifier.padding(24.dp)) {
                            SectionHeader("Dati Personali")

                            PremiumTextField(
                                value = name,
                                label = "Nome",
                                icon = Icons.Default.Person,
                                onValueChange = { name = it }
                            )
                            PremiumTextField(
                                value = surname,
                                label = "Cognome",
                                icon = Icons.Default.Person,
                                onValueChange = { surname = it }
                            )
                            PremiumTextField(
                                value = email,
                                label = "Email",
                                icon = Icons.Default.Mail,
                                isError = !isEmailValid,
                                supportingText = if (!isEmailValid) "Formato non valido" else null,
                                keyboardType = KeyboardType.Email,
                                onValueChange = { email = it }
                            )
                            PremiumTextField(
                                value = phone,
                                label = "Telefono",
                                icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone,
                                onValueChange = { phone = it }
                            )
                            PremiumTextField(
                                value = address,
                                label = "Indirizzo di Residenza",
                                icon = Icons.Default.LocationOn,
                                singleLine = false,
                                onValueChange = { address = it }
                            )
                        }
                    }
                }
            }

            // --- SEZIONE 2: SICUREZZA (Card fluttuante separate) ---
            item(key = "security") {
                Box(modifier = Modifier.offset(y = -cardOverlap + 16.dp).padding(horizontal = 20.dp)) {
                    PremiumCard {
                        Column(modifier = Modifier.padding(24.dp)) {
                            SectionHeader("Sicurezza")

                            // Campo Nuova Password con checklist live integrata
                            PremiumTextField(
                                value = newPassword,
                                label = "Nuova Password (Opzionale)",
                                icon = Icons.Default.Lock,
                                isError = !isPasswordValid,
                                isPassword = true,
                                isVisible = passwordVisible,
                                onVisibilityToggle = { passwordVisible = !passwordVisible },
                                onValueChange = { newPassword = it }
                            )

                            // Checklist integrata elegante
                            if (newPassword.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF4F7F5), RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    Text("La password deve avere:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                                    PasswordRequirement("Almeno 8 caratteri", hasMinLength)
                                    PasswordRequirement("Almeno una lettera maiuscola", hasUpper)
                                    PasswordRequirement("Almeno un numero", hasDigit)
                                    PasswordRequirement("Almeno un carattere speciale", hasSpecial)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Campo Conferma Password live
                            PremiumTextField(
                                value = confirmPassword,
                                label = "Conferma Nuova Password",
                                icon = Icons.Default.Lock,
                                isError = !passwordsMatch,
                                supportingText = if (!passwordsMatch) "Le password non coincidono" else null,
                                isPassword = true,
                                isVisible = confirmPasswordVisible,
                                onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                                onValueChange = { confirmPassword = it }
                            )
                        }
                    }
                }
            }

            // --- BOTTONE INTELLIGENTE PREMIM ---
            item(key = "save_button") {
                Spacer(modifier = Modifier.height(cardOverlap))
                Button(
                    onClick = {
                        onSaveProfile(name, surname, phone, address, email, newPassword)
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TripifyDarkGreen,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
                ) {
                    if (isFormValid) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = "SALVA MODIFICHE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        color = if (isFormValid) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

// --- COMPONENTI PREMIUM RIUTILIZZABILI (DESIGN SYSTEM) ---

@Composable
private fun EditProfileHeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(TripifyDarkGreen, Color(0xFF0B3023))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset(y = (-16).dp), // Compensiamo l'overlap
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AGGIORNA IL TUO PROFILO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dati e Sicurezza",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 20.dp)
    )
}

@Composable
fun PremiumCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun PremiumTextField(
    value: String,
    label: String,
    icon: ImageVector,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isVisible: Boolean = false,
    singleLine: Boolean = true,
    onVisibilityToggle: () -> Unit = {},
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = TripifyDarkGreen, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (isPassword && value.isNotEmpty()) {
                val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onVisibilityToggle) {
                    Icon(image, contentDescription = "Mostra/Nascondi password", tint = TripifyGreen)
                }
            } else if (isError) {
                Icon(Icons.Filled.Cancel, contentDescription = "Errore", tint = MaterialTheme.colorScheme.error)
            }
        },
        isError = isError,
        supportingText = supportingText?.let {
            { Text(it, fontSize = 12.sp) }
        },
        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF4F7F5),
            unfocusedContainerColor = Color(0xFFF4F7F5),
            disabledContainerColor = Color(0xFFF4F7F5),
            errorContainerColor = Color(0xFFFFF0F0),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedTextColor = TripifyDarkGreen,
            unfocusedTextColor = TripifyDarkGreen,
            focusedLabelColor = TripifyGreen,
            unfocusedLabelColor = Color.Gray,
            cursorColor = TripifyGreen
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine
    )
}

// COMPONENTE PER LA CHECKLIST (Ridisegnato Premium)
@Composable
fun PasswordRequirement(text: String, isMet: Boolean) {
    val color = if (isMet) TripifyGreen else Color.Gray.copy(alpha = 0.6f)
    val icon = if (isMet) Icons.Filled.CheckCircle else Icons.Filled.Cancel
    val weight = if (isMet) FontWeight.Bold else FontWeight.Normal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, color = color, fontSize = 13.sp, fontWeight = weight)
    }
}