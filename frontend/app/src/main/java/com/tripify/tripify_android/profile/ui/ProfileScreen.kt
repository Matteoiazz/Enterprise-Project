package com.tripify.tripify_android.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToCompanions: () -> Unit,
    onNavigateToTravelDocuments: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val logoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.logout()
        onLogoutSuccess()
    }

    LaunchedEffect(viewModel) {
        viewModel.loadUserProfile()
    }

    if (viewModel.isLoggedOut) {
        LaunchedEffect(Unit) {
            onLogoutSuccess()
            viewModel.isLoggedOut = false
        }
    }

    val isLoggedIn = viewModel.name.isNotEmpty() && viewModel.errorMessage == null

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "IL MIO PROFILO",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 4.sp,
                        color = TripifyDarkGreen
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TripifyGreen)
                }
            } else if (!isLoggedIn) {
                GuestProfileView(onNavigateToLogin)
            } else {
                LoggedProfileContent(
                    viewModel = viewModel,
                    onNavigateToCompanions = onNavigateToCompanions,
                    onNavigateToTravelDocuments = onNavigateToTravelDocuments,
                    onNavigateToPaymentMethods = onNavigateToPaymentMethods,
                    onLogoutClick = {
                        coroutineScope.launch {
                            val idToken = viewModel.getIdToken()
                            if (!idToken.isNullOrEmpty()) {
                                val intent = viewModel.getEndSessionIntent(context, idToken)
                                logoutLauncher.launch(intent)
                            } else {
                                viewModel.logout()
                                onLogoutSuccess()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GuestProfileView(onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(TripifyGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = "Lock", modifier = Modifier.size(50.dp), tint = TripifyGreen)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Accedi a Tripify", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Gestisci i tuoi documenti, aggiungi i tuoi compagni di viaggio e velocizza i tuoi pagamenti.",
            textAlign = TextAlign.Center, color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TripifyGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("VAI AL LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun LoggedProfileContent(
    viewModel: ProfileViewModel,
    onNavigateToCompanions: () -> Unit,
    onNavigateToTravelDocuments: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onLogoutClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(TripifyGreen, TripifyDarkGreen))))
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).size(110.dp).clip(CircleShape).background(Color.White).border(4.dp, SfondoPremium, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(TripifyGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(text = viewModel.name.take(1).uppercase() + viewModel.surname.take(1).uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "${viewModel.name} ${viewModel.surname}".trim().ifEmpty { "Utente Tripify" }, fontSize = 24.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen)
            Text(text = viewModel.email, fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                SectionTitle("Il tuo Account")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column {
                        ProfileMenuRow(Icons.Outlined.Badge, "Documenti di Viaggio", hasDivider = true, onClick = onNavigateToTravelDocuments)
                        ProfileMenuRow(Icons.Outlined.Group, "Compagni di Viaggio", hasDivider = true, onClick = onNavigateToCompanions)
                        ProfileMenuRow(Icons.Outlined.AccountBalanceWallet, "Portafoglio e Pagamenti", hasDivider = true, onClick = onNavigateToPaymentMethods)
                        ProfileMenuRow(Icons.Outlined.Settings, "Impostazioni App", hasDivider = false)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Esci dall'account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TripifyDarkGreen, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Start)
}

@Composable
fun ProfileMenuRow(icon: ImageVector, text: String, hasDivider: Boolean, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = TripifyGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TripifyDarkGreen, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Vai", tint = Color.LightGray)
        }
        if (hasDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SfondoPremium, thickness = 1.dp)
        }
    }
}