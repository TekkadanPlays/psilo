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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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
    
    // Handle back gesture
    BackHandler(enabled = appState.isSearchMode || appState.currentScreen != "dashboard") {
        viewModel.navigateBack()
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
    AnimatedContent(
        targetState = appState.currentScreen,
        transitionSpec = {
            when {
                // List item to details: Thread view expands from note card
                targetState == "thread" && initialState != "thread" -> {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) + fadeIn(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    ) + fadeOut(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    )
                }
                // List item to details: Profile view expands from profile card
                targetState == "profile" && initialState != "thread" && initialState != "profile" -> {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) + fadeIn(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    ) + fadeOut(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    )
                }
                // Details to list item: Thread view contracts back to note card
                initialState == "thread" && targetState != "thread" -> {
                    slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) + fadeIn(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    ) + fadeOut(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    )
                }
                // Details to list item: Profile view contracts back to profile card
                initialState == "profile" && targetState != "thread" -> {
                    slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) + fadeIn(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    ) + fadeOut(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    )
                }
                // Thread to profile: Return to profile from thread
                initialState == "thread" && targetState == "profile" -> {
                    slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) + fadeIn(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedDecelerate)
                    ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    ) + fadeOut(
                        animationSpec = tween(300, easing = MaterialMotion.EasingEmphasizedAccelerate)
                    )
                }
                // Other transitions use fade through
                else -> {
                    MaterialMotion.FadeThrough.enterTransition() togetherWith
                    MaterialMotion.FadeThrough.exitTransition()
                }
            }
        },
        label = "list_to_details_transition"
    ) { currentScreen ->
        when (currentScreen) {
            "dashboard" -> {
                DashboardScreen(
                    isSearchMode = appState.isSearchMode,
                    onSearchModeChange = { viewModel.updateSearchMode(it) },
                    onProfileClick = { authorId ->
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
                    onNavigateTo = { screen -> viewModel.updateCurrentScreen(screen) },
                    onThreadClick = { note ->
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
                            viewModel.updateSelectedNote(note)
                            viewModel.updateThreadSourceScreen("profile")
                            // Add to navigation history
                            addToHistory("thread", noteId = note.id)
                            viewModel.updateCurrentScreen("thread")
                        },
                        onProfileClick = { authorId ->
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
                            // Settings sub-pages use shared x-axis transitions
                            targetState == "appearance" && initialState == "settings" -> {
                                MaterialMotion.SharedAxisX.enterTransition() togetherWith
                                MaterialMotion.SharedAxisX.exitTransition()
                            }
                            targetState == "settings" && initialState == "appearance" -> {
                                MaterialMotion.SharedAxisX.enterTransition() togetherWith
                                MaterialMotion.SharedAxisX.exitTransition()
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
                        viewModel.updateSelectedNote(note)
                        viewModel.updateThreadSourceScreen("notifications")
                        viewModel.updateCurrentScreen("thread")
                    },
                    onLike = { noteId -> /* TODO: Handle like */ },
                    onShare = { noteId -> /* TODO: Handle share */ },
                    onComment = { noteId -> /* TODO: Handle comment */ },
                    onProfileClick = { authorId ->
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
}

// Sample data for comment threads
private fun createSampleCommentThreads(): List<com.example.views.ui.screens.CommentThread> {
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
