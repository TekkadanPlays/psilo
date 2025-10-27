package com.example.views.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.views.data.Author
import com.example.views.data.Comment
import com.example.views.data.Note
import com.example.views.viewmodel.Kind1RepliesViewModel
import com.example.views.viewmodel.ThreadRepliesUiState
import com.example.views.data.ThreadReply
import com.example.views.data.ThreadedReply
import com.example.views.data.toThreadReply
import com.example.views.data.toNote
import com.example.views.data.SampleData
import com.example.views.repository.RelayStorageManager
import com.example.views.ui.components.AdaptiveHeader
import com.example.views.ui.components.BottomNavigationBar
import com.example.views.ui.components.NoteCard
import com.example.views.ui.components.ProfilePicture
import com.example.views.ui.components.ZapButtonWithMenu
import com.example.views.ui.components.ZapMenuRow
import com.example.views.ui.components.LoadingAnimation
import com.example.views.ui.icons.ArrowDownward
import com.example.views.ui.icons.ArrowUpward
import com.example.views.ui.icons.Bolt
import com.example.views.ui.icons.Bookmark
import com.example.views.viewmodel.ThreadRepliesViewModel
import com.example.views.viewmodel.ReplySortOrder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ✅ PERFORMANCE: Cached date formatter
private val dateFormatter by lazy { SimpleDateFormat("MMM d", Locale.getDefault()) }

// ✅ PERFORMANCE: Consistent animation specs
private val standardAnimation = tween<IntSize>(durationMillis = 200, easing = FastOutSlowInEasing)
private val fastAnimation = tween<IntSize>(durationMillis = 150, easing = FastOutSlowInEasing)

// CommentState is imported from ThreadViewScreen.kt

