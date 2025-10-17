package com.example.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.views.data.Author
import com.example.views.data.SampleData
import com.example.views.ui.screens.AboutScreen
import com.example.views.ui.screens.DashboardScreen
import com.example.views.ui.screens.ProfileScreen
import com.example.views.ui.screens.SettingsScreen
import com.example.views.ui.theme.ViewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViewsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithNavigation()
                }
            }
        }
    }
}

@Composable
private fun AppWithNavigation() {
    var currentScreen by remember { mutableStateOf("dashboard") }
    var isSearchMode by remember { mutableStateOf(false) }
    var selectedAuthor by remember { mutableStateOf<Author?>(null) }
    
    // Track navigation stack for proper back handling
    var previousScreen by remember { mutableStateOf<String?>(null) }
    
    // Remember feed scroll position across navigation
    val feedListState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Handle back gesture
    BackHandler(enabled = isSearchMode || currentScreen != "dashboard") {
        when {
            isSearchMode -> isSearchMode = false
            currentScreen == "about" && previousScreen == "settings" -> currentScreen = "settings"
            currentScreen != "dashboard" -> currentScreen = "dashboard"
        }
    }
    
    when (currentScreen) {
        "dashboard" -> {
            DashboardScreen(
                isSearchMode = isSearchMode,
                onSearchModeChange = { isSearchMode = it },
                onProfileClick = { authorId ->
                    val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                    if (author != null) {
                        selectedAuthor = author
                        currentScreen = "profile"
                    }
                },
                onNavigateTo = { screen -> currentScreen = screen },
                onScrollToTop = { /* Scroll to top handled in DashboardScreen */ },
                listState = feedListState // Pass the remembered list state
            )
        }
        "profile" -> {
            selectedAuthor?.let { author ->
                val authorNotes = SampleData.sampleNotes.filter { it.author.id == author.id }
                ProfileScreen(
                    author = author,
                    authorNotes = authorNotes,
                    onBackClick = { currentScreen = "dashboard" },
                    onProfileClick = { authorId ->
                        val newAuthor = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                        if (newAuthor != null) {
                            selectedAuthor = newAuthor
                        }
                    }
                )
            }
        }
        "about" -> {
            AboutScreen(
                onBackClick = { 
                    currentScreen = if (previousScreen == "settings") "settings" else "dashboard"
                    previousScreen = null
                }
            )
        }
        "settings" -> {
            SettingsScreen(
                onBackClick = { currentScreen = "dashboard" },
                onNavigateTo = { screen -> 
                    previousScreen = "settings"
                    currentScreen = screen 
                }
            )
        }
        "user_profile" -> {
            // User's own profile - use the first sample author as the current user
            val currentUser = SampleData.sampleAuthors.first()
            val userNotes = SampleData.sampleNotes.filter { it.author.id == currentUser.id }
            ProfileScreen(
                author = currentUser,
                authorNotes = userNotes,
                onBackClick = { currentScreen = "dashboard" },
                onProfileClick = { authorId ->
                    val author = SampleData.sampleNotes.find { it.author.id == authorId }?.author
                    if (author != null) {
                        selectedAuthor = author
                        currentScreen = "profile"
                    }
                },
                onNavigateTo = { screen -> currentScreen = screen }
            )
        }
    }
}