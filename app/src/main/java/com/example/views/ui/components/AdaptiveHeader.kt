package com.example.views.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveHeader(
    title: String = "Ribbit",
    isSearchMode: Boolean = false,
    showBackArrow: Boolean = false,
    searchQuery: TextFieldValue = TextFieldValue(""),
    onSearchQueryChange: (TextFieldValue) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onMoreOptionClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    onLoginClick: (() -> Unit)? = null,
    isGuest: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Show keyboard when search mode is activated
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            if (isSearchMode) {
                // Search mode - just a simple text field
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { 
                        Text(
                            "Search notes...",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp) // Add padding to match title spacing
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            } else {
                // Normal mode - show title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (isSearchMode || showBackArrow) {
                // Search mode or back arrow mode - show back button
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // Normal mode - show menu button
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (isSearchMode) {
                // Search mode - no actions, just the clear button in the text field
            } else {
                // Normal mode - show normal actions
                Row {
                    // Search button
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Filter/Sort button
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter/Sort",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // More options menu
                    MoreOptionsMenu(
                        onMoreOptionClick = onMoreOptionClick,
                        onLoginClick = onLoginClick,
                        isGuest = isGuest
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

@Composable
private fun MoreOptionsMenu(
    onMoreOptionClick: (String) -> Unit,
    onLoginClick: (() -> Unit)? = null,
    isGuest: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Show login option only for guests
            if (isGuest && onLoginClick != null) {
                DropdownMenuItem(
                    text = { Text("Log In") },
                    leadingIcon = { Icon(Icons.Outlined.Login, contentDescription = null) },
                    onClick = { 
                        onLoginClick()
                        expanded = false
                    }
                )
                HorizontalDivider()
            }
            
            DropdownMenuItem(
                text = { Text("Post View") },
                leadingIcon = { Icon(Icons.Outlined.Create, contentDescription = null) },
                onClick = { 
                    onMoreOptionClick("post_view")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Refresh") },
                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                onClick = { 
                    onMoreOptionClick("refresh")
                    expanded = false
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = { 
                    onMoreOptionClick("settings")
                    expanded = false
                }
            )
        }
    }
}

