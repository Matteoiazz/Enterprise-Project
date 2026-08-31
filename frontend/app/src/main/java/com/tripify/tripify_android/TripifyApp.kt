package com.tripify.tripify_android

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF7A7A73)
private val TripifyGreen = Color(0xFF2E7D32)

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)

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

    // Con launchMode="singleTask" un link aperto ad app già avviata non ricrea
    // l'Activity: arriva qui come nuovo intent (vedi MainActivity.onNewIntent) e va
    // passato esplicitamente al NavController, altrimenti resta ignorato e l'app
    // si limita a tornare in primo piano sulla schermata dove si era rimasti.
    LaunchedEffect(pendingDeepLinkIntent) {
        pendingDeepLinkIntent?.let { intent ->
            navController.handleDeepLink(intent)
            onDeepLinkHandled()
        }
    }

    val bookingContext = LocalContext.current
    val bookingTokenManager = remember { TokenManager(bookingContext) }
    val cartViewModel = remember { CartViewModel(bookingTokenManager) }
    val bookingViewModel = remember { BookingViewModel(bookingTokenManager) }

    val bottomNavItems = listOf(
        BottomNavItem(Route.Home.path, "Home", Icons.Filled.Home),
        BottomNavItem(Route.OrganizerSearch.path, "Esplora", Icons.Filled.Storefront),
        BottomNavItem("itineraries", "Itinerari", Icons.Filled.Map),
        BottomNavItem(Route.Bookings.path, "Prenotazioni", Icons.Filled.ConfirmationNumber),
        BottomNavItem(Route.Profile.path, "Profilo", Icons.Filled.PersonOutline)
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Ink,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = {
                                Text(
                                    item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
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
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TripifyGreen,
                                selectedTextColor = TripifyGreen,
                                unselectedIconColor = InkMuted,
                                unselectedTextColor = InkMuted,
                                indicatorColor = TripifyGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(innerPadding)
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
                val tokenManager = remember { TokenManager(context) }

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
                val tokenManager = remember { TokenManager(context) }

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
                val tokenManager = remember { TokenManager(context) }

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
                val tokenManager = remember { TokenManager(context) }

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
                    onShowBoardingPassClick = { bookingId -> navController.navigate(Route.BoardingPass.path(bookingId)) }
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

            composable(Route.AddPassengers.path) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId")?.toLongOrNull() ?: 0L
                AddPassengersScreen(
                    viewModel = bookingViewModel,
                    catalogViewModel = catalogViewModel,
                    bookingId = bookingId,
                    onNavigateBack = { navController.popBackStack() }
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
                val tokenManager = remember { TokenManager(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel = viewModel(
                    factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                )

                com.tripify.tripify_android.itinerary.ui.ItineraryListScreen(
                    viewModel = itineraryViewModel,
                    tokenManager = tokenManager,
                    onNavigateToDetail = { id, publicToken ->
                        if (!publicToken.isNullOrBlank()) navController.navigate("itinerary_public/$publicToken")
                        else navController.navigate("itinerary_detail/$id")
                    }
                )
            }

            composable("itinerary_detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
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
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") }
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
                val tokenManager = remember { TokenManager(context) }
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
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") }
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
                val tokenManager = remember { TokenManager(context) }
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
                    },
                    onNavigateToRegister = {}
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
                val tokenManager = remember { TokenManager(context) }
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
                val tokenManager = remember { TokenManager(context) }
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
                    onSaveProfile = { newName, newSurname, newPhone, newAddress, newEmail, newPwd ->
                        profileViewModel.updateProfile(
                            newName = newName,
                            newSurname = newSurname,
                            newPhone = newPhone,
                            newAddress = newAddress,
                            newEmail = newEmail,
                            newPassword = newPwd,
                            onSuccess = {
                                if (newEmail.isNotBlank() && newEmail != profileViewModel.email) {
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
}