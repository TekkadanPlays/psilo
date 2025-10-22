package com.example.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.example.views.data.Author
import com.example.views.data.Note
import com.example.views.data.SampleData
import com.example.views.ui.navigation.MaterialMotion
import com.example.views.ui.screens.AboutScreen
import com.example.views.ui.screens.AppearanceSettingsScreen
import com.example.views.ui.screens.DashboardScreen
import com.example.views.ui.screens.ProfileScreen
import com.example.views.ui.screens.SettingsScreen
import com.example.views.ui.screens.NotificationsScreen
import com.example.views.ui.screens.ModernThreadViewScreen
import com.example.views.ui.screens.RelayManagementScreen
import com.example.views.ui.screens.createSampleCommentThreads
import com.example.views.repository.RelayRepository
import com.example.views.ui.theme.ViewsTheme
import com.example.views.viewmodel.AppViewModel
import com.example.views.viewmodel.AuthViewModel

data class NavigationEntry(
    val screen: String,
    val noteId: String? = null,
    val authorId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    
    // Activity result launcher for Amber login
    private val amberLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle the login result
        onAmberLoginResult?.invoke(result.resultCode, result.data)
    }
    
    // Callback to handle login result
    private var onAmberLoginResult: ((Int, Intent?) -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        
        setContent {
            ViewsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithNavigation(
                        onAmberLogin = { intent -> amberLoginLauncher.launch(intent) },
                        onSetLoginResultHandler = { handler -> 
                            onAmberLoginResult = handler
                        }
                    )
                }
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Configuration changes (like rotation) are handled automatically by Compose
        // The ViewModel will preserve state across configuration changes
    }
}

