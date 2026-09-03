package com.tripify.tripify_android

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.tripify.tripify_android.catalog.ui.theme.CatalogColors
import com.tripify.tripify_android.catalog.ui.theme.CatalogShapes
import com.tripify.tripify_android.catalog.ui.theme.CatalogType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink

import com.tripify.tripify_android.auth.ui.LoginScreen
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.booking.ui.AddPassengersScreen
import com.tripify.tripify_android.booking.ui.BookingScreen
import com.tripify.tripify_android.booking.ui.CartScreen
import com.tripify.tripify_android.booking.ui.CheckoutScreen
import com.tripify.tripify_android.booking.viewmodel.BookingViewModel
import com.tripify.tripify_android.booking.viewmodel.CartViewModel
import com.tripify.tripify_android.catalog.ui.DetailScreen
import com.tripify.tripify_android.catalog.ui.HomeScreen
import com.tripify.tripify_android.catalog.ui.SearchResultsScreen
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.chat.ui.ChatScreen
import com.tripify.tripify_android.chat.ui.InboxScreen
import com.tripify.tripify_android.chat.viewmodel.ChatViewModel
import com.tripify.tripify_android.chat.viewmodel.ChatViewModelFactory
import com.tripify.tripify_android.chat.viewmodel.InboxViewModel
import com.tripify.tripify_android.chat.viewmodel.InboxViewModelFactory
import com.tripify.tripify_android.core.navigation.Route
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.notification.data.NotificationRepository
import com.tripify.tripify_android.notification.ui.NotificationsScreen
import com.tripify.tripify_android.notification.viewmodel.NotificationViewModel
import com.tripify.tripify_android.notification.viewmodel.NotificationViewModelFactory
import com.tripify.tripify_android.profile.ui.CompanionsScreen
import com.tripify.tripify_android.profile.ui.EditProfileScreen
import com.tripify.tripify_android.profile.ui.PaymentMethodsScreen
import com.tripify.tripify_android.profile.ui.ProfileScreen
import com.tripify.tripify_android.profile.ui.SettingsScreen
import com.tripify.tripify_android.profile.ui.TravelDocumentsScreen
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModel
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel
import com.tripify.tripify_android.profile.viewmodel.SettingsViewModel
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModel

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

/**
 * Altezza reale (misurata) della barra flottante, cosi' le schermate delle singole tab
 * possono lasciare altrettanto spazio in fondo al contenuto scrollabile: la barra ora
 * galleggia sopra il contenuto invece di riservargli spazio (vedi Box in TripifyApp),
 * quindi senza questo l'ultima card di una lista resterebbe a meta' nascosta sotto di lei.
 */
val LocalBottomNavBarHeight = compositionLocalOf { 0.dp }

