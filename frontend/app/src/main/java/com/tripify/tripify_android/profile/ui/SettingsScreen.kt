package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import com.tripify.tripify_android.profile.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToKeycloakAccount: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val useMetricSystem by viewModel.useMetricSystem.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val chatAlertsEnabled by viewModel.chatAlertsEnabled.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "IMPOSTAZIONI",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            color = TripifyDarkGreen
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripifyDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // --- HERO HEADER (Come nella HomeScreen) ---
            item(key = "hero") {
                SettingsHeroHeader()
            }

            // --- MENU IMPOSTAZIONI SOVRAPPOSTO ---
            item(key = "settings_card") {
                Box(modifier = Modifier.offset(y = (-32).dp).padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {

                            // SEZIONE 1: Preferenze
                            SettingsSectionTitle("Preferenze di Viaggio")
                            SettingsSwitchRow(
                                icon = Icons.Default.Straighten,
                                title = "Sistema Metrico (Km)",
                                checked = useMetricSystem,
                                hasDivider = true,
                                onCheckedChange = { viewModel.toggleMetricSystem(it) }
                            )
                            SettingsActionRow(
                                icon = Icons.Default.Payments,
                                title = "Valuta Predefinita",
                                value = selectedCurrency,
                                hasDivider = true,
                                onClick = { viewModel.toggleCurrency() }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // SEZIONE 2: Notifiche
                            SettingsSectionTitle("Avvisi e Notifiche")
                            SettingsSwitchRow(
                                icon = Icons.Default.NotificationsActive,
                                title = "Promemoria Prenotazioni",
                                checked = notificationsEnabled,
                                hasDivider = true,
                                onCheckedChange = { viewModel.toggleNotifications(it) }
                            )
                            SettingsSwitchRow(
                                icon = Icons.Default.ChatBubbleOutline,
                                title = "Messaggi Chat Host",
                                checked = chatAlertsEnabled,
                                hasDivider = true,
                                onCheckedChange = { viewModel.toggleChatAlerts(it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // SEZIONE 3: Sicurezza
                            SettingsSectionTitle("Account e Sicurezza")
                            SettingsActionRow(
                                icon = Icons.Default.ManageAccounts,
                                title = "Gestione Profilo e Credenziali",
                                value = "",
                                hasDivider = false,
                                onClick = { onNavigateToKeycloakAccount() }
                            )
                        }
                    }
                }
            }

            // --- TASTO ELIMINA ACCOUNT ESTRA-CARD ---
            item(key = "danger_zone") {
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF0F0),
                        contentColor = Color(0xFFD14343)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-8).dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Elimina Account Definitivamente", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // --- DIALOG CONFERMA ---
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFFFF0F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFD14343),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Elimina Account",
                        fontWeight = FontWeight.Black,
                        color = TripifyDarkGreen,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Sei sicuro di voler eliminare definitivamente il tuo account? Questa azione è irreversibile. Perderai l'accesso a tutte le tue prenotazioni, metodi di pagamento e storico dei viaggi.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteAccount(onSuccess = { onAccountDeleted() })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD14343)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Sì, Elimina Definitivamente", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Annulla", color = TripifyDarkGreen, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsHeroHeader() {
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
            modifier = Modifier.offset(y = (-20).dp), // Compensiamo l'overlap della card
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PERSONALIZZA LA TUA APP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Le tue preferenze",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = Color.Gray.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 24.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    hasDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(TripifyGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = TripifyDarkGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TripifyDarkGreen,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TripifyGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp, end = 24.dp),
                color = Color.LightGray.copy(alpha = 0.25f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    value: String,
    hasDivider: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current
        ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(TripifyGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = TripifyDarkGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TripifyDarkGreen,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(22.dp)
            )
        }
        if (hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp, end = 24.dp),
                color = Color.LightGray.copy(alpha = 0.25f),
                thickness = 1.dp
            )
        }
    }
}