@Composable
private fun AppWithNavigation(
    onAmberLogin: (android.content.Intent) -> Unit,
    onSetLoginResultHandler: ((Int, android.content.Intent?) -> Unit) -> Unit
) {
    val viewModel: AppViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val appState by viewModel.appState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Initialize relay repository
    val context = LocalContext.current
    val relayRepository = remember { RelayRepository(context) }
    
    // Remember feed scroll position across navigation with state preservation
    val feedListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = appState.feedScrollPosition
    )
    val profileListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = appState.profileScrollPosition
    )
    val userProfileListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = appState.userProfileScrollPosition
    )
    
    // Track TopAppBarState for dashboard (thread view uses predictive back)
    var dashboardTopAppBarState by remember { mutableStateOf<androidx.compose.material3.TopAppBarState?>(null) }
    
    // Thread state persistence - remember scroll position for each thread
    var threadStates by remember { mutableStateOf<Map<String, LazyListState>>(emptyMap()) }
    
    // Profile state persistence - remember scroll position for each profile
    var profileStates by remember { mutableStateOf<Map<String, LazyListState>>(emptyMap()) }
    
    // Navigation history - tracks the full exploration path
    var navigationHistory by remember { mutableStateOf<List<NavigationEntry>>(listOf()) }
    
    // Function to get the root source (where we originally came from)
    fun getRootSource(): String {
        return navigationHistory.firstOrNull()?.screen ?: "dashboard"
    }
    
    // Function to get the immediate source (where we came from directly)
    fun getImmediateSource(): String {
        return appState.threadSourceScreen ?: "dashboard"
    }
    
    // Function to add entry to navigation history
    fun addToHistory(screen: String, noteId: String? = null, authorId: String? = null) {
        navigationHistory = navigationHistory + NavigationEntry(screen, noteId, authorId)
    }
    
    // Function to go back in history
    fun goBackInHistory(): String? {
        return if (navigationHistory.size > 1) {
            navigationHistory = navigationHistory.dropLast(1)
            navigationHistory.lastOrNull()?.screen
        } else {
            null
        }
    }
    
    // Function to reset history for new exploration
    fun resetHistory() {
        navigationHistory = listOf()
    }
    
    // Function to determine navigation direction for transitions
    fun determineNavigationDirection(initialState: String, targetState: String, history: List<NavigationEntry>): Boolean {
        // If we're going back in history, it's backward navigation
        if (history.size > 1) {
            val lastEntry = history.lastOrNull()
            val previousEntry = history.dropLast(1).lastOrNull()
            
            // If target state matches previous entry, it's backward
            if (lastEntry?.screen == initialState && previousEntry?.screen == targetState) {
                return false // Backward
            }
        }
        
        // Default logic for forward/backward based on screen hierarchy
        return when {
            // Going to detail screens (thread, profile) from list screens is forward
            targetState in listOf("thread", "profile") && initialState in listOf("dashboard", "notifications", "relays") -> true
            // Going back from detail screens to list screens is backward
            initialState in listOf("thread", "profile") && targetState in listOf("dashboard", "notifications", "relays") -> false
            // Going to primary navigation screens from dashboard is forward
            targetState in listOf("relays", "notifications", "settings") && initialState == "dashboard" -> true
            // Going back to dashboard from primary navigation is backward
            initialState in listOf("relays", "notifications", "settings") && targetState == "dashboard" -> false
            // Default to forward for other transitions
            else -> true
        }
    }
    
    // Function to get the proper MaterialSharedAxis transition based on screen relationship
    fun getSharedAxisTransition(initialState: String, targetState: String, isForward: Boolean): Pair<EnterTransition, ExitTransition> {
        return when {
            // Thread/Profile transitions: Use Z-axis for parent-child relationship (drill-down)
            targetState in listOf("thread", "profile") || initialState in listOf("thread", "profile") -> {
                val enterTransition = slideInVertically(
                    initialOffsetY = { if (isForward) it else -it },
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                ) + fadeIn(
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                )
                val exitTransition = slideOutVertically(
                    targetOffsetY = { if (isForward) -it else it },
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardAccelerate)
                ) + fadeOut(
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardAccelerate)
                )
                Pair(enterTransition, exitTransition)
            }
            // Primary navigation: Use X-axis for horizontal navigation
            targetState in listOf("relays", "notifications", "settings") || 
            initialState in listOf("relays", "notifications", "settings") -> {
                val enterTransition = slideInHorizontally(
                    initialOffsetX = { if (isForward) it else -it },
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                ) + fadeIn(
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                )
                val exitTransition = slideOutHorizontally(
                    targetOffsetX = { if (isForward) -it else it },
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardAccelerate)
                ) + fadeOut(
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardAccelerate)
                )
                Pair(enterTransition, exitTransition)
            }
            // Default: Use Fade Through for unrelated transitions
            else -> {
                val enterTransition = fadeIn(
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(300, easing = MaterialMotion.EasingStandardDecelerate)
                )
                val exitTransition = fadeOut(
                    animationSpec = tween(150, easing = MaterialMotion.EasingStandardAccelerate)
                )
                Pair(enterTransition, exitTransition)
            }
        }
    }
    
    // Function to dismiss exit snackbar and reset exit window
    fun dismissExitSnackbar() {
        viewModel.updateShowExitSnackbar(false)
        viewModel.updateExitWindowActive(false)
        viewModel.updateBackPressCount(0)
    }
    
    // Function to get or create thread state for a specific note
    @Composable
    fun getThreadState(noteId: String): LazyListState {
        return threadStates[noteId] ?: rememberLazyListState().also { newState ->
            threadStates = threadStates + (noteId to newState)
        }
    }
    
    // Function to get or create profile state for a specific author
    @Composable
    fun getProfileState(authorId: String): LazyListState {
        return profileStates[authorId] ?: rememberLazyListState().also { newState ->
            profileStates = profileStates + (authorId to newState)
        }
    }
    
    // Save scroll positions when they change
    LaunchedEffect(feedListState.firstVisibleItemIndex) {
        viewModel.updateFeedScrollPosition(feedListState.firstVisibleItemIndex)
    }
    
    LaunchedEffect(profileListState.firstVisibleItemIndex) {
        viewModel.updateProfileScrollPosition(profileListState.firstVisibleItemIndex)
    }
    
    LaunchedEffect(userProfileListState.firstVisibleItemIndex) {
        viewModel.updateUserProfileScrollPosition(userProfileListState.firstVisibleItemIndex)
    }
    
    // Snackbar state for exit confirmation
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle back gesture
    BackHandler(enabled = true) {
        if (viewModel.handleAppExit()) {
            // Exit the app
            (context as ComponentActivity).finish()
        }
    }
    
    // Show exit snackbar when needed
    LaunchedEffect(appState.showExitSnackbar) {
        if (appState.showExitSnackbar) {
            snackbarHostState.showSnackbar(
                message = "Press back again to exit",
                duration = SnackbarDuration.Indefinite
            )
        }
    }
    
    // Reset exit window when navigating away from dashboard
    LaunchedEffect(appState.currentScreen) {
        if (appState.currentScreen != "dashboard") {
            viewModel.updateBackPressCount(0)
            viewModel.updateExitWindowActive(false)
        }
    }
    
    
    // Login handler
    val onLoginClick = {
        val loginIntent = authViewModel.loginWithAmber()
        onAmberLogin(loginIntent)
    }
    
    // Set up login result handler
    LaunchedEffect(Unit) {
        onSetLoginResultHandler { resultCode, data ->
            authViewModel.handleLoginResult(resultCode, data)
        }
    }
    
    // Material Design list item to details page transition
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = appState.currentScreen,
            transitionSpec = {
                // Determine navigation direction based on navigation history
                val isForward = determineNavigationDirection(initialState, targetState, navigationHistory)
                
                // Get the proper shared axis transition based on screen relationship
                val (enterTransition, exitTransition) = getSharedAxisTransition(initialState, targetState, isForward)
                
                enterTransition togetherWith exitTransition
            },
            label = "list_to_details_transition"
        ) { currentScreen ->
            when (currentScreen) {
                "dashboard" -> {
                    DashboardScreen(
                        isSearchMode = appState.isSearchMode,
                        onSearchModeChange = { 
                            dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                            viewModel.updateSearchMode(it) 
                        },
                        onProfileClick = { authorId ->
                            dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                            val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                            if (author != null) {
                                viewModel.updateSelectedAuthor(author)
                                viewModel.updateThreadSourceScreen("dashboard")
                                // Initialize navigation history
                                resetHistory()
                                addToHistory("dashboard")
                                addToHistory("profile", authorId = author.id)
                                viewModel.updateCurrentScreen("profile")
                            }
                        },
                        onNavigateTo = { screen -> 
                            dismissExitSnackbar() // Dismiss exit snackbar on any navigation
                            // Add to navigation history for proper transition direction
                            addToHistory(screen)
                            viewModel.updateCurrentScreen(screen) 
                        },
                        onThreadClick = { note ->
                            dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                            viewModel.updateSelectedNote(note)
                            viewModel.updateThreadSourceScreen("dashboard")
                            // Initialize navigation history
                            resetHistory()
                            addToHistory("dashboard")
                            addToHistory("thread", noteId = note.id)
                            viewModel.updateCurrentScreen("thread")
                        },
                        onScrollToTop = { /* Scroll to top handled in DashboardScreen */ },
                        onLoginClick = onLoginClick,
                        onTopAppBarStateChange = { state -> dashboardTopAppBarState = state },
                        listState = feedListState
                    )
                }
                "profile" -> {
                    appState.selectedAuthor?.let { author ->
                        val authorNotes = SampleData.sampleNotes.filter { it.author.id == author.id }
                        val profileListState = getProfileState(author.id)
                        
                        ProfileScreen(
                            author = author,
                            authorNotes = authorNotes,
                            listState = profileListState,
                            onBackClick = { 
                                // Go back in navigation history
                                val previousScreen = goBackInHistory()
                                if (previousScreen != null) {
                                    viewModel.updateCurrentScreen(previousScreen)
                                } else {
                                    // Fallback to immediate source
                                    val sourceScreen = appState.threadSourceScreen ?: "dashboard"
                                    viewModel.updateCurrentScreen(sourceScreen)
                                }
                            },
                            onNoteClick = { note ->
                                dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                                viewModel.updateSelectedNote(note)
                                viewModel.updateThreadSourceScreen("profile")
                                // Add to navigation history
                                addToHistory("thread", noteId = note.id)
                                viewModel.updateCurrentScreen("thread")
                            },
                            onProfileClick = { authorId ->
                                dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                                val newAuthor = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                                if (newAuthor != null) {
                                    viewModel.updateSelectedAuthor(newAuthor)
                                    // Update thread source to remember we came from profile
                                    viewModel.updateThreadSourceScreen("profile")
                                    // Add to navigation history
                                    addToHistory("profile", authorId = newAuthor.id)
                                }
                            }
                        )
                    }
                }
                "about" -> {
                    AboutScreen(
                        onBackClick = { 
                            val targetScreen = if (appState.previousScreen == "settings") "settings" else "dashboard"
                            viewModel.updateCurrentScreen(targetScreen)
                            viewModel.updatePreviousScreen(null)
                        }
                    )
                }
                "settings" -> {
                    AnimatedContent(
                        targetState = appState.currentScreen,
                        transitionSpec = {
                            when {
                                // Settings sub-pages use Shared Axis X (horizontal slide)
                                targetState == "appearance" && initialState == "settings" -> {
                                    MaterialMotion.SharedAxisX.enterTransition(forward = true) togetherWith
                                    MaterialMotion.SharedAxisX.exitTransition(forward = true)
                                }
                                targetState == "settings" && initialState == "appearance" -> {
                                    MaterialMotion.SharedAxisX.enterTransition(forward = false) togetherWith
                                    MaterialMotion.SharedAxisX.exitTransition(forward = false)
                                }
                                // Other settings sub-pages (when implemented)
                                targetState in listOf("general", "notifications_settings", "account_preferences", "filters_blocks", "data_storage", "advanced") && initialState == "settings" -> {
                                    MaterialMotion.SharedAxisX.enterTransition(forward = true) togetherWith
                                    MaterialMotion.SharedAxisX.exitTransition(forward = true)
                                }
                                targetState == "settings" && initialState in listOf("general", "notifications_settings", "account_preferences", "filters_blocks", "data_storage", "advanced") -> {
                                    MaterialMotion.SharedAxisX.enterTransition(forward = false) togetherWith
                                    MaterialMotion.SharedAxisX.exitTransition(forward = false)
                                }
                                else -> {
                                    MaterialMotion.FadeThrough.enterTransition() togetherWith
                                    MaterialMotion.FadeThrough.exitTransition()
                                }
                            }
                        },
                        label = "settings_transition"
                    ) { currentScreen ->
                        when (currentScreen) {
                            "settings" -> {
                                SettingsScreen(
                                    onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                                    onNavigateTo = { screen -> 
                                        viewModel.updatePreviousScreen("settings")
                                        viewModel.updateCurrentScreen(screen)
                                    },
                                    onBugReportClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                        intent.data = android.net.Uri.parse("https://github.com/TekkadanPlays/ribbit-android/issues")
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            "appearance" -> {
                                AppearanceSettingsScreen(
                                    onBackClick = { viewModel.updateCurrentScreen("settings") }
                                )
                            }
                        }
                    }
                }
                "relays" -> {
                    RelayManagementScreen(
                        onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                        relayRepository = relayRepository
                    )
                }
                "notifications" -> {
                    NotificationsScreen(
                        onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                        onNoteClick = { note ->
                            dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                            viewModel.updateSelectedNote(note)
                            viewModel.updateThreadSourceScreen("notifications")
                            viewModel.updateCurrentScreen("thread")
                        },
                        onLike = { noteId -> /* TODO: Handle like */ },
                        onShare = { noteId -> /* TODO: Handle share */ },
                        onComment = { noteId -> /* TODO: Handle comment */ },
                        onProfileClick = { authorId ->
                            dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                            val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                            if (author != null) {
                                viewModel.updateSelectedAuthor(author)
                                viewModel.updateThreadSourceScreen("notifications")
                                viewModel.updateCurrentScreen("profile")
                            }
                        }
                    )
                }
                "user_profile" -> {
                    // Convert auth state to Author for ProfileScreen
                    val currentUser = authState.userProfile?.let { userProfile ->
                        Author(
                            id = userProfile.pubkey,
                            username = userProfile.name ?: "user",
                            displayName = userProfile.displayName ?: userProfile.name ?: "User",
                            avatarUrl = userProfile.picture,
                            isVerified = false
                        )
                    } ?: Author(
                        id = "guest",
                        username = "guest",
                        displayName = "Guest User",
                        avatarUrl = null,
                        isVerified = false
                    )
                    
                    // For now, show empty notes list - in real app, fetch user's notes
                    val userNotes = emptyList<Note>()
                    
                    ProfileScreen(
                        author = currentUser,
                        authorNotes = userNotes,
                        onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                        onNoteClick = { note ->
                            viewModel.updateSelectedNote(note)
                            viewModel.updateThreadSourceScreen("user_profile")
                            viewModel.updateCurrentScreen("thread")
                        },
                        onProfileClick = { authorId ->
                            val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                            if (author != null) {
                                viewModel.updateSelectedAuthor(author)
                                viewModel.updateCurrentScreen("profile")
                            }
                        },
                        onNavigateTo = { screen -> viewModel.updateCurrentScreen(screen) },
                        listState = userProfileListState
                    )
                }
                "thread" -> {
                    appState.selectedNote?.let { note ->
                        val sampleComments = createSampleCommentThreads()
                        val threadListState = getThreadState(note.id)
                        
                        ModernThreadViewScreen(
                            note = note,
                            comments = sampleComments,
                            listState = threadListState,
                            onBackClick = { 
                                // Go back in navigation history
                                val previousScreen = goBackInHistory()
                                if (previousScreen != null) {
                                    viewModel.updateCurrentScreen(previousScreen)
                                } else {
                                    // Fallback to root source
                                    val sourceScreen = getRootSource()
                                    viewModel.updateCurrentScreen(sourceScreen)
                                }
                            },
                            onLike = { noteId -> /* TODO: Handle like */ },
                            onShare = { noteId -> /* TODO: Handle share */ },
                            onComment = { noteId -> /* TODO: Handle comment */ },
                            onProfileClick = { authorId ->
                                dismissExitSnackbar() // Dismiss exit snackbar on any interaction
                                val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                                if (author != null) {
                                    viewModel.updateSelectedAuthor(author)
                                    viewModel.updateThreadSourceScreen("thread") // Mark that we're coming from thread
                                    // Add to navigation history
                                    addToHistory("profile", authorId = author.id)
                                    viewModel.updateCurrentScreen("profile")
                                }
                            },
                            onCommentLike = { commentId -> /* TODO: Handle comment like */ },
                            onCommentReply = { commentId -> /* TODO: Handle comment reply */ }
                        )
                    }
                }
            }
        }
        
        // Custom styled SnackbarHost for exit confirmation - positioned above navigation bar
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { snackbarData ->
                CustomExitSnackbar(
                    snackbarData = snackbarData,
                    modifier = Modifier.padding(16.dp)
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // Position above navigation bar
        )
    }
}

@Composable
private fun CustomExitSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Subtle accent indicator
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Message text with consistent typography
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Subtle close indicator
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Sample data for comment threads
fun createSampleCommentThreads(): List<com.example.views.ui.screens.CommentThread> {
    val sampleAuthors = SampleData.sampleAuthors
    return listOf(
        com.example.views.ui.screens.CommentThread(
            comment = com.example.views.data.Comment(
                id = "comment1",
                author = sampleAuthors[1],
                content = "This is a great post! I totally agree with your perspective.",
                timestamp = System.currentTimeMillis() - 3600000,
                likes = 5,
                isLiked = false
            ),
            replies = listOf(
                com.example.views.ui.screens.CommentThread(
                    comment = com.example.views.data.Comment(
                        id = "comment1_reply1",
                        author = sampleAuthors[2],
                        content = "I second that! Really insightful thoughts here.",
                        timestamp = System.currentTimeMillis() - 3000000,
                        likes = 2,
                        isLiked = true
                    )
                )
            )
        )
    )
}