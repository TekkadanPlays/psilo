package com.example.views.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.views.data.Note
import com.example.views.data.SampleData
import com.example.views.ui.components.AdaptiveHeader
import com.example.views.ui.components.BottomNavigationBar
import com.example.views.ui.components.BottomNavDestinations
import com.example.views.ui.components.ModernSidebar
import com.example.views.ui.components.NoteCard
import com.example.views.ui.components.SearchResults
import com.example.views.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isSearchMode: Boolean = false,
    onSearchModeChange: (Boolean) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    viewModel: DashboardViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Search state
    var searchQuery by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var searchResults by remember { mutableStateOf<List<Note>>(emptyList()) }
    
    // Use Material3's built-in scroll behavior for top app bar
    // Use pinned behavior in search mode to prevent UI issues, enterAlways when not searching
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = if (isSearchMode) {
        TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    }
    
    // Filter results based on search query
    LaunchedEffect(searchQuery.text, uiState.notes) {
        searchResults = if (searchQuery.text.isBlank()) {
            emptyList()
        } else {
            uiState.notes.filter { note ->
                note.content.contains(searchQuery.text, ignoreCase = true) ||
                note.author.displayName.contains(searchQuery.text, ignoreCase = true) ||
                note.author.username.contains(searchQuery.text, ignoreCase = true) ||
                note.hashtags.any { it.contains(searchQuery.text, ignoreCase = true) }
            }
        }
    }
    
    
    ModernSidebar(
        drawerState = drawerState,
        onItemClick = { itemId -> viewModel.onSidebarItemClick(itemId) },
        modifier = modifier
    ) {
        Scaffold(
            modifier = if (isSearchMode) {
                Modifier
            } else {
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            },
            topBar = {
                AdaptiveHeader(
                    title = "Ribbit",
                    isSearchMode = isSearchMode,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onMenuClick = { 
                        scope.launch {
                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }
                        }
                    },
                    onSearchClick = { onSearchModeChange(true) },
                    onNotificationsClick = { /* TODO: Handle notifications */ },
                    onMoreOptionClick = { option ->
                        when (option) {
                            "about" -> onNavigateTo("about")
                            "settings" -> onNavigateTo("settings")
                            else -> viewModel.onMoreOptionClick(option)
                        }
                    },
                    onBackClick = { onSearchModeChange(false) },
                    onClearSearch = { 
                        searchQuery = androidx.compose.ui.text.input.TextFieldValue("")
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                if (!isSearchMode) {
                    BottomNavigationBar(
                        currentDestination = "home", // Always show home as selected on dashboard
                        onDestinationClick = { destination -> 
                            when (destination) {
                                "home" -> {
                                    // Scroll to top when already on dashboard and expand header
                                    scope.launch {
                                        // Expand the top app bar
                                        topAppBarState.heightOffset = 0f
                                        // Smooth scroll to top
                                        listState.animateScrollToItem(0)
                                    }
                                }
                                "search" -> onSearchModeChange(true)
                                "profile" -> onNavigateTo("user_profile")
                                else -> { /* Other destinations not implemented yet */ }
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            if (isSearchMode) {
                // Search mode - show search results
                SearchResults(
                    isVisible = isSearchMode,
                    searchQuery = searchQuery.text,
                    searchResults = searchResults,
                    onNoteClick = { note ->
                        // Handle note click in search results
                        onSearchModeChange(false)
                    },
                    onRecentSearchClick = { query ->
                        searchQuery = androidx.compose.ui.text.input.TextFieldValue(query)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                // Normal mode - show notes feed
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.notes) { note ->
                        NoteCard(
                            note = note,
                            onLike = { noteId -> viewModel.toggleLike(noteId) },
                            onShare = { noteId -> viewModel.shareNote(noteId) },
                            onComment = { noteId -> viewModel.commentOnNote(noteId) },
                            onProfileClick = onProfileClick
                        )
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen()
    }
}
