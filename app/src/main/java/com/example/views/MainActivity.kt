package com.example.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.example.views.data.Author
import com.example.views.data.Note
import com.example.views.data.SampleData
import com.example.views.ui.navigation.MaterialMotion
import com.example.views.ui.screens.AboutScreen
import com.example.views.ui.screens.AppearanceSettingsScreen
import com.example.views.ui.screens.DashboardScreen
import com.example.views.ui.screens.ProfileScreen
import com.example.views.ui.screens.SettingsScreen
import com.example.views.ui.screens.ModernThreadViewScreen
import com.example.views.ui.screens.RelayManagementScreen
import com.example.views.ui.screens.createSampleCommentThreads
import com.example.views.repository.RelayRepository
import com.example.views.ui.theme.ViewsTheme
import com.example.views.viewmodel.AppViewModel
import com.example.views.viewmodel.AuthViewModel

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
    val context = androidx.compose.ui.platform.LocalContext.current
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
    
    // Optimized navigation - minimal recomposition scope
    when (appState.currentScreen) {
            "dashboard" -> {
                DashboardScreen(
                    isSearchMode = appState.isSearchMode,
                    onSearchModeChange = { viewModel.updateSearchMode(it) },
                    onProfileClick = { authorId ->
                        val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                        if (author != null) {
                            viewModel.updateSelectedAuthor(author)
                            viewModel.updateCurrentScreen("profile")
                        }
                    },
                    onNavigateTo = { screen -> viewModel.updateCurrentScreen(screen) },
                    onThreadClick = { note ->
                        viewModel.updateSelectedNote(note)
                        viewModel.updateThreadSourceScreen("dashboard")
                        viewModel.updateCurrentScreen("thread")
                    },
                    onScrollToTop = { /* Scroll to top handled in DashboardScreen */ },
                    onLoginClick = onLoginClick,
                    listState = feedListState
                )
            }
            "profile" -> {
                appState.selectedAuthor?.let { author ->
                    val authorNotes = SampleData.sampleNotes.filter { it.author.id == author.id }
                    ProfileScreen(
                        author = author,
                        authorNotes = authorNotes,
                        onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                        onNoteClick = { note ->
                            viewModel.updateSelectedNote(note)
                            viewModel.updateThreadSourceScreen("profile")
                            viewModel.updateCurrentScreen("thread")
                        },
                        onProfileClick = { authorId ->
                            val newAuthor = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                            if (newAuthor != null) {
                                viewModel.updateSelectedAuthor(newAuthor)
                            }
                        },
                        listState = profileListState
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
                SettingsScreen(
                    onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                    onNavigateTo = { screen -> 
                        viewModel.updatePreviousScreen("settings")
                        viewModel.updateCurrentScreen(screen)
                    }
                )
            }
            "appearance" -> {
                AppearanceSettingsScreen(
                    onBackClick = { viewModel.updateCurrentScreen("settings") }
                )
            }
            "relays" -> {
                RelayManagementScreen(
                    onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                    relayRepository = relayRepository
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
                    ModernThreadViewScreen(
                        note = note,
                        comments = sampleComments,
                        onBackClick = { viewModel.updateCurrentScreen("dashboard") },
                        onLike = { noteId -> /* TODO: Handle like */ },
                        onShare = { noteId -> /* TODO: Handle share */ },
                        onComment = { noteId -> /* TODO: Handle comment */ },
                        onProfileClick = { authorId ->
                            val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                            if (author != null) {
                                viewModel.updateSelectedAuthor(author)
                                viewModel.updateThreadSourceScreen("thread") // Mark that we're coming from thread
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
