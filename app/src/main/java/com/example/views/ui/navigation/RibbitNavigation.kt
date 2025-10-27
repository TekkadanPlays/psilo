package com.example.views.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.views.data.SampleData
import com.example.views.data.Author
import com.example.views.repository.RelayRepository
import com.example.views.ui.components.NoteCard
import com.example.views.ui.components.ScrollAwareBottomNavigationBar
import com.example.views.ui.screens.AboutScreen
import com.example.views.ui.screens.AccountPreferencesScreen
import com.example.views.ui.screens.AppearanceSettingsScreen
import com.example.views.ui.screens.DashboardScreen
import com.example.views.ui.screens.ModernThreadViewScreen
import com.example.views.ui.screens.NotificationsScreen
import com.example.views.ui.screens.TopicsScreen
import com.example.views.ui.screens.ProfileScreen
import com.example.views.ui.screens.RelayManagementScreen
import com.example.views.ui.screens.SettingsScreen
import com.example.views.ui.screens.createSampleCommentThreads
import com.example.views.viewmodel.AppViewModel
import com.example.views.viewmodel.rememberThreadStateHolder
import com.example.views.viewmodel.DashboardViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main navigation composable for Ribbit app using Jetpack Navigation. This provides proper
 * backstack management like Primal, allowing infinite exploration through feeds, threads, and
 * profiles without losing history.
 *
 * The bottom navigation bar is persistent across main screens and hidden on detail screens. Uses
 * MaterialFadeThrough transitions for navigation bar page changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RibbitNavigation(
        appViewModel: AppViewModel,
        accountStateViewModel: com.example.views.viewmodel.AccountStateViewModel,
        onAmberLogin: (android.content.Intent) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val relayRepository = remember { RelayRepository(context) }

    // Feed state - separate states for Home and Topics feeds
    val feedStateViewModel: com.example.views.viewmodel.FeedStateViewModel = viewModel()

    // Thread state holder - persists scroll positions and comment states per thread
    val threadStateHolder = rememberThreadStateHolder()

    // Track scroll states for different screens
    val feedListState = rememberLazyListState()

    // Global top app bar state for collapsible navigation
    // This state is shared across main screens so collapse state persists during navigation
    val topAppBarState = rememberTopAppBarState()

    // Dashboard list state for scroll-to-top functionality
    val dashboardListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Determine current route to show/hide bottom nav
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Main screens that should show the bottom navigation
    val mainScreenRoutes = setOf("dashboard", "notifications", "relays", "messages", "announcements", "topics")
    val showBottomNav = currentRoute in mainScreenRoutes

    // Current destination for bottom nav highlighting
    val currentDestination =
            when {
                currentRoute == "dashboard" -> "home"
                currentRoute == "topics" -> "topics"
                currentRoute == "announcements" -> "announcements"
                currentRoute in mainScreenRoutes -> currentRoute ?: "home"
                else -> "home"
            }

    // Sample notification count
    val notificationCount = 6

    // Double-tap back to exit
    var backPressedTime by remember { mutableLongStateOf(0L) }
    var shouldShowExitToast by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Show toast when needed
    LaunchedEffect(shouldShowExitToast) {
        if (shouldShowExitToast) {
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            shouldShowExitToast = false
        }
    }

    // Handle back press on main screens
    BackHandler(enabled = currentRoute in mainScreenRoutes) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            // Double tap detected - exit app
            (context as? android.app.Activity)?.finish()
        } else {
            // First tap - show toast
            backPressedTime = currentTime
            shouldShowExitToast = true
        }
    }

    Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    ScrollAwareBottomNavigationBar(
                            currentDestination = currentDestination,
                            onDestinationClick = { destination ->
                                when (destination) {
                                    "home" -> {
                                        if (currentDestination == "dashboard") {
                                            // Already on dashboard - scroll to top with animation
                                            coroutineScope.launch {
                                                dashboardListState.animateScrollToItem(0)
                                            }
                                        } else {
                                            // Navigate to dashboard
                                            navController.navigate("dashboard") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                    "messages" -> {
                                        /* No action - icon disabled */
                                    }
                                    "relays" ->
                                            navController.navigate("relays") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                    "topics" ->
                                            navController.navigate("topics") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                    "announcements" ->
                                            navController.navigate("announcements") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                    "notifications" ->
                                            navController.navigate("notifications") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                }
                            },
                            isVisible = true,
                            notificationCount = notificationCount,
                            topAppBarState = topAppBarState
                    )
                }
            }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    enterTransition = {
                        // Use MaterialFadeThrough for main screen transitions only
                        when {
                            initialState.destination.route in mainScreenRoutes &&
                                    targetState.destination.route in mainScreenRoutes -> {
                                // Fade through transition for navigation bar navigation
                                fadeIn(animationSpec = tween(210, delayMillis = 90))
                            }
                            else -> {
                                // Default: slide + fade (overridden by per-route transitions)
                                slideIntoContainer(
                                        towards =
                                                AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec =
                                                tween(
                                                        300,
                                                        easing =
                                                                MaterialMotion
                                                                        .EasingStandardDecelerate
                                                )
                                ) + fadeIn(animationSpec = tween(300))
                            }
                        }
                    },
                    exitTransition = {
                        // Use MaterialFadeThrough for main screen transitions only
                        when {
                            initialState.destination.route in mainScreenRoutes &&
                                    targetState.destination.route in mainScreenRoutes -> {
                                // Fade through transition for navigation bar navigation
                                fadeOut(animationSpec = tween(90))
                            }
                            else -> {
                                // Default: slide + fade (overridden by per-route transitions)
                                slideOutOfContainer(
                                        towards =
                                                AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec =
                                                tween(
                                                        300,
                                                        easing =
                                                                MaterialMotion
                                                                        .EasingStandardAccelerate
                                                )
                                ) + fadeOut(animationSpec = tween(300))
                            }
                        }
                    },
                    popEnterTransition = {
                        // Use MaterialFadeThrough for main screen transitions only
                        when {
                            initialState.destination.route in mainScreenRoutes &&
                                    targetState.destination.route in mainScreenRoutes -> {
                                fadeIn(animationSpec = tween(210, delayMillis = 90))
                            }
                            else -> {
                                // Default: slide + fade (overridden by per-route transitions)
                                slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec =
                                                tween(
                                                        300,
                                                        easing =
                                                                MaterialMotion
                                                                        .EasingStandardDecelerate
                                                )
                                ) + fadeIn(animationSpec = tween(300))
                            }
                        }
                    },
                    popExitTransition = {
                        // Use MaterialFadeThrough for main screen transitions only
                        when {
                            initialState.destination.route in mainScreenRoutes &&
                                    targetState.destination.route in mainScreenRoutes -> {
                                fadeOut(animationSpec = tween(90))
                            }
                            else -> {
                                // Default: slide + fade (overridden by per-route transitions)
                                slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec =
                                                tween(
                                                        300,
                                                        easing =
                                                                MaterialMotion
                                                                        .EasingStandardAccelerate
                                                )
                                ) + fadeOut(animationSpec = tween(300))
                            }
                        }
                    }
            ) {
                // Dashboard - Home feed
                composable("dashboard") {
                    val appState by appViewModel.appState.collectAsState()

                    DashboardScreen(
                            isSearchMode = appState.isSearchMode,
                            onSearchModeChange = { appViewModel.updateSearchMode(it) },
                            onProfileClick = { authorId ->
                                navController.navigateToProfile(authorId)
                            },
                            onNavigateTo = { screen ->
                                when (screen) {
                                    "settings" -> navController.navigate("settings")
                                    "relays" -> navController.navigate("relays")
                                    "notifications" -> navController.navigate("notifications")
                                    "messages" -> navController.navigate("messages")
                                    "announcements" -> navController.navigate("announcements")
                                    "user_profile" -> navController.navigate("user_profile")
                                }
                            },
                            onThreadClick = { note ->
                                appViewModel.updateSelectedNote(note)
                                navController.navigateToThread(note.id)
                            },
                            onScrollToTop = {
                                coroutineScope.launch {
                                    dashboardListState.animateScrollToItem(0)
                                }
                            },
                            listState = dashboardListState,
                            feedStateViewModel = feedStateViewModel,
                            accountStateViewModel = accountStateViewModel,
                            relayRepository = relayRepository,
                            onLoginClick = {
                                val loginIntent = accountStateViewModel.loginWithAmber()
                                onAmberLogin(loginIntent)
                            },
                            initialTopAppBarState = topAppBarState
                    )
                }

                // Thread view - Can navigate to profiles and other threads
                composable(
                        route = "thread/{noteId}?replyKind={replyKind}",
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.StringType },
                            navArgument("replyKind") {
                                type = NavType.IntType
                                defaultValue = 1 // Default to Kind 1 (home feed)
                            }
                        ),
                        enterTransition = {
                            // Override: Shared X-axis forward (no fade to prevent doubling)
                            slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                            )
                        },
                        exitTransition = {
                            // Override: No exit animation when going forward
                            null
                        },
                        popEnterTransition = {
                            // Override: No enter animation when coming back
                            null
                        },
                        popExitTransition = {
                            // Override: Shared X-axis back (no fade to prevent doubling)
                            slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardAccelerate)
                            )
                        }
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
                    val replyKind = backStackEntry.arguments?.getInt("replyKind") ?: 1
                    val context = LocalContext.current

                    // Get note from AppViewModel's selected note
                    val appState by appViewModel.appState.collectAsState()
                    val note = appState.selectedNote ?: run {
                        // Fallback to sample data for testing
                        SampleData.sampleNotes.find { it.id == noteId }
                    } ?: return@composable

                    // Get relay URLs for thread replies
                    val storageManager = remember { com.example.views.repository.RelayStorageManager(context) }
                    val currentAccount by accountStateViewModel.currentAccount.collectAsState()
                    val relayUrls = remember(currentAccount) {
                        currentAccount?.toHexKey()?.let { pubkey ->
                            val categories = storageManager.loadCategories(pubkey)
                            val favoriteCategory = categories.firstOrNull { it.isFavorite }
                                ?: categories.firstOrNull { it.isDefault }
                            favoriteCategory?.relays?.map { it.url } ?: emptyList()
                        } ?: emptyList()
                    }

                    val sampleComments = createSampleCommentThreads()

                        // Restore scroll state for this specific thread
                        val savedScrollState = threadStateHolder.getScrollState(noteId)
                        val threadListState =
                                rememberLazyListState(
                                        initialFirstVisibleItemIndex =
                                                savedScrollState.firstVisibleItemIndex,
                                        initialFirstVisibleItemScrollOffset =
                                                savedScrollState.firstVisibleItemScrollOffset
                                )

                        // Get comment states for this specific thread
                        val commentStates = threadStateHolder.getCommentStates(noteId)
                        var expandedControlsCommentId by remember {
                            mutableStateOf(threadStateHolder.getExpandedControls(noteId))
                        }

                        // Save scroll state when leaving the screen
                        DisposableEffect(noteId) {
                            onDispose {
                                threadStateHolder.saveScrollState(noteId, threadListState)
                                threadStateHolder.setExpandedControls(
                                        noteId,
                                        expandedControlsCommentId
                                )
                            }
                        }

                    ModernThreadViewScreen(
                            note = note,
                            comments = sampleComments,
                            listState = threadListState,
                            commentStates = commentStates,
                            expandedControlsCommentId = expandedControlsCommentId,
                            onExpandedControlsChange = { commentId ->
                                expandedControlsCommentId =
                                        if (expandedControlsCommentId == commentId) null
                                        else commentId
                            },
                            topAppBarState = topAppBarState,
                            replyKind = replyKind,
                            relayUrls = relayUrls,
                            onBackClick = { navController.popBackStack() },
                            onLike = { /* TODO: Handle like */},
                            onShare = { /* TODO: Handle share */},
                            onComment = { /* TODO: Handle comment */},
                            onProfileClick = { authorId ->
                                // Navigate to profile - adds to backstack
                                navController.navigateToProfile(authorId)
                            },
                            onCommentLike = { /* TODO: Handle comment like */},
                            onCommentReply = { /* TODO: Handle comment reply */},
                            onLoginClick = {
                                val loginIntent = accountStateViewModel.loginWithAmber()
                                onAmberLogin(loginIntent)
                            }
                    )
                }

                // Profile view - Can navigate to threads and other profiles
                composable(
                        route = "profile/{authorId}",
                        arguments = listOf(navArgument("authorId") { type = NavType.StringType }),
                        enterTransition = {
                            // Override: Shared X-axis forward (no fade to prevent doubling)
                            slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                            )
                        },
                        exitTransition = {
                            // Override: No exit animation when going forward
                            null
                        },
                        popEnterTransition = {
                            // Override: No enter animation when coming back
                            null
                        },
                        popExitTransition = {
                            // Override: Shared X-axis back (no fade to prevent doubling)
                            slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardAccelerate)
                            )
                        }
                ) { backStackEntry ->
                    val authorId =
                            backStackEntry.arguments?.getString("authorId") ?: return@composable

                    // Try to find author from real notes in dashboard or sample data
                    val dashboardViewModel: DashboardViewModel = viewModel()
                    val dashboardState by dashboardViewModel.uiState.collectAsState()

                    val author = dashboardState.notes.find { it.author.id == authorId }?.author
                        ?: SampleData.sampleNotes.find { it.author.id == authorId }?.author
                        ?: Author(
                            id = authorId,
                            username = authorId.take(8) + "...",
                            displayName = authorId.take(8) + "...",
                            avatarUrl = null,
                            isVerified = false
                        )

                    val authorNotes = dashboardState.notes.filter { it.author.id == author.id }
                            .ifEmpty { SampleData.sampleNotes.filter { it.author.id == author.id } }
                    val profileListState = rememberLazyListState()

                    ProfileScreen(
                            author = author,
                            authorNotes = authorNotes,
                            listState = profileListState,
                            onBackClick = { navController.popBackStack() },
                            onNoteClick = { note ->
                                // Navigate to thread - adds to backstack
                                navController.navigateToThread(note.id)
                                appViewModel.updateSelectedNote(note)
                            },
                            onProfileClick = { newAuthorId ->
                                // Navigate to another profile - adds to backstack
                                // This allows infinite profile browsing with full history
                                navController.navigateToProfile(newAuthorId)
                            },
                            onNavigateTo = { /* Not needed with NavController */}
                    )
                }

                // User's own profile
                composable("user_profile") {
                    val authState by accountStateViewModel.authState.collectAsState()
                    val currentUser =
                            authState.userProfile?.let { userProfile ->
                                com.example.views.data.Author(
                                        id = userProfile.pubkey,
                                        username = userProfile.name ?: "user",
                                        displayName = userProfile.displayName
                                                        ?: userProfile.name ?: "User",
                                        avatarUrl = userProfile.picture,
                                        isVerified = false
                                )
                            }
                                    ?: com.example.views.data.Author(
                                            id = "guest",
                                            username = "guest",
                                            displayName = "Guest User",
                                            avatarUrl = null,
                                            isVerified = false
                                    )

                    val userNotes = emptyList<com.example.views.data.Note>()
                    val userProfileListState = rememberLazyListState()

                    ProfileScreen(
                            author = currentUser,
                            authorNotes = userNotes,
                            listState = userProfileListState,
                            onBackClick = { navController.popBackStack() },
                            onNoteClick = { note ->
                                navController.navigateToThread(note.id)
                                appViewModel.updateSelectedNote(note)
                            },
                            onProfileClick = { authorId ->
                                navController.navigateToProfile(authorId)
                            },
                            onNavigateTo = { /* Not needed with NavController */}
                    )
                }

                // Settings
                composable("settings") {
                    SettingsScreen(
                            onBackClick = { navController.popBackStack() },
                            onNavigateTo = { screen ->
                                when (screen) {
                                    "appearance" -> navController.navigate("settings/appearance")
                                    "account_preferences" ->
                                            navController.navigate("settings/account_preferences")
                                    "about" -> navController.navigate("settings/about")
                                }
                            },
                            onBugReportClick = {
                                val intent =
                                        android.content.Intent(android.content.Intent.ACTION_VIEW)
                                intent.data =
                                        android.net.Uri.parse(
                                                "https://github.com/TekkadanPlays/ribbit-android/issues"
                                        )
                                context.startActivity(intent)
                            }
                    )
                }

                // Settings sub-screens
                composable("settings/appearance") {
                    AppearanceSettingsScreen(onBackClick = { navController.popBackStack() })
                }

                composable("settings/account_preferences") {
                    AccountPreferencesScreen(
                            onBackClick = { navController.popBackStack() },
                            accountStateViewModel = accountStateViewModel
                    )
                }

                composable("settings/about") {
                    AboutScreen(onBackClick = { navController.popBackStack() })
                }

                // Relay management
                // Relay Management - Can navigate back to dashboard
                composable("relays") {
                    RelayManagementScreen(
                            onBackClick = {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            },
                            relayRepository = relayRepository,
                            accountStateViewModel = accountStateViewModel,
                            topAppBarState = topAppBarState
                    )
                }

                // Notifications - Can navigate to threads and profiles
                composable("notifications") {
                    NotificationsScreen(
                            onBackClick = {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            },
                            onNoteClick = { note ->
                                navController.navigateToThread(note.id)
                                appViewModel.updateSelectedNote(note)
                            },
                            onLike = { /* TODO: Handle like */},
                            onShare = { /* TODO: Handle share */},
                            onComment = { /* TODO: Handle comment */},
                            onProfileClick = { authorId ->
                                navController.navigateToProfile(authorId)
                            },
                            topAppBarState = topAppBarState
                    )
                }

                // Topics - Kind 11 topics with kind 1111 replies
                composable("topics") {
                    TopicsScreen(
                            onNavigateTo = { destination ->
                                navController.navigate(destination)
                            },
                            onThreadClick = { note ->
                                navController.navigateToThread(note.id)
                                appViewModel.updateSelectedNote(note)
                            },
                            onProfileClick = { authorId ->
                                navController.navigateToProfile(authorId)
                            },
                            feedStateViewModel = feedStateViewModel,
                            appViewModel = appViewModel,
                            relayRepository = relayRepository,
                            accountStateViewModel = accountStateViewModel,
                            onLoginClick = {
                                val loginIntent = accountStateViewModel.loginWithAmber()
                                onAmberLogin(loginIntent)
                            },
                            initialTopAppBarState = topAppBarState
                    )
                }

                // Announcements - Special feed for Tekkadan announcements
                composable("announcements") {
                    val announcementsViewModel: com.example.views.viewmodel.AnnouncementsViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel()

                    val announcementsUiState by announcementsViewModel.uiState.collectAsState()
                    val announcementListState = rememberLazyListState()

                    // Simple announcements display
                    LazyColumn(
                        state = announcementListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = announcementsUiState.announcements,
                            key = { it.id }
                        ) { announcement ->
                            Column {
                                NoteCard(
                                    note = announcement,
                                    onProfileClick = { authorId ->
                                        navController.navigateToProfile(authorId)
                                    },
                                    onLike = { /* Announcements are read-only */ },
                                    onShare = { /* TODO: Implement share */ },
                                    onComment = { /* TODO: Implement comment */ }
                                )
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Navigation extension functions for type-safe navigation */
private fun NavController.navigateToProfile(authorId: String) {
    navigate("profile/$authorId")
}

private fun NavController.navigateToThread(noteId: String, replyKind: Int = 1) {
    navigate("thread/$noteId?replyKind=$replyKind")
}
