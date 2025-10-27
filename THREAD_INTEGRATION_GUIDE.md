# Thread View Integration Guide
## Implementing Kind 1111 Replies in ModernThreadViewScreen

This guide explains how to integrate the `ThreadRepliesViewModel` and `ThreadRepliesRepository` into your thread view to display kind 1111 replies.

---

## Quick Start

### 1. Update ModernThreadViewScreen Parameters

Add the `ThreadRepliesViewModel` to your screen composable:

```kotlin
@Composable
fun ModernThreadViewScreen(
    note: Note,
    comments: List<CommentThread>,
    listState: LazyListState = rememberLazyListState(),
    // Add these parameters:
    threadRepliesViewModel: ThreadRepliesViewModel = viewModel(),
    relayUrls: List<String> = emptyList(),
    // ... existing parameters
) {
    // Collect thread replies state
    val repliesState by threadRepliesViewModel.uiState.collectAsState()
    
    // Load replies when screen opens
    LaunchedEffect(note.id) {
        if (relayUrls.isNotEmpty()) {
            threadRepliesViewModel.loadRepliesForNote(note, relayUrls)
        }
    }
    
    // ... rest of implementation
}
```

---

## Step-by-Step Integration

### Step 1: Add ViewModel to Screen

In `ModernThreadViewScreen.kt`, import and add the view model:

```kotlin
import com.example.views.viewmodel.ThreadRepliesViewModel
import com.example.views.data.ThreadReply
import com.example.views.data.ThreadedReply
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ModernThreadViewScreen(
    note: Note,
    threadRepliesViewModel: ThreadRepliesViewModel = viewModel(),
    relayUrls: List<String> = emptyList(),
    // ... other parameters
) {
    val repliesState by threadRepliesViewModel.uiState.collectAsState()
    
    // ...
}
```

---

### Step 2: Load Replies on Screen Mount

Add a `LaunchedEffect` to fetch replies when the thread opens:

```kotlin
// Inside ModernThreadViewScreen composable
LaunchedEffect(note.id, relayUrls) {
    if (relayUrls.isNotEmpty()) {
        android.util.Log.d("ThreadView", "Loading ${relayUrls.size} relays for note ${note.id.take(8)}")
        threadRepliesViewModel.loadRepliesForNote(note, relayUrls)
    }
}
```

---

### Step 3: Display Loading State

Show loading animation while fetching replies:

```kotlin
import com.example.views.ui.components.LoadingAnimation

if (repliesState.isLoading && repliesState.replies.isEmpty()) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingAnimation(indicatorSize = 32.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading replies...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

### Step 4: Display Threaded Replies

Replace the existing comments list with threaded replies:

```kotlin
// In your LazyColumn
item {
    // Display the main note first
    NoteCard(
        note = note,
        onLike = onLike,
        onShare = onShare,
        onComment = onComment,
        onProfileClick = onProfileClick,
        onNoteClick = {},
        modifier = Modifier.fillMaxWidth()
    )
    
    Divider(modifier = Modifier.padding(vertical = 8.dp))
    
    // Reply count header
    Text(
        text = "${repliesState.totalReplyCount} replies",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// Display threaded replies
items(repliesState.threadedReplies) { threadedReply ->
    ThreadedReplyCard(
        threadedReply = threadedReply,
        onLike = { replyId -> threadRepliesViewModel.likeReply(replyId) },
        onReply = onCommentReply,
        onProfileClick = onProfileClick,
        modifier = Modifier.fillMaxWidth()
    )
}
```

---

### Step 5: Create ThreadedReplyCard Composable

Create a new composable to display individual replies with proper indentation:

```kotlin
@Composable
fun ThreadedReplyCard(
    threadedReply: ThreadedReply,
    onLike: (String) -> Unit,
    onReply: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reply = threadedReply.reply
    val level = threadedReply.level
    val indentWidth = (level * 16).dp
    
    Column(
        modifier = modifier.padding(start = indentWidth)
    ) {
        // Reply card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f - (level * 0.05f) // Subtle depth indication
                )
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Author info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfilePicture(
                        avatarUrl = reply.author.avatarUrl,
                        size = 32.dp,
                        onClick = { onProfileClick(reply.author.id) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reply.author.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTimestamp(reply.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Thread level indicator
                    if (level > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "↳ $level",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Reply content
                Text(
                    text = reply.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        // Like button
                        IconButton(
                            onClick = { onLike(reply.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (reply.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (reply.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (reply.likes > 0) {
                            Text(
                                text = reply.likes.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Reply button
                        IconButton(
                            onClick = { onReply(reply.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Reply,
                                contentDescription = "Reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Recursively display child replies
        threadedReply.children.forEach { childReply ->
            ThreadedReplyCard(
                threadedReply = childReply,
                onLike = onLike,
                onReply = onReply,
                onProfileClick = onProfileClick,
                modifier = modifier
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
```

---

### Step 6: Add Sort Controls

Allow users to change reply sort order:

```kotlin
// Add to your TopAppBar actions or as a separate control
var showSortMenu by remember { mutableStateOf(false) }

IconButton(onClick = { showSortMenu = true }) {
    Icon(
        imageVector = Icons.Default.Sort,
        contentDescription = "Sort replies"
    )
}

DropdownMenu(
    expanded = showSortMenu,
    onDismissRequest = { showSortMenu = false }
) {
    DropdownMenuItem(
        text = { Text("Oldest first") },
        onClick = {
            threadRepliesViewModel.setSortOrder(ReplySortOrder.CHRONOLOGICAL)
            showSortMenu = false
        }
    )
    DropdownMenuItem(
        text = { Text("Newest first") },
        onClick = {
            threadRepliesViewModel.setSortOrder(ReplySortOrder.REVERSE_CHRONOLOGICAL)
            showSortMenu = false
        }
    )
    DropdownMenuItem(
        text = { Text("Most liked") },
        onClick = {
            threadRepliesViewModel.setSortOrder(ReplySortOrder.MOST_LIKED)
            showSortMenu = false
        }
    )
}
```

---

### Step 7: Add Pull-to-Refresh for Replies

Wrap your content in `PullToRefreshBox`:

```kotlin
PullToRefreshBox(
    isRefreshing = repliesState.isLoading,
    onRefresh = {
        threadRepliesViewModel.refreshReplies(relayUrls)
    }
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        // ... your items
    }
}
```

---

## Advanced Features

### Collapsible Thread Branches

Add collapse/expand functionality for nested replies:

```kotlin
@Composable
fun CollapsibleThreadedReplyCard(
    threadedReply: ThreadedReply,
    // ... other parameters
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column {
        Card {
            // ... reply content
            
            // Collapse button for replies with children
            if (threadedReply.children.isNotEmpty()) {
                TextButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                    Text("${threadedReply.children.size} replies")
                }
            }
        }
        
        // Show children only if expanded
        if (isExpanded) {
            threadedReply.children.forEach { child ->
                CollapsibleThreadedReplyCard(
                    threadedReply = child,
                    // ... other parameters
                )
            }
        }
    }
}
```

---

### Reply Count Badge

Show reply statistics in the thread header:

```kotlin
@Composable
fun ReplyStatsHeader(repliesViewModel: ThreadRepliesViewModel) {
    val state by repliesViewModel.uiState.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${state.totalReplyCount} total replies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${repliesViewModel.getDirectRepliesCount()} direct · ${repliesViewModel.getNestedRepliesCount()} nested",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Sort indicator
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = when (state.sortOrder) {
                    ReplySortOrder.CHRONOLOGICAL -> "Oldest"
                    ReplySortOrder.REVERSE_CHRONOLOGICAL -> "Newest"
                    ReplySortOrder.MOST_LIKED -> "Top"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
```

---

## Navigation Integration

### Pass Relay URLs to Thread View

In `RibbitNavigation.kt`, ensure relay URLs are passed to the thread screen:

```kotlin
composable(
    route = "thread/{noteId}",
    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
) { backStackEntry ->
    val noteId = backStackEntry.arguments?.getString("noteId")
    val note = /* fetch note by ID */
    
    // Get relay URLs from RelayStorageManager
    val context = LocalContext.current
    val storageManager = remember { RelayStorageManager(context) }
    val accountViewModel: AccountStateViewModel = viewModel()
    val currentAccount by accountViewModel.currentAccount.collectAsState()
    
    val relayUrls = remember(currentAccount) {
        currentAccount?.toHexKey()?.let { pubkey ->
            val categories = storageManager.loadCategories(pubkey)
            val favoriteCategory = categories.firstOrNull { it.isFavorite }
                ?: categories.firstOrNull { it.isDefault }
            
            favoriteCategory?.relays?.map { it.url } ?: emptyList()
        } ?: emptyList()
    }
    
    ModernThreadViewScreen(
        note = note,
        relayUrls = relayUrls,
        threadRepliesViewModel = viewModel(),
        // ... other parameters
    )
}
```

---

## Error Handling

### Display Error State

Show error messages when reply loading fails:

```kotlin
if (repliesState.error != null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Failed to load replies",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = repliesState.error ?: "Unknown error",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

---

## Performance Tips

1. **Lazy Loading**: Only load replies when thread is opened
2. **Pagination**: Implement pagination for threads with 100+ replies
3. **Caching**: Use `remember` for computed values
4. **Debouncing**: Debounce like/reply actions to prevent spam

```kotlin
// Debounced like action
var lastLikeTime by remember { mutableStateOf(0L) }
val debouncedLike: (String) -> Unit = { replyId ->
    val now = System.currentTimeMillis()
    if (now - lastLikeTime > 500) {
        threadRepliesViewModel.likeReply(replyId)
        lastLikeTime = now
    }
}
```

---

## Testing Checklist

- [ ] Replies load when thread opens
- [ ] Loading indicator displays during fetch
- [ ] Threaded replies display with correct indentation
- [ ] Nested replies render recursively
- [ ] Like button updates reply state
- [ ] Sort order changes work correctly
- [ ] Pull-to-refresh reloads replies
- [ ] Error state displays when loading fails
- [ ] Empty state shows when no replies exist
- [ ] Navigation passes correct relay URLs

---

## Complete Example

See `ModernThreadViewScreen.kt` for the complete implementation. Key integration points:

1. ViewModel injection
2. LaunchedEffect for loading
3. State collection
4. UI rendering with ThreadedReplyCard
5. Action handlers

---

## Troubleshooting

### Replies Not Loading

**Check:**
- Relay URLs are being passed correctly
- NostrClient is connected
- Kind 1111 filter is correct
- Network permissions are granted

**Debug:**
```kotlin
LaunchedEffect(note.id, relayUrls) {
    Log.d("ThreadView", "Loading replies for ${note.id}")
    Log.d("ThreadView", "Using relays: $relayUrls")
    threadRepliesViewModel.loadRepliesForNote(note, relayUrls)
}
```

### Indentation Not Working

Ensure `level` is being calculated correctly in `parseThreadTags()`:

```kotlin
val (rootId, replyToId, level) = ThreadReply.parseThreadTags(tags)
Log.d("ThreadReply", "Reply $id: root=$rootId, parent=$replyToId, level=$level")
```

---

## Next Steps

1. Implement reply composer for creating kind 1111 events
2. Add profile fetching for reply authors
3. Implement reply notifications
4. Add thread bookmarking
5. Create quote reply functionality

---

**Last Updated**: January 2025  
**Status**: Ready for integration