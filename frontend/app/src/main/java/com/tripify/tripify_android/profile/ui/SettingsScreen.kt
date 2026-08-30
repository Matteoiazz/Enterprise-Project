package com.tripify.tripify_android.profile.ui

import android.widget.Toast
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.profile.viewmodel.SettingsViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToKeycloakAccount: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val useMetricSystem by viewModel.useMetricSystem.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val chatAlertsEnabled by viewModel.chatAlertsEnabled.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Prima, se l'eliminazione account falliva lato server, non succedeva
    // visibilmente nulla (solo un log in console): l'utente restava sulla
    // schermata senza sapere se doveva riprovare. Stesso pattern già usato in
    // LoginScreen per viewModel.errorMessage.
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "IMPOSTAZIONI",
                            style = CatalogType.Wordmark,
                            color = CatalogColors.Ink
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = CatalogColors.Ink)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CatalogColors.Surface)
                )
                HorizontalDivider(color = CatalogColors.Hairline)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "hero") {
                SettingsHeroHeader()
            }

            item(key = "settings_card") {
                Box(modifier = Modifier.offset(y = (-32).dp).padding(horizontal = CatalogSpacing.Gutter)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CatalogShapes.Card,
                        colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
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

            item(key = "danger_zone") {
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatalogColors.AlertSoft,
                        contentColor = CatalogColors.Alert
                    ),
                    shape = CatalogShapes.Card,
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CatalogSpacing.Gutter)
                        .offset(y = (-8).dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Elimina Account Definitivamente", style = CatalogType.Button)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = CatalogColors.Surface,
                shape = CatalogShapes.Card,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(CatalogColors.AlertSoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = CatalogColors.Alert,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Elimina Account",
                        style = CatalogType.CardTitle,
                        color = CatalogColors.Ink,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Sei sicuro di voler eliminare definitivamente il tuo account? Questa azione è irreversibile. Perderai l'accesso a tutte le tue prenotazioni, metodi di pagamento e storico dei viaggi.",
                        style = CatalogType.Body,
                        color = CatalogColors.InkMuted,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteAccount(onSuccess = { onAccountDeleted() })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.Alert),
                        shape = CatalogShapes.Field,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Sì, Elimina Definitivamente", style = CatalogType.Button, color = CatalogColors.Surface)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Annulla", style = CatalogType.Button, color = CatalogColors.InkMuted)
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
            .background(CatalogColors.AccentDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset(y = (-20).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PERSONALIZZA LA TUA APP",
                style = CatalogType.Overline,
                color = CatalogColors.Surface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Le tue preferenze",
                style = CatalogType.Hero,
                color = CatalogColors.Surface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = CatalogType.Overline,
        color = CatalogColors.InkSubtle,
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
                    .size(40.dp)
                    .background(CatalogColors.SurfaceMuted, CatalogShapes.Badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = CatalogType.LabelStrong,
                color = CatalogColors.Ink,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CatalogColors.Surface,
                    checkedTrackColor = CatalogColors.Accent,
                    uncheckedThumbColor = CatalogColors.Surface,
                    uncheckedTrackColor = CatalogColors.InkSubtle,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 24.dp),
                color = CatalogColors.Hairline,
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
                    .size(40.dp)
                    .background(CatalogColors.SurfaceMuted, CatalogShapes.Badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = CatalogType.LabelStrong,
                color = CatalogColors.Ink,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = CatalogType.LabelStrong,
                    color = CatalogColors.InkMuted,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CatalogColors.InkSubtle,
                modifier = Modifier.size(22.dp)
            )
        }
        if (hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 24.dp),
                color = CatalogColors.Hairline,
                thickness = 1.dp
            )
        }
    }
}
