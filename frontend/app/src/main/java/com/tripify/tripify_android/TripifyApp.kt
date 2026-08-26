package com.tripify.tripify_android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.tripify.tripify_android.chat.ui.InboxScreen
import com.tripify.tripify_android.chat.viewmodel.InboxViewModel
import com.tripify.tripify_android.chat.ui.ChatScreen
import com.tripify.tripify_android.chat.viewmodel.ChatViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Import delle schermate e dei ViewModel
import com.tripify.tripify_android.auth.ui.LoginScreen
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.booking.ui.BookingsScreen
import com.tripify.tripify_android.catalog.ui.DetailScreen
import com.tripify.tripify_android.catalog.ui.HomeScreen
import com.tripify.tripify_android.catalog.ui.SearchResultsScreen
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.core.navigation.Route
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.profile.ui.CompanionsScreen
import com.tripify.tripify_android.profile.ui.PaymentMethodsScreen
import com.tripify.tripify_android.profile.ui.ProfileScreen
import com.tripify.tripify_android.profile.ui.TravelDocumentsScreen
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModel
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel
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
    settingsViewModel: com.tripify.tripify_android.profile.viewmodel.SettingsViewModel,
    notificationViewModel: com.tripify.tripify_android.notification.viewmodel.NotificationViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Route.Home.path, "Home", Icons.Filled.Home),
        BottomNavItem("saved", "Salvati", Icons.Filled.FavoriteBorder),
        BottomNavItem("itineraries", "Itinerari", Icons.Filled.Map),
        BottomNavItem("bookings", "Prenotazioni", Icons.Filled.ConfirmationNumber),
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
                                    navController.navigate(item.route) {
                                        popUpTo(Route.Home.path) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
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
                    onNavigateToAuth = { navController.navigate(Route.Auth.path) },
                    onNavigateToDetail = { itemId -> navController.navigate("detail/$itemId") },
                    onNavigateToProfile = {
                        // Stesso pattern delle tab della bottom bar: Profilo e' una di
                        // quelle destinazioni, un navigate() semplice creerebbe una voce
                        // duplicata nel back stack e romperebbe il ritorno a Home dalla
                        // bottom bar.
                        navController.navigate(Route.Profile.path) {
                            popUpTo(Route.Home.path) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToBookings = { navController.navigate("bookings") },
                    onNavigateToSearchResults = { navController.navigate(Route.SearchResults.path) },
                    onNavigateToChat = { navController.navigate("chat")},
                    onNavigateToNotifications = { navController.navigate("notifications") }

                )
            }

            // ROTTA: Salvati (liste proprie/condivise/con like + singoli item di catalogo con like)
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
            // ROTTA: Inbox (Messaggi)
            composable("chat") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }

                // Niente ID fittizi! Passiamo il TokenManager al ViewModel
                val inboxViewModel: InboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.tripify.tripify_android.chat.viewmodel.InboxViewModelFactory(tokenManager)
                )

                InboxScreen(
                    viewModel = inboxViewModel,
                    onChatRoomClick = { roomId -> navController.navigate("chat_detail/$roomId") },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ROTTA: Dettaglio Chat
            composable("chat_detail/{chatId}") { backStackEntry ->
                // Niente più Long, prendiamo l'UUID come Stringa!
                val roomId = backStackEntry.arguments?.getString("chatId") ?: ""

                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }

                // Passiamo l'UUID reale della stanza e il TokenManager
                val chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.tripify.tripify_android.chat.viewmodel.ChatViewModelFactory(
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
                com.tripify.tripify_android.notification.ui.NotificationsScreen(
                    viewModel = notificationViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("bookings") {
                BookingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ROTTA: Itinerari (liste pubbliche/social)
            composable("itineraries") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModelFactory(tokenManager)
                    )

                com.tripify.tripify_android.itinerary.ui.ItineraryListScreen(
                    viewModel = itineraryViewModel,
                    onNavigateToDetail = { id -> navController.navigate("itinerary_detail/$id") },
                    onNavigateToMyLists = { navController.navigate("my_itineraries") }
                )
            }

            composable("my_itineraries") {
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }

                com.tripify.tripify_android.itinerary.ui.MyItinerariesScreen(
                    tokenManager = tokenManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("itinerary_detail/$id") }
                )
            }

            composable("itinerary_detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                val context = LocalContext.current
                val tokenManager = remember { TokenManager(context) }
                val itineraryViewModel: com.tripify.tripify_android.itinerary.viewmodel.ItineraryViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
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

            // ROTTA 2: Login
            composable(Route.Auth.path) {
                LoginScreen(
                    viewModel = loginViewModel,
                    onNavigateToCatalog = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Auth.path) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                    }
                )
            }

            // ROTTA 3: Dettaglio Elemento
            composable(route = "detail/{itemId}") { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                DetailScreen(
                    itemId = itemId,
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onBookNow = { clickedItemId -> println("Inizio prenotazione per l'ID: $clickedItemId") },
                    onChatWithOrganizer = { chatId -> navController.navigate("chat_detail/$chatId") }
                )
            }

            // ROTTA 4: Profilo Utente Unificato e suoi sottomenu
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
                    onNavigateToSettings = { navController.navigate(Route.Settings.path) }
                )
            }

            composable(Route.Settings.path) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val tokenManager = androidx.compose.runtime.remember { com.tripify.tripify_android.data.TokenManager(context) }
                val profileApi = androidx.compose.runtime.remember { com.tripify.tripify_android.data.RetrofitClient.createProfileApi(tokenManager) }
                val settingsViewModel = androidx.compose.runtime.remember {
                    com.tripify.tripify_android.profile.viewmodel.SettingsViewModel(tokenManager, profileApi)
                }

                com.tripify.tripify_android.profile.ui.SettingsScreen(
                    viewModel = settingsViewModel,
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

            // ROTTA 5: Compagni di Viaggio
            composable(Route.Companions.path) {
                CompanionsScreen(
                    viewModel = companionsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ROTTA 6: Documenti di Viaggio
            composable(Route.TravelDocuments.path) {
                TravelDocumentsScreen(
                    viewModel = travelDocumentsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ROTTA 7: Metodi di Pagamento (Wallet)
            composable(Route.PaymentMethods.path) {
                PaymentMethodsScreen(
                    viewModel = paymentMethodsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // ROTTA 8: Pagina di Ricerca
            composable(Route.SearchResults.path) {
                SearchResultsScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { itemId -> navController.navigate("detail/$itemId") }
                )
            }

            composable("edit_profile") {
                com.tripify.tripify_android.profile.ui.EditProfileScreen(
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
                                if (newEmail.isNotBlank()) {
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