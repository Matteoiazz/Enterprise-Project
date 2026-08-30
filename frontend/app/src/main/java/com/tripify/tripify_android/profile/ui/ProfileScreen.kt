package com.tripify.tripify_android.profile.ui

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.util.extractRolesFromToken
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
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
    onNavigateToSettings: () -> Unit,
    onNavigateToOrganizer: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tokenManager = remember { TokenManager(context) }
    val currentToken by tokenManager.tokenFlow.collectAsState(initial = null)
    val isOrganizer = currentToken?.let { extractRolesFromToken(it) }?.contains("ROLE_ORGANIZER") == true

    val logoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.logout()
        onLogoutSuccess()
    }

    LaunchedEffect(viewModel) {
        viewModel.loadUserProfile()
    }

    // Prima gli errori di loadUserProfile/uploadProfilePicture/updateProfile finivano
    // solo in Logcat: se il caricamento del profilo falliva o l'upload della foto non
    // andava a buon fine, l'utente non aveva nessun avviso e non capiva perché la
    // schermata restava vuota o la foto non cambiava. Stesso pattern già usato in
    // SettingsScreen per viewModel.errorMessage.
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    if (viewModel.isLoggedOut) {
        LaunchedEffect(Unit) {
            onLogoutSuccess()
            viewModel.isLoggedOut = false
        }
    }
    // isLoggedIn dipende solo dal token: prima richiedeva anche name.isNotEmpty(), quindi
    // se loadUserProfile() falliva per un problema temporaneo (rete instabile, server
    // lento) un utente comunque autenticato si ritrovava catapultato sulla schermata
    // "Accedi al tuo mondo" come se avesse fatto logout, pur avendo ancora una sessione
    // valida. Il caso "token cancellato" (logout o eliminazione account) resta comunque
    // coperto, dato che currentToken passa a null in entrambi i casi.
    val isLoggedIn = currentToken != null

    Scaffold(
        containerColor = CatalogColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "PROFILO",
                            style = CatalogType.Wordmark,
                            color = CatalogColors.Ink
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = CatalogColors.Surface
                    )
                )
                HorizontalDivider(color = CatalogColors.Hairline)
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
                    CircularProgressIndicator(color = CatalogColors.AccentDark, strokeWidth = 3.dp)
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
                    isOrganizer = isOrganizer,
                    onNavigateToOrganizer = onNavigateToOrganizer,
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
            .padding(horizontal = CatalogSpacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(CatalogColors.AccentSoft, CircleShape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LockPerson,
                contentDescription = "Lock",
                modifier = Modifier.fillMaxSize(),
                tint = CatalogColors.AccentDark
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Accedi al tuo mondo",
            style = CatalogType.DetailTitle,
            color = CatalogColors.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gestisci i documenti, i compagni di viaggio e velocizza i pagamenti in un unico posto.",
            style = CatalogType.Body,
            textAlign = TextAlign.Center,
            color = CatalogColors.InkMuted
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
            shape = CatalogShapes.Pill,
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text("ACCEDI O REGISTRATI", style = CatalogType.Button, color = CatalogColors.Surface)
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
    isOrganizer: Boolean = false,
    onNavigateToOrganizer: () -> Unit = {},
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
                    .background(CatalogColors.AccentDark),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-16).dp)
                ) {
                    Box(modifier = Modifier.size(90.dp)) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(CatalogColors.Surface, CircleShape)
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
                                    modifier = Modifier.size(82.dp).clip(CircleShape).background(CatalogColors.AccentSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = viewModel.name.take(1).uppercase() + viewModel.surname.take(1).uppercase(),
                                        style = CatalogType.DetailTitle,
                                        color = CatalogColors.AccentDark
                                    )
                                }
                            }

                            if (viewModel.isUploadingImage) {
                                Box(
                                    modifier = Modifier.size(82.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = CatalogColors.Surface, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(28.dp)
                                .background(CatalogColors.Accent, CircleShape)
                                .border(2.dp, CatalogColors.Surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = CatalogColors.Surface, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${viewModel.name} ${viewModel.surname}".trim().ifEmpty { "Utente Tripify" },
                        style = CatalogType.DetailTitle,
                        color = CatalogColors.Surface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.email,
                        style = CatalogType.BodyStrong,
                        color = CatalogColors.Surface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CatalogSpacing.Gutter)
                    .offset(y = -cardOverlap),
                shape = CatalogShapes.Card,
                colors = CardDefaults.cardColors(containerColor = CatalogColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        hasDivider = isOrganizer,
                        onClick = onNavigateToSettings
                    )
                    if (isOrganizer) {
                        ProfileMenuRow(
                            icon = Icons.Outlined.Storefront,
                            text = "Modalità organizzatore",
                            hasDivider = false,
                            onClick = onNavigateToOrganizer
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CatalogSpacing.Gutter)
                    .offset(y = -cardOverlap + 16.dp)
                    .height(56.dp),
                shape = CatalogShapes.Card,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CatalogColors.AlertSoft,
                    contentColor = CatalogColors.Alert
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
                    .size(40.dp)
                    .background(CatalogColors.SurfaceMuted, CatalogShapes.Badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CatalogColors.AccentDark,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = CatalogType.LabelStrong,
                color = CatalogColors.Ink,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Vai",
                tint = CatalogColors.InkSubtle,
                modifier = Modifier.size(22.dp)
            )
        }
        if (hasDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                color = CatalogColors.Hairline,
                thickness = 1.dp
            )
        }
    }
}