/**
 * Modern, performant Thread View Screen following Material Design 3 principles
 *
 * Key Performance Improvements:
 * - Single animation spec for consistency
 * - Simplified state management
 * - Reduced recompositions
 * - Clean visual hierarchy
 * - Smooth animations without conflicts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernThreadViewScreen(
    note: Note,
    comments: List<CommentThread>,
    listState: LazyListState = rememberLazyListState(),
    commentStates: MutableMap<String, CommentState> = remember { mutableStateMapOf() },
    expandedControlsCommentId: String? = null,
    onExpandedControlsChange: (String?) -> Unit = {},
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
    replyKind: Int = 1111, // 1 = Kind 1 replies (home feed), 1111 = Kind 1111 replies (topics)
    threadRepliesViewModel: ThreadRepliesViewModel = viewModel(),
    kind1RepliesViewModel: Kind1RepliesViewModel = viewModel(),
    relayUrls: List<String> = emptyList(),
    onBackClick: () -> Unit = {},
    onLike: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onCommentLike: (String) -> Unit = {},
    onCommentReply: (String) -> Unit = {},
    onLoginClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Select appropriate ViewModel based on reply kind
    val repliesState = when (replyKind) {
        1 -> {
            // Kind 1 replies for home feed
            val kind1State by kind1RepliesViewModel.uiState.collectAsState()
            // Convert to compatible state format
            ThreadRepliesUiState(
                note = kind1State.note,
                replies = kind1State.replies.map { it.toThreadReply(replyToId = note.id) },
                threadedReplies = emptyList(), // Kind 1 uses flat structure
                isLoading = kind1State.isLoading,
                error = kind1State.error,
                totalReplyCount = kind1State.totalReplyCount
            )
        }
        else -> {
            // Kind 1111 replies for topics
            threadRepliesViewModel.uiState.collectAsState().value
        }
    }

    // Load replies when screen opens
    LaunchedEffect(note.id, relayUrls, replyKind) {
        if (relayUrls.isNotEmpty()) {
            when (replyKind) {
                1 -> kind1RepliesViewModel.loadRepliesForNote(note, relayUrls)
                else -> threadRepliesViewModel.loadRepliesForNote(note, relayUrls)
            }
        }
    }

    // ✅ ZAP MENU AWARENESS: Global state for zap menu closure (like feed cards)
    var shouldCloseZapMenus by remember { mutableStateOf(false) }
    var expandedZapMenuCommentId by remember { mutableStateOf<String?>(null) }

    // ✅ ZAP CONFIGURATION: Dialog state for editing zap amounts
    var showZapConfigDialog by remember { mutableStateOf(false) }
    var showWalletConnectDialog by remember { mutableStateOf(false) }

    // ✅ ZAP MENU AWARENESS: Close zap menus when scrolling starts (like feed cards)
    var wasScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !wasScrolling) {
            // Scroll just started - close zap menus immediately
            shouldCloseZapMenus = true
            expandedZapMenuCommentId = null
            kotlinx.coroutines.delay(100)
            shouldCloseZapMenus = false
        }
        wasScrolling = listState.isScrollInProgress
    }

    // Use predictive back for smooth gesture navigation
    androidx.activity.compose.BackHandler {
        onBackClick()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "thread",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Search button for searching within thread replies
                    IconButton(onClick = { /* TODO: Search comments */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search thread",
                            tint = Color.White
                        )
                    }

                    // Filter button
                    IconButton(onClick = { /* TODO: Filter comments */ }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }

                    // Menu button
                    IconButton(onClick = { /* TODO: Show menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }

                    // Profile/Login button
                    if (onLoginClick != null) {
                        IconButton(onClick = onLoginClick) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            // Main note card - no pull to refresh, uses predictive back
            item(key = "main_note") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NoteCard(
                        note = note,
                        onLike = onLike,
                        onShare = onShare,
                        onComment = onComment,
                        onProfileClick = onProfileClick,
                        onNoteClick = { /* Already on thread */ },
                        // ✅ MAIN NOTE ZAP AWARENESS: Pass zap menu state to main note
                        shouldCloseZapMenus = shouldCloseZapMenus,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Comment control board - compact, above divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter/Sort control for comments
                        IconButton(
                            onClick = { /* TODO: Implement comment filtering/sorting */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter/Sort Comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Modern divider
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Threaded replies section with loading state
            item(key = "replies_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Reply count header
                    if (repliesState.totalReplyCount > 0 || repliesState.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (repliesState.isLoading && repliesState.replies.isEmpty()) {
                                    "Loading replies..."
                                } else {
                                    "${repliesState.totalReplyCount} ${if (repliesState.totalReplyCount == 1) "reply" else "replies"}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Sort button
                            if (repliesState.totalReplyCount > 0) {
                                var showSortMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = "Sort",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
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
                                }
                            }
                        }
                    }

                    // Loading indicator
                    if (repliesState.isLoading && repliesState.replies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LoadingAnimation(indicatorSize = 32.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading replies from relays...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Empty state
                    else if (repliesState.replies.isEmpty() && !repliesState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No replies yet. Be the first to reply!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Display replies - check if we have threaded structure or flat list
            if (replyKind == 1 && repliesState.threadedReplies.isEmpty() && repliesState.replies.isNotEmpty()) {
                // Kind 1 replies - display as flat list using NoteCard
                items(
                    items = repliesState.replies,
                    key = { it.id }
                ) { reply ->
                    NoteCard(
                        note = reply.toNote(),
                        onLike = { replyId ->
                            if (replyKind == 1) {
                                kind1RepliesViewModel.likeReply(replyId)
                            }
                        },
                        onShare = onShare,
                        onComment = { replyId -> onCommentReply(replyId) },
                        onProfileClick = onProfileClick,
                        onNoteClick = { /* Already in thread */ },
                        shouldCloseZapMenus = shouldCloseZapMenus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp) // Slight indent for replies
                    )
                }
            } else {
                // Kind 1111 replies - display as threaded structure
                items(
                    items = repliesState.threadedReplies,
                    key = { it.reply.id }
                ) { threadedReply ->
                    ThreadedReplyCard(
                        threadedReply = threadedReply,
                        onLike = { replyId -> threadRepliesViewModel.likeReply(replyId) },
                        onReply = onCommentReply,
                        onProfileClick = onProfileClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ✅ ZAP CONFIGURATION: Dialogs for editing zap amounts
    if (showZapConfigDialog) {
        com.example.views.ui.components.ZapConfigurationDialog(
            onDismiss = { showZapConfigDialog = false },
            onOpenWalletSettings = {
                showZapConfigDialog = false
                showWalletConnectDialog = true
            }
        )
    }

    if (showWalletConnectDialog) {
        com.example.views.ui.components.WalletConnectDialog(
            onDismiss = { showWalletConnectDialog = false }
        )
    }
}

@Composable
private fun ModernCommentThreadItem(
    commentThread: CommentThread,
    onLike: (String) -> Unit,
    onReply: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onZap: (String, Long) -> Unit = { _, _ -> },
    onCustomZap: (String) -> Unit = {},
    onTestZap: (String) -> Unit = {},
    onZapSettings: () -> Unit = {},
    depth: Int,
    commentStates: MutableMap<String, CommentState>,
    expandedControlsCommentId: String?,
    onExpandControls: (String) -> Unit,
    // ✅ ZAP MENU AWARENESS: Add zap menu state parameters
    shouldCloseZapMenus: Boolean = false,
    expandedZapMenuCommentId: String? = null,
    onExpandZapMenu: (String) -> Unit = {},
    isLastComment: Boolean = false,
    modifier: Modifier = Modifier
) {
    val commentId = commentThread.comment.id
    val state = commentStates.getOrPut(commentId) { CommentState() }
    val isControlsExpanded = expandedControlsCommentId == commentId

    // ✅ ULTRA COMPACT INDENTATION: Very tight spacing for child comments
    val indentPadding = (depth * 1.5).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Critical for proper vertical lines
            .padding(start = indentPadding)
    ) {
        // Vertical thread line - like original but cleaner
        if (depth > 0) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight() // Full height for proper thread navigation
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Comment content - no individual animation to prevent staggering
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            ModernCommentCard(
                comment = commentThread.comment,
                onLike = onLike,
                onReply = onReply,
                onProfileClick = onProfileClick,
                onZap = onZap,
                onCustomZap = onCustomZap,
                onTestZap = onTestZap,
                onZapSettings = onZapSettings,
                isControlsExpanded = isControlsExpanded,
                onToggleControls = { onExpandControls(commentId) },
                isCollapsed = state.isCollapsed,
                onCollapsedChange = { collapsed ->
                    commentStates[commentId] = state.copy(
                        isCollapsed = collapsed,
                        isExpanded = !collapsed
                    )
                },
                // ✅ ZAP MENU AWARENESS: Pass zap menu state to ModernCommentCard
                shouldCloseZapMenus = shouldCloseZapMenus,
                expandedZapMenuCommentId = expandedZapMenuCommentId,
                onExpandZapMenu = { onExpandZapMenu(commentId) },
                modifier = Modifier.fillMaxWidth()
            )

            // Replies - all animated together
            if (state.isExpanded && !state.isCollapsed && commentThread.replies.isNotEmpty()) {
                commentThread.replies.forEachIndexed { index, reply ->
                    ModernCommentThreadItem(
                        commentThread = reply,
                        onLike = onLike,
                        onReply = onReply,
                        onProfileClick = onProfileClick,
                        onZap = onZap,
                        onCustomZap = onCustomZap,
                        onTestZap = onTestZap,
                        onZapSettings = onZapSettings,
                        depth = depth + 1,
                        commentStates = commentStates,
                        expandedControlsCommentId = expandedControlsCommentId,
                        onExpandControls = onExpandControls,
                        // ✅ ZAP MENU AWARENESS: Pass zap menu state to nested replies
                        shouldCloseZapMenus = shouldCloseZapMenus,
                        expandedZapMenuCommentId = expandedZapMenuCommentId,
                        onExpandZapMenu = onExpandZapMenu,
                        isLastComment = index == commentThread.replies.size - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Minimal separator for top-level comments (but not the last one)
            if (depth == 0 && !isLastComment) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernCommentCard(
    comment: Comment,
    onLike: (String) -> Unit,
    onReply: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onZap: (String, Long) -> Unit = { _, _ -> },
    onCustomZap: (String) -> Unit = {},
    onTestZap: (String) -> Unit = {},
    onZapSettings: () -> Unit = {},
    isControlsExpanded: Boolean,
    onToggleControls: () -> Unit,
    isCollapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    // ✅ ZAP MENU AWARENESS: Add zap menu state parameters
    shouldCloseZapMenus: Boolean = false,
    expandedZapMenuCommentId: String? = null,
    onExpandZapMenu: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val commentId = comment.id
    val isZapMenuExpanded = expandedZapMenuCommentId == commentId

    // ✅ ZAP MENU AWARENESS: Close zap menu when shouldCloseZapMenus is true (like feed cards)
    LaunchedEffect(shouldCloseZapMenus) {
        if (shouldCloseZapMenus && isZapMenuExpanded) {
            onExpandZapMenu(commentId) // This will close the menu
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isCollapsed) {
                        onCollapsedChange(false)
                    } else {
                        onToggleControls()
                    }
                },
                onLongClick = {
                    if (!isCollapsed) {
                        onCollapsedChange(true)
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCollapsed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RectangleShape, // Sharp, edge-to-edge
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (!isCollapsed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Modern author info with better spacing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfilePicture(
                        author = comment.author,
                        size = 36.dp,
                        onClick = { onProfileClick(comment.author.id) }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = comment.author.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatTimestamp(comment.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content with better typography
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )

                // Optimized controls - only show/hide, no complex animations
                if (isControlsExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // ✅ COMPACT CONTROLS: Right-aligned with consistent spacing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactModernButton(
                                icon = Icons.Outlined.ArrowUpward,
                                contentDescription = "Upvote",
                                isActive = comment.isLiked,
                                onClick = { onLike(comment.id) }
                            )

                            CompactModernButton(
                                icon = Icons.Outlined.ArrowDownward,
                                contentDescription = "Downvote",
                                isActive = false,
                                onClick = { /* Handle downvote */ }
                            )

                            CompactModernButton(
                                icon = Icons.Outlined.Bookmark,
                                contentDescription = "Bookmark",
                                isActive = false,
                                onClick = { /* Handle bookmark */ }
                            )

                            CompactModernButton(
                                icon = Icons.Outlined.Reply,
                                contentDescription = "Reply",
                                isActive = false,
                                onClick = { onReply(comment.id) }
                            )

                            // Zap button with menu - using shared state (like feed cards)
                            CompactModernButton(
                                icon = Icons.Filled.Bolt,
                                contentDescription = "Zap",
                                isActive = false,
                                onClick = { onExpandZapMenu(commentId) }
                            )

                            // More options
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                CompactModernButton(
                                    icon = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    isActive = false,
                                    onClick = { showMenu = true }
                                )

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = { showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Report") },
                                        onClick = { showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }

                    // Zap menu - flowing layout with custom amounts, test, and edit buttons
                    AnimatedVisibility(
                        visible = isZapMenuExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Flowing zap amounts using FlowRow
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Get custom zap amounts from ZapAmountManager
                                val context = LocalContext.current
                                LaunchedEffect(Unit) {
                                    com.example.views.utils.ZapAmountManager.initialize(context)
                                }
                                val zapAmounts by com.example.views.utils.ZapAmountManager.zapAmounts.collectAsState()

                                // Zap amount chips - sorted largest to smallest
                                zapAmounts.sortedDescending().forEach { amount ->
                                    FilterChip(
                                        selected = amount == 1L, // Highlight 1 sat
                                        onClick = {
                                            onExpandZapMenu(commentId) // Close menu using shared state
                                            onZap(comment.id, amount)
                                        },
                                        label = {
                                            Text(
                                                text = com.example.views.utils.ZapUtils.formatZapAmount(amount),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Bolt,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFA500),
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White,
                                            containerColor = Color(0xFFFFA500),
                                            labelColor = Color.White,
                                            iconColor = Color.White
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = Color(0xFFFFA500)
                                        )
                                    )
                                }

                                // Test zap chip
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        onExpandZapMenu(commentId) // Close menu using shared state
                                        onTestZap(comment.id)
                                    },
                                    label = { Text("TEST") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.BugReport,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                // Edit zap amounts chip
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        onExpandZapMenu(commentId) // Close menu using shared state
                                        onZapSettings()
                                    },
                                    label = { Text("Edit") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Zap Amounts",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Compact collapsed state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Expand thread",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = comment.author.displayName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onProfileClick(comment.author.id) }
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "· tap to expand",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ CONSISTENT: Match main card ActionButton pattern
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp) // ✅ CONSISTENT: Match main card icon size
        )
    }
}

@Composable
private fun CompactModernButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ COMPACT CONTROLS: Smaller, right-aligned with consistent spacing
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp) // Slightly bigger button
            .padding(horizontal = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp) // Slightly bigger icon
        )
    }


}

/**
 * Display a single threaded reply with proper indentation
 */
@Composable
private fun ThreadedReplyCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f - (level * 0.05f)
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
                        author = reply.author,
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
                            text = formatReplyTimestamp(reply.timestamp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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

private fun formatReplyTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

// Data classes are imported from ThreadViewScreen.kt

// formatTimestamp is imported from ThreadViewScreen.kt

@Preview(showBackground = true)
@Composable
fun ModernThreadViewScreenPreview() {
    MaterialTheme {
        ModernThreadViewScreen(
            note = SampleData.sampleNotes.first(),
            comments = createSampleCommentThreads()
        )
    }
}
