package com.tripify.tripify_android.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
            CenterAlignedTopAppBar(
                title = { Text("IMPOSTAZIONI", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 4.sp, color = TripifyDarkGreen) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Indietro") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                SettingsSectionTitle("Preferenze di Viaggio")
                SettingsCard {
                    SettingsSwitchRow(Icons.Default.Straighten, "Sistema Metrico (Km)", useMetricSystem) { viewModel.toggleMetricSystem(it) }
                    HorizontalDivider(color = SfondoPremium)
                    SettingsActionRow(Icons.Default.Payments, "Valuta Predefinita", selectedCurrency) { viewModel.toggleCurrency() }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                SettingsSectionTitle("Avvisi e Notifiche")
                SettingsCard {
                    SettingsSwitchRow(Icons.Default.NotificationsActive, "Promemoria Prenotazioni", notificationsEnabled) { viewModel.toggleNotifications(it) }
                    HorizontalDivider(color = SfondoPremium)
                    SettingsSwitchRow(Icons.Default.ChatBubbleOutline, "Messaggi Chat Host", chatAlertsEnabled) { viewModel.toggleChatAlerts(it) }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                SettingsSectionTitle("Sicurezza Account")
                SettingsCard {
                    SettingsActionRow(Icons.Default.Lock, "Modifica Password", "") { onNavigateToKeycloakAccount() }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { showDeleteDialog = true }, // 👉 Apre il popup!
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Elimina Account Definitivamente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = {
                    Text(text = "Elimina Account", fontWeight = FontWeight.Black, color = TripifyDarkGreen)
                },
                text = {
                    Text("Sei sicuro di voler eliminare definitivamente il tuo account? Questa azione è irreversibile. Perderai tutte le tue prenotazioni e lo storico dei viaggi.", color = Color.DarkGray)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteAccount(onSuccess = { onAccountDeleted() })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Elimina", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Annulla", color = TripifyGreen, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}


@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        color = TripifyDarkGreen,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsSwitchRow(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TripifyDarkGreen, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = TripifyGreen))
    }
}

@Composable
fun SettingsActionRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TripifyDarkGreen, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 8.dp))
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}