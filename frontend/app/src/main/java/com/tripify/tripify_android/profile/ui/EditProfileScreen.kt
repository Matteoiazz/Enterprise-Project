package com.tripify.tripify_android.profile.ui

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onSaveProfile: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

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
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "MODIFICA PROFILO",
                            style = CatalogType.Wordmark,
                            color = CatalogColors.Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "hero") {
                EditProfileHeroHeader()
            }

            item(key = "personal_data") {
                Box(modifier = Modifier.offset(y = -cardOverlap).padding(horizontal = CatalogSpacing.Gutter)) {
                    ProfileContentCard {
                        Column(modifier = Modifier.padding(24.dp)) {
                            ProfileSectionHeader("Dati Personali")

                            ProfileOutlinedTextField(
                                value = name,
                                label = "Nome",
                                icon = Icons.Default.Person,
                                onValueChange = { name = it }
                            )
                            ProfileOutlinedTextField(
                                value = surname,
                                label = "Cognome",
                                icon = Icons.Default.Person,
                                onValueChange = { surname = it }
                            )
                            ProfileOutlinedTextField(
                                value = email,
                                label = "Email",
                                icon = Icons.Default.Mail,
                                isError = !isEmailValid,
                                supportingText = if (!isEmailValid) "Formato non valido" else null,
                                keyboardType = KeyboardType.Email,
                                onValueChange = { email = it }
                            )
                            ProfileOutlinedTextField(
                                value = phone,
                                label = "Telefono",
                                icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone,
                                onValueChange = { phone = it }
                            )
                            ProfileOutlinedTextField(
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

            item(key = "security") {
                Box(modifier = Modifier.offset(y = -cardOverlap + 16.dp).padding(horizontal = CatalogSpacing.Gutter)) {
                    ProfileContentCard {
                        Column(modifier = Modifier.padding(24.dp)) {
                            ProfileSectionHeader("Sicurezza")

                            ProfileOutlinedTextField(
                                value = newPassword,
                                label = "Nuova Password (Opzionale)",
                                icon = Icons.Default.Lock,
                                isError = !isPasswordValid,
                                isPassword = true,
                                isVisible = passwordVisible,
                                onVisibilityToggle = { passwordVisible = !passwordVisible },
                                onValueChange = { newPassword = it }
                            )

                            if (newPassword.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CatalogColors.SurfaceMuted, CatalogShapes.Field)
                                        .padding(16.dp)
                                ) {
                                    Text("La password deve avere:", style = CatalogType.LabelStrong, color = CatalogColors.InkMuted, modifier = Modifier.padding(bottom = 8.dp))
                                    PasswordRequirement("Almeno 8 caratteri", hasMinLength)
                                    PasswordRequirement("Almeno una lettera maiuscola", hasUpper)
                                    PasswordRequirement("Almeno un numero", hasDigit)
                                    PasswordRequirement("Almeno un carattere speciale", hasSpecial)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            ProfileOutlinedTextField(
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

            item(key = "save_button") {
                Spacer(modifier = Modifier.height(cardOverlap))
                Button(
                    onClick = {
                        onSaveProfile(name, surname, phone, address, email, newPassword)
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CatalogSpacing.Gutter)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogColors.AccentDark,
                        disabledContainerColor = CatalogColors.SurfaceMuted,
                        disabledContentColor = CatalogColors.InkSubtle
                    ),
                    shape = CatalogShapes.Pill,
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    if (isFormValid) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CatalogColors.Surface)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = "SALVA MODIFICHE",
                        style = CatalogType.Button
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileHeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(CatalogColors.AccentDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset(y = (-16).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AGGIORNA IL TUO PROFILO",
                style = CatalogType.Overline,
                color = CatalogColors.Surface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dati e Sicurezza",
                style = CatalogType.Hero,
                color = CatalogColors.Surface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = CatalogType.Overline,
        color = CatalogColors.InkMuted,
        modifier = Modifier.padding(bottom = 20.dp)
    )
}

@Composable
fun ProfileContentCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CatalogShapes.Card,
        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun ProfileOutlinedTextField(
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = CatalogType.Label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (isPassword && value.isNotEmpty()) {
                val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onVisibilityToggle) {
                    Icon(image, contentDescription = "Mostra/Nascondi password", tint = CatalogColors.Accent)
                }
            } else if (isError) {
                Icon(Icons.Filled.Cancel, contentDescription = "Errore", tint = CatalogColors.Alert)
            }
        },
        isError = isError,
        supportingText = supportingText?.let {
            { Text(it, style = CatalogType.Caption) }
        },
        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CatalogColors.Surface,
            unfocusedContainerColor = CatalogColors.Surface,
            disabledContainerColor = CatalogColors.SurfaceMuted,
            errorContainerColor = CatalogColors.AlertSoft,
            focusedBorderColor = CatalogColors.Accent,
            unfocusedBorderColor = CatalogColors.Hairline,
            errorBorderColor = CatalogColors.Alert,
            focusedTextColor = CatalogColors.Ink,
            unfocusedTextColor = CatalogColors.Ink,
            focusedLabelColor = CatalogColors.Accent,
            unfocusedLabelColor = CatalogColors.InkMuted,
            cursorColor = CatalogColors.Accent
        ),
        textStyle = CatalogType.BodyStrong,
        shape = CatalogShapes.Field,
        singleLine = singleLine
    )
}

@Composable
fun PasswordRequirement(text: String, isMet: Boolean) {
    val color = if (isMet) CatalogColors.Accent else CatalogColors.InkSubtle
    val icon = if (isMet) Icons.Filled.CheckCircle else Icons.Filled.Cancel

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
        Text(text = text, color = color, style = if (isMet) CatalogType.LabelStrong else CatalogType.Label)
    }
}