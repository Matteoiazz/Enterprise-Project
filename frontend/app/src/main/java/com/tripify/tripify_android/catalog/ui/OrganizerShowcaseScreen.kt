package com.tripify.tripify_android.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripify.tripify_android.catalog.model.CatalogItem
import com.tripify.tripify_android.catalog.ui.components.ExcursionCard
import com.tripify.tripify_android.catalog.ui.components.FlightCard
import com.tripify.tripify_android.catalog.ui.components.HotelCard
import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogSpacing
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.UserResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerShowcaseScreen(
    hostId: String,
    catalogViewModel: CatalogViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onChatWithOrganizer: (String) -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val profileApi = remember { RetrofitClient.createProfileApi(tokenManager) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var organizerProfile by remember { mutableStateOf<UserResponse?>(null) }
    var organizerItems by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(hostId) {
        try {
            val profile = profileApi.getOrganizerById(hostId)
            organizerProfile = profile

            if (profile.id.isNotBlank()) {
                organizerItems = catalogViewModel.getItemsByOrganizer(profile.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = CatalogColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("VETRINA", style = CatalogType.Wordmark, color = CatalogColors.Ink) },
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CatalogColors.AccentDark, strokeWidth = 3.dp)
                    }
                }
            } else if (organizerProfile != null) {
                val org = organizerProfile!!
                val displayName = "${org.name ?: ""} ${org.surname ?: ""}".trim().ifEmpty { "Organizzatore" }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CatalogColors.Surface)
                            .padding(vertical = 32.dp, horizontal = CatalogSpacing.Gutter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!org.profilePictureUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = org.profilePictureUrl,
                                contentDescription = displayName,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(CatalogColors.AccentSoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = CatalogColors.AccentDark,
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = displayName, style = CatalogType.Hero, color = CatalogColors.Ink, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = org.email, style = CatalogType.Body, color = CatalogColors.InkMuted)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(CatalogShapes.Badge)
                                .background(CatalogColors.AccentSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (organizerItems.isEmpty()) "PARTNER VERIFICATO"
                                else "PARTNER VERIFICATO · ${organizerItems.size} ANNUNCI",
                                style = CatalogType.Overline,
                                color = CatalogColors.AccentDark
                            )
                        }

                        // SEZIONE INFORMAZIONI AZIENDALI
                        if (!org.companyName.isNullOrBlank() || !org.vatNumber.isNullOrBlank() || !org.pec.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CatalogColors.SurfaceMuted),
                                shape = CatalogShapes.Card
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text("INFORMAZIONI AZIENDALI", style = CatalogType.Overline, color = CatalogColors.InkMuted)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (!org.companyName.isNullOrBlank()) {
                                        BusinessInfoRow(Icons.Default.Business, "Azienda", org.companyName)
                                    }
                                    if (!org.vatNumber.isNullOrBlank()) {
                                        BusinessInfoRow(Icons.Default.ReceiptLong, "P.IVA", org.vatNumber)
                                    }
                                    if (!org.pec.isNullOrBlank()) {
                                        BusinessInfoRow(Icons.Default.Email, "PEC", org.pec)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val token = tokenManager.tokenFlow.first()
                                    if (token.isNullOrBlank()) {
                                        snackbarHostState.showSnackbar("Devi accedere per contattare l'organizzatore")
                                        return@launch
                                    }
                                    val chatRoom = com.tripify.tripify_android.chat.repository.ChatRepository.getOrCreateChatRoom(
                                        hostId = org.id, title = "Organizzatore $displayName", authToken = token
                                    )
                                    if (chatRoom != null) {
                                        onChatWithOrganizer(chatRoom.id)
                                    } else {
                                        snackbarHostState.showSnackbar("Impossibile aprire la chat con l'organizzatore")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CatalogColors.AccentDark),
                            shape = CatalogShapes.Pill
                        ) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Contatta Organizzatore", style = CatalogType.Button)
                        }
                    }
                    HorizontalDivider(color = CatalogColors.Hairline)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Tutti gli annunci",
                        style = CatalogType.Section,
                        color = CatalogColors.Ink,
                        modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter).padding(bottom = 16.dp)
                    )
                }

                if (organizerItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(72.dp).background(CatalogColors.SurfaceMuted, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, tint = CatalogColors.InkSubtle, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Nessun annuncio", style = CatalogType.Section, color = CatalogColors.Ink)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Questo organizzatore non ha ancora pubblicato esperienze.",
                                style = CatalogType.Body,
                                color = CatalogColors.InkMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(organizerItems) { item ->
                        Box(modifier = Modifier.padding(horizontal = CatalogSpacing.Gutter, vertical = 8.dp)) {
                            val openDetail = {
                                catalogViewModel.onItemViewed(item)
                                onNavigateToDetail(item.id.toString())
                            }
                            when (item) {
                                is CatalogItem.Flight -> FlightCard(flight = item, onClick = openDetail)
                                is CatalogItem.Hotel -> HotelCard(hotel = item, onClick = openDetail)
                                is CatalogItem.Excursion -> ExcursionCard(excursion = item, onClick = openDetail)
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Errore nel caricamento del profilo.", style = CatalogType.Body, color = CatalogColors.Alert)
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(CatalogColors.Surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CatalogColors.AccentDark, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = CatalogType.Caption, color = CatalogColors.InkMuted)
            Text(text = value, style = CatalogType.BodyStrong, color = CatalogColors.Ink)
        }
    }
}