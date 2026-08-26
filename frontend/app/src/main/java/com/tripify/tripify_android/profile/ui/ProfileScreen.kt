package com.tripify.tripify_android.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel
import com.tripify.tripify_android.core.theme.SfondoPremium
import com.tripify.tripify_android.core.theme.TripifyDarkGreen
import com.tripify.tripify_android.core.theme.TripifyGreen
import com.tripify.tripify_android.catalog.ui.theme.CatalogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToCompanions: () -> Unit,
    onNavigateToTravelDocuments: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToSettings: () -> Unit
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

    val isLoggedIn = viewModel.name.isNotEmpty()

    Scaffold(
        containerColor = SfondoPremium,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "PROFILO",
                            style = CatalogType.Wordmark,
                            color = TripifyDarkGreen
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TripifyDarkGreen, strokeWidth = 3.dp)
                }
            } else if (!isLoggedIn) {
                GuestProfileView(onNavigateToLogin)
            } else {
                LoggedProfileContent(
                    viewModel = viewModel,
                    context = context,
                    onNavigateToCompanions = onNavigateToCompanions,
                    onNavigateToTravelDocuments = onNavigateToTravelDocuments,
                    onNavigateToPaymentMethods = onNavigateToPaymentMethods,
                    onNavigateToSettings = onNavigateToSettings,
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
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(TripifyGreen.copy(alpha = 0.08f), CircleShape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LockPerson,
                contentDescription = "Lock",
                modifier = Modifier.fillMaxSize(),
                tint = TripifyDarkGreen
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Accedi al tuo mondo",
            style = CatalogType.DetailTitle,
            color = TripifyDarkGreen,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gestisci i documenti, i compagni di viaggio e velocizza i pagamenti in un unico posto.",
            style = CatalogType.Body,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TripifyDarkGreen),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
        ) {
            Text("ACCEDI O REGISTRATI", style = CatalogType.Button, color = Color.White)
        }
    }
}

@Composable
fun LoggedProfileContent(
    viewModel: ProfileViewModel,
    context: android.content.Context,
    onNavigateToCompanions: () -> Unit,
    onNavigateToTravelDocuments: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val cardOverlap = 32.dp

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(context, it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(TripifyDarkGreen, Color(0xFF0B3023))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-16).dp)
                ) {
                    Box(
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(Color.White, CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.profilePictureUrl != null) {
                                AsyncImage(
                                    model = viewModel.profilePictureUrl,
                                    contentDescription = "Foto Profilo",
                                    modifier = Modifier.size(82.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(82.dp).clip(CircleShape).background(TripifyGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = viewModel.name.take(1).uppercase() + viewModel.surname.take(1).uppercase(),
                                        style = CatalogType.DetailTitle,
                                        color = TripifyDarkGreen
                                    )
                                }
                            }

                            if (viewModel.isUploadingImage) {
                                Box(
                                    modifier = Modifier.size(82.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(28.dp)
                                .background(TripifyDarkGreen, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${viewModel.name} ${viewModel.surname}".trim().ifEmpty { "Utente Tripify" },
                        style = CatalogType.DetailTitle,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.email,
                        style = CatalogType.BodyStrong,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = -cardOverlap),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    ProfileMenuRow(
                        icon = Icons.Outlined.FolderOpen,
                        text = "Documenti di Viaggio",
                        hasDivider = true,
                        onClick = onNavigateToTravelDocuments
                    )
                    ProfileMenuRow(
                        icon = Icons.Outlined.PeopleAlt,
                        text = "Compagni di Viaggio",
                        hasDivider = true,
                        onClick = onNavigateToCompanions
                    )
                    ProfileMenuRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        text = "Portafoglio e Pagamenti",
                        hasDivider = true,
                        onClick = onNavigateToPaymentMethods
                    )
                    ProfileMenuRow(
                        icon = Icons.Outlined.Settings,
                        text = "Impostazioni App",
                        hasDivider = false,
                        onClick = onNavigateToSettings
                    )
                }
            }
        }

        item {
            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = -cardOverlap + 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFF0F0),
                    contentColor = Color(0xFFD14343)
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Esci dall'account",
                    style = CatalogType.Button
                )
            }
        }
    }
}

@Composable
fun ProfileMenuRow(icon: ImageVector, text: String, hasDivider: Boolean, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TripifyGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TripifyDarkGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = CatalogType.LabelStrong,
                color = TripifyDarkGreen,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Vai",
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
        if (hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                color = Color.LightGray.copy(alpha = 0.25f),
                thickness = 1.dp
            )
        }
    }
}