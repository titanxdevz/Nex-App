package com.nexchat.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexchat.data.local.TokenStorage
import com.nexchat.service.SocketService
import com.nexchat.service.SocketState
import com.nexchat.ui.components.AppLockGate
import com.nexchat.ui.components.OfflineIndicatorBar
import com.nexchat.ui.screens.auth.AuthScreen
import com.nexchat.ui.screens.chat.ChatPage
import com.nexchat.ui.screens.calls.CallsPage
import com.nexchat.ui.screens.status.StatusPage
import com.nexchat.ui.screens.explore.ExplorePage
import com.nexchat.ui.screens.friends.FriendsPage
import com.nexchat.ui.screens.community.CommunityPage
import com.nexchat.ui.screens.settings.SettingsPage
import com.nexchat.ui.screens.join.JoinGroupPage
import com.nexchat.ui.viewmodel.AuthViewModel
import com.nexchat.util.BiometricHelper
import javax.inject.Inject

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Chat : Screen("chat")
    data object GroupChat : Screen("group/{id}") {
        fun createRoute(id: String) = "group/$id"
    }
    data object DirectMessage : Screen("dm/{username}") {
        fun createRoute(username: String) = "dm/$username"
    }
    data object Status : Screen("status")
    data object Settings : Screen("settings")
    data object Explore : Screen("explore")
    data object Friends : Screen("friends")
    data object Calls : Screen("calls")
    data object Community : Screen("community/{id}") {
        fun createRoute(id: String) = "community/$id"
    }
    data object JoinGroup : Screen("invite/{token}") {
        fun createRoute(token: String) = "invite/$token"
    }
}

// ─── Transition Specs ──────────────────────────────────────────────────────

private const val SLIDE_MS = 320
private const val FADE_MS = 180

private val slideFromRight: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it / 3 },
        animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(FADE_MS, delayMillis = 80, easing = FastOutSlowInEasing)) +
    scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))

private val slideToLeft: ExitTransition =
    slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
    fadeOut(tween(FADE_MS, easing = FastOutSlowInEasing))

private val slideFromLeft: EnterTransition =
    slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
    fadeIn(tween(FADE_MS, delayMillis = 50, easing = FastOutSlowInEasing))

private val slideToRight: ExitTransition =
    slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
    fadeOut(tween(FADE_MS, easing = FastOutSlowInEasing))

private val authEnter: EnterTransition =
    fadeIn(tween(400, easing = FastOutSlowInEasing)) +
    scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))

private val authExit: ExitTransition =
    fadeOut(tween(300, easing = FastOutSlowInEasing)) +
    scaleOut(targetScale = 1.05f, animationSpec = tween(300, easing = FastOutSlowInEasing))

private val authPopEnter: EnterTransition =
    fadeIn(tween(400, easing = FastOutSlowInEasing)) +
    scaleIn(initialScale = 1.05f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))

private val authPopExit: ExitTransition =
    fadeOut(tween(300, easing = FastOutSlowInEasing)) +
    scaleOut(targetScale = 0.92f, animationSpec = tween(300, easing = FastOutSlowInEasing))

private val slideFromBottom: EnterTransition =
    slideInVertically(initialOffsetY = { it / 4 }, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) +
    fadeIn(tween(FADE_MS, delayMillis = 60, easing = FastOutSlowInEasing))

private val slideToBottom: ExitTransition =
    slideOutVertically(targetOffsetY = { it / 6 }, animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
    fadeOut(tween(FADE_MS, easing = FastOutSlowInEasing))

// ─── NavGraph ──────────────────────────────────────────────────────────────

@Composable
fun NexChatNavGraph(
    initialDeepLink: DeepLink? = null
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val socketService: SocketService = hiltViewModel<com.nexchat.ui.viewmodel.ChatViewModel>().let { null } // injected via Hilt
    val connectionState by remember { mutableStateOf<SocketState>(SocketState.Disconnected) }

    // App lock state
    var isAppLocked by remember { mutableStateOf(false) }
    var hasPin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    // Handle deep links
    LaunchedEffect(initialDeepLink) {
        when (initialDeepLink) {
            is DeepLink.JoinGroup -> {
                navController.navigate(Screen.JoinGroup.createRoute(initialDeepLink.token))
            }
            is DeepLink.UserProfile -> {
                navController.navigate(Screen.DirectMessage.createRoute(initialDeepLink.username))
            }
            null -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
        ) {
            composable(
                route = Screen.Auth.route,
                enterTransition = { authEnter },
                exitTransition = { authExit },
                popEnterTransition = { authPopEnter },
                popExitTransition = { authPopExit }
            ) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Chat.route,
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) {
                ChatPage(
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToGroup = { navController.navigate(Screen.GroupChat.createRoute(it)) },
                    onNavigateToDm = { navController.navigate(Screen.DirectMessage.createRoute(it)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToCalls = { navController.navigate(Screen.Calls.route) },
                    onNavigateToStatus = { navController.navigate(Screen.Status.route) },
                    onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToCommunity = { navController.navigate(Screen.Community.createRoute(it)) }
                )
            }

            composable(
                route = Screen.GroupChat.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) {
                ChatPage(
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToGroup = { navController.navigate(Screen.GroupChat.createRoute(it)) },
                    onNavigateToDm = { navController.navigate(Screen.DirectMessage.createRoute(it)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToCalls = { navController.navigate(Screen.Calls.route) },
                    onNavigateToStatus = { navController.navigate(Screen.Status.route) },
                    onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToCommunity = { navController.navigate(Screen.Community.createRoute(it)) }
                )
            }

            composable(
                route = Screen.DirectMessage.route,
                arguments = listOf(navArgument("username") { type = NavType.StringType }),
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) {
                ChatPage(
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToGroup = { navController.navigate(Screen.GroupChat.createRoute(it)) },
                    onNavigateToDm = { navController.navigate(Screen.DirectMessage.createRoute(it)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToCalls = { navController.navigate(Screen.Calls.route) },
                    onNavigateToStatus = { navController.navigate(Screen.Status.route) },
                    onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                    onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                    onNavigateToCommunity = { navController.navigate(Screen.Community.createRoute(it)) }
                )
            }

            composable(
                route = Screen.Calls.route,
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) { CallsPage(onBack = { navController.popBackStack() }) }

            composable(
                route = Screen.Status.route,
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) { StatusPage(onBack = { navController.popBackStack() }) }

            composable(
                route = Screen.Explore.route,
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) { ExplorePage(onBack = { navController.popBackStack() }) }

            composable(
                route = Screen.Friends.route,
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) { FriendsPage(onBack = { navController.popBackStack() }) }

            composable(
                route = Screen.Settings.route,
                enterTransition = { slideFromRight },
                exitTransition = { slideToLeft },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) {
                SettingsPage(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Community.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                enterTransition = { slideFromBottom },
                exitTransition = { slideToBottom },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) { backStackEntry ->
                CommunityPage(
                    communityId = backStackEntry.arguments?.getString("id") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.JoinGroup.route,
                arguments = listOf(navArgument("token") { type = NavType.StringType }),
                enterTransition = { slideFromBottom },
                exitTransition = { slideToBottom },
                popEnterTransition = { slideFromLeft },
                popExitTransition = { slideToRight }
            ) { backStackEntry ->
                JoinGroupPage(
                    token = backStackEntry.arguments?.getString("token") ?: "",
                    onJoined = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Chat.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