@Composable
fun TripifyApp(
    loginViewModel: LoginViewModel,
    catalogViewModel: CatalogViewModel,
    profileViewModel: ProfileViewModel,
    companionsViewModel: CompanionsViewModel,
    travelDocumentsViewModel: TravelDocumentsViewModel,
    paymentMethodsViewModel: PaymentMethodsViewModel,
    settingsViewModel: SettingsViewModel,
    notificationViewModel: NotificationViewModel,
    pendingDeepLinkIntent: Intent? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Con launchMode="singleTask" un deep link ad app già avviata arriva come nuovo
    // intent (vedi MainActivity.onNewIntent) e va passato esplicitamente al NavController.
    LaunchedEffect(pendingDeepLinkIntent) {
        pendingDeepLinkIntent?.let { intent ->
            navController.handleDeepLink(intent)
            onDeepLinkHandled()
        }
    }

    val bookingContext = LocalContext.current
    val bookingTokenManager = remember { TokenManager.getInstance(bookingContext) }
    val cartViewModel = remember { CartViewModel(bookingTokenManager) }
    val bookingViewModel = remember { BookingViewModel(bookingTokenManager) }

    val bottomNavItems = listOf(
        BottomNavItem(Route.Home.path, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(Route.OrganizerSearch.path, "Esplora", Icons.Filled.Storefront, Icons.Outlined.Storefront),
        BottomNavItem("itineraries", "Itinerari", Icons.Filled.Map, Icons.Outlined.Map),
        BottomNavItem(Route.Bookings.path, "Prenotazioni", Icons.Filled.ConfirmationNumber, Icons.Outlined.ConfirmationNumber),
        BottomNavItem(Route.Profile.path, "Profilo", Icons.Filled.Person, Icons.Outlined.Person)
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    // La barra flottante si restringe scorrendo verso il basso e torna alla
    // dimensione piena scorrendo verso l'alto (come Instagram): osserva lo scroll
    // di qualunque contenuto scrollabile sotto di lei tramite nested scroll, senza
    // consumarlo, cosi' le liste continuano a scorrere normalmente.
    val navBarCollapse = remember { Animatable(0f) }
    val navBarScope = rememberCoroutineScope()
    val navBarNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val target = (navBarCollapse.value - available.y / 500f).coerceIn(0f, 1f)
                navBarScope.launch { navBarCollapse.snapTo(target) }
                return Offset.Zero
            }
        }
    }

    val density = LocalDensity.current
    var navBarHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CatalogColors.Background)
            .nestedScroll(navBarNestedScrollConnection)
    ) {
        CompositionLocalProvider(LocalBottomNavBarHeight provides navBarHeight) {
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.fillMaxSize()
        ) {
            // ROTTA 1: Home (Catalogo)
            composable(Route.Home.path) {
                HomeScreen(
                    viewModel = catalogViewModel,
                    notificationViewModel = notificationViewModel,
                    onNavigateToAuth = { navController.navigate(Route.Auth.path) },
                    onNavigateToDetail = { itemId -> navController.navigate("detail/$itemId") },
                    onNavigateToSaved = { navController.navigate("saved") },
                    onNavigateToBookings = { navController.navigate(Route.Bookings.path) },
                    onNavigateToSearchResults = { navController.navigate(Route.SearchResults.path) },
                    onNavigateToChat = { navController.navigate("chat") },
                    onNavigateToNotifications = { navController.navigate("notifications") }
                )
            }

            // ROTTA: Salvati
            composable("saved") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }

                com.tripify.tripify_android.itinerary.ui.MyItinerariesScreen(
                    tokenManager = tokenManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("itinerary_detail/$id") },
                    catalogViewModel = catalogViewModel,
                    showSavedContent = true,
                    onNavigateToCatalogItem = { id -> navController.navigate("detail/$id") }
                )
            }

            composable(Route.OrganizerSearch.path) {
                com.tripify.tripify_android.catalog.ui.OrganizersSearchScreen(
                    viewModel = profileViewModel,
                    onNavigateToOrganizer = { hostId -> navController.navigate(Route.OrganizerShowcase.path(hostId)) }
                )
            }

            composable(Route.OrganizerShowcase.path) { backStackEntry ->
                val hostId = backStackEntry.arguments?.getString("hostId") ?: ""
                com.tripify.tripify_android.catalog.ui.OrganizerShowcaseScreen(
                    hostId = hostId,
                    catalogViewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { itemId -> navController.navigate("detail/$itemId") },
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") }
                )
            }

            // ROTTA: Inbox (Messaggi)
            composable("chat") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }

                val inboxViewModel: InboxViewModel = viewModel(
                    factory = InboxViewModelFactory(tokenManager)
                )

                InboxScreen(
                    viewModel = inboxViewModel,
                    onChatRoomClick = { roomId -> navController.navigate("chat_detail/$roomId") },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ROTTA: Dettaglio Chat
            composable("chat_detail/{chatId}") { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("chatId") ?: ""
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }

                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(
                        roomId = roomId,
                        tokenManager = tokenManager
                    )
                )

                ChatScreen(
                    viewModel = chatViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("notifications") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }

                // Creiamo il NotificationViewModel localmente con la sua factory pulita
                val localNotificationViewModel: NotificationViewModel = viewModel(
                    factory = NotificationViewModelFactory(
                        repository = NotificationRepository(
                            RetrofitClient.createNotificationApi(
                                tokenManager
                            )
                        ),
                        tokenManager = tokenManager
                    )
                )

                NotificationsScreen(
                    viewModel = localNotificationViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Route.Bookings.path) {
                BookingScreen(
                    viewModel = bookingViewModel,
                    cartViewModel = cartViewModel,
                    catalogViewModel = catalogViewModel,
                    onNavigateToCart = { navController.navigate(Route.Cart.path) },
                    onAddPassengersClick = { bookingId -> navController.navigate(Route.AddPassengers.path(bookingId)) },
                    onShowBoardingPassClick = { bookingId -> navController.navigate(Route.BoardingPass.path(bookingId)) },
                    onBookingClick = { bookingId -> navController.navigate(Route.BookingDetail.path(bookingId)) },
                    onRetryPaymentClick = { bookingId -> navController.navigate(Route.RetryPayment.path(bookingId)) }
                )
            }

            // ROTTA: Biglietto/QR di check-in di una prenotazione
            composable(Route.BoardingPass.path) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toLongOrNull() ?: 0L
                com.tripify.tripify_android.booking.ui.BoardingPassScreen(
                    viewModel = bookingViewModel,
                    catalogViewModel = catalogViewModel,
                    bookingId = bookingId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ROTTA: Riepilogo completo di una prenotazione (solo confermate,
            // vedi BookingCard)
            composable(Route.BookingDetail.path) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toLongOrNull() ?: 0L
                com.tripify.tripify_android.booking.ui.BookingDetailScreen(
                    viewModel = bookingViewModel,
                    catalogViewModel = catalogViewModel,
                    bookingId = bookingId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Route.AddPassengers.path) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toLongOrNull() ?: 0L
                AddPassengersScreen(
                    viewModel = bookingViewModel,
                    catalogViewModel = catalogViewModel,
                    bookingId = bookingId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ROTTA: riprova il pagamento di una prenotazione rimasta PENDING
            // (es. carta rifiutata al primo tentativo), senza rifare il checkout.
            composable(Route.RetryPayment.path) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toLongOrNull() ?: 0L
                com.tripify.tripify_android.booking.ui.RetryPaymentScreen(
                    viewModel = bookingViewModel,
                    catalogViewModel = catalogViewModel,
                    bookingId = bookingId,
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentSuccess = { navController.popBackStack() }
                )
            }

            composable(Route.Cart.path) {
                CartScreen(
                    viewModel = cartViewModel,
                    catalogViewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCheckout = { navController.navigate(Route.Checkout.path) }
                )
            }

            composable(Route.Checkout.path) {
                CheckoutScreen(
                    viewModel = cartViewModel,
                    catalogViewModel = catalogViewModel,
                    bookingViewModel = bookingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentSuccess = {
                        // Stesso pattern deterministico delle tab della bottom bar (vedi
                        // onClick più sopra): pop fino a Home e via, niente saveState.
                        navController.navigate(Route.Bookings.path) {
                            popUpTo(Route.Home.path)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("itineraries") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel = viewModel(
                    factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.itinerary.ui.ItineraryListScreen(
                    viewModel = itineraryViewModel,
                    tokenManager = tokenManager,
                    onNavigateToDetail = { id, publicToken ->
                        if (!publicToken.isNullOrBlank()) navController.navigate("itinerary_public/$publicToken")
                        else navController.navigate("itinerary_detail/$id")
                    },
                    onNavigateToGenerate = { navController.navigate("itinerary_generate") }
                )
            }

            // Form per generare una bozza di itinerario (volo+hotel+attività) dal catalogo esistente.
            composable("itinerary_generate") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel = viewModel(
                    factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.itinerary.ui.GenerateItineraryScreen(
                    viewModel = itineraryViewModel,
                    catalogViewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onGenerated = { newListId ->
                        navController.navigate("itinerary_detail/$newListId") {
                            popUpTo("itinerary_generate") { inclusive = true }
                        }
                    }
                )
            }

            composable("itinerary_detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel = viewModel(
                    factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.itinerary.ui.ItineraryDetailScreen(
                    listId = id,
                    viewModel = itineraryViewModel,
                    catalogViewModel = catalogViewModel,
                    tokenManager = tokenManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToComponent = { itemId -> navController.navigate("detail/$itemId") },
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") },
                    onCloned = { newListId ->
                        navController.navigate("itinerary_detail/$newListId")
                    }
                )
            }

            // Apertura di un itinerario pubblico tramite link condiviso (capabilities):
            // nessun login richiesto, vedi ItineraryViewModel.loadDetailByPublicToken.
            composable(
                route = "itinerary_public/{token}",
                deepLinks = listOf(navDeepLink { uriPattern = "tripify://itinerary/public/{token}" })
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: return@composable
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel = viewModel(
                    factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.itinerary.ui.ItineraryDetailScreen(
                    publicToken = token,
                    viewModel = itineraryViewModel,
                    catalogViewModel = catalogViewModel,
                    tokenManager = tokenManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToComponent = { itemId -> navController.navigate("detail/$itemId") },
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") },
                    onCloned = { newListId ->
                        navController.navigate("itinerary_detail/$newListId")
                    }
                )
            }

            // Link di invito a collaborare: chi lo apre da loggato entra come collaboratore
            // (può modificare la lista, non solo vederla) e viene mandato al dettaglio normale.
            composable(
                route = "itinerary_join/{token}",
                deepLinks = listOf(navDeepLink { uriPattern = "tripify://itinerary/join/{token}" })
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: return@composable
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel = viewModel(
                    factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.itinerary.ui.JoinCollabScreen(
                    token = token,
                    viewModel = itineraryViewModel,
                    tokenManager = tokenManager,
                    onJoined = { listId ->
                        navController.navigate("itinerary_detail/$listId") {
                            popUpTo("itinerary_join/{token}") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Route.Auth.path) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Route.Auth.path) {
                LoginScreen(
                    viewModel = loginViewModel,
                    onNavigateToCatalog = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Auth.path) { inclusive = true }
                        }
                    }
                )
            }

            composable("detail/{itemId}") { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                DetailScreen(
                    itemId = itemId,
                    viewModel = catalogViewModel,
                    cartViewModel = cartViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onBookNow = { navController.navigate(Route.Cart.path) },
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") }
                )
            }

            composable("organizer") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val organizerViewModel: com.tripify.tripify_android.organizer.viewmodel.OrganizerViewModel = viewModel(
                    factory = com.tripify.tripify_android.organizer.viewmodel.OrganizerViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.organizer.ui.OrganizerScreen(
                    viewModel = organizerViewModel,
                    catalogViewModel = catalogViewModel,
                    tokenManager = tokenManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Route.Profile.path) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogoutSuccess = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Route.Auth.path) },
                    onNavigateToCompanions = { navController.navigate(Route.Companions.path) },
                    onNavigateToTravelDocuments = { navController.navigate(Route.TravelDocuments.path) },
                    onNavigateToPaymentMethods = { navController.navigate(Route.PaymentMethods.path) },
                    onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                    onNavigateToOrganizer = { navController.navigate("organizer") }
                )
            }

            composable(Route.Settings.path) {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager.getInstance(context) }
                val profileApi = remember { RetrofitClient.createProfileApi(tokenManager) }
                val currentSettingsViewModel = remember {
                    SettingsViewModel(tokenManager, profileApi)
                }

                SettingsScreen(
                    viewModel = currentSettingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToKeycloakAccount = {
                        navController.navigate("edit_profile")
                    },
                    onAccountDeleted = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Route.Companions.path) {
                CompanionsScreen(
                    viewModel = companionsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Route.TravelDocuments.path) {
                TravelDocumentsScreen(
                    viewModel = travelDocumentsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Route.PaymentMethods.path) {
                PaymentMethodsScreen(
                    viewModel = paymentMethodsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Route.SearchResults.path) {
                SearchResultsScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { itemId -> navController.navigate("detail/$itemId") }
                )
            }

            composable("edit_profile") {
                EditProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveProfile = { newName, newSurname, newPhone, newAddress, newEmail, newPwd, currentPwd ->
                        val previousEmail = profileViewModel.email
                        profileViewModel.updateProfile(
                            newName = newName,
                            newSurname = newSurname,
                            newPhone = newPhone,
                            newAddress = newAddress,
                            newEmail = newEmail,
                            newPassword = newPwd,
                            currentPassword = currentPwd,
                            onSuccess = {
                                if (newEmail.isNotBlank() && newEmail != previousEmail) {
                                    profileViewModel.logout()
                                    navController.navigate(Route.Auth.path) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    profileViewModel.loadUserProfile()
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                )
            }
        }
        }

        if (showBottomBar) {
            Surface(
                color = CatalogColors.Surface,
                contentColor = CatalogColors.Ink,
                shape = CatalogShapes.Pill,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp, top = 4.dp)
                    .navigationBarsPadding()
                    .onGloballyPositioned { coordinates ->
                        navBarHeight = with(density) { coordinates.size.height.toDp() }
                    }
                    .graphicsLayer {
                        val scale = 1f - navBarCollapse.value * 0.12f
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CatalogShapes.Pill)
                                .background(if (selected) CatalogColors.AccentDark else Color.Transparent)
                                .clickable {
                                    // Toccare una voce riporta subito la barra a piena grandezza,
                                    // anche se si era rimpicciolita scorrendo.
                                    navBarScope.launch { navBarCollapse.animateTo(0f) }
                                    if (!selected) {
                                        // Niente saveState/restoreState: con più punti che li usavano
                                        // insieme (qui e nel redirect post-pagamento) le "transazioni di
                                        // salvataggio" di Navigation-Compose finivano per mescolarsi,
                                        // e il tasto Home poteva riportare a una tab sbagliata invece che
                                        // a Home. Pop deterministico fino a Home e via: niente da
                                        // ripristinare, niente da confondere.
                                        navController.navigate(item.route) {
                                            popUpTo(Route.Home.path)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                if (selected) item.iconSelected else item.iconUnselected,
                                contentDescription = item.title,
                                tint = if (selected) Color.White else CatalogColors.InkSubtle,
                                modifier = Modifier.size(22.dp)
                            )
                            // Solo la tab attiva mostra l'etichetta: con 5 voci e schermi stretti,
                            // testi come "Prenotazioni" sempre visibili andavano a capo su due righe.
                            if (selected) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    item.title,
                                    color = Color.White,
                                    style = CatalogType.Meta.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}