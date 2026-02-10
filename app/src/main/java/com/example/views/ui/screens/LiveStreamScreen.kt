package com.example.views.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.views.data.LiveActivity
import com.example.views.data.LiveActivityStatus
import com.example.views.repository.LiveActivityRepository
import com.example.views.ui.components.LiveStatusDot
import com.example.views.ui.components.PipStreamManager

/**
 * NIP-53 Live Stream viewer screen.
 * Shows the video player (HLS), stream info (title, host, participants),
 * and a placeholder for live chat (kind:1311) in the future.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
    activityAddressableId: String,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val decodedId = remember(activityAddressableId) { Uri.decode(activityAddressableId) }
    val repository = remember { LiveActivityRepository.getInstance() }
    val allActivities by repository.allActivities.collectAsState()
    val activity = allActivities.firstOrNull { "${it.hostPubkey}:${it.dTag}" == decodedId }

    if (activity == null) {
        // Activity not found or expired — show placeholder and allow back
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Live Stream") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Stream not available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    // --- Hoisted ExoPlayer: created at screen level so we can hand it off to PiP ---
    val context = LocalContext.current
    val streamUrl = activity.streamingUrl ?: activity.recordingUrl
    val isHls = remember(streamUrl) { streamUrl?.contains(".m3u8", ignoreCase = true) == true }

    var playerError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasReceivedVideo by remember { mutableStateOf(false) }
    var showUnavailable by remember { mutableStateOf(streamUrl == null) }

    // Track whether we handed the player to PiP (so DisposableEffect doesn't release it)
    var handedOffToPip by remember { mutableStateOf(false) }

    // Try to reclaim a PiP player for this stream, or create a new one
    val player: ExoPlayer? = remember(streamUrl) {
        if (streamUrl == null) return@remember null
        val reclaimed = if (PipStreamManager.isActiveFor(decodedId)) {
            PipStreamManager.reclaimPlayer()?.player
        } else null
        reclaimed ?: ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            if (isHls) {
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                setMediaSource(hlsSource)
            } else {
                setMediaItem(mediaItem)
            }
            prepare()
            playWhenReady = true
        }
    }

    // Player state listener + lifecycle
    if (player != null) {
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isBuffering = false
                            hasReceivedVideo = true
                            showUnavailable = false
                            playerError = null
                        }
                        Player.STATE_BUFFERING -> {
                            isBuffering = true
                        }
                        Player.STATE_ENDED -> {
                            isBuffering = false
                        }
                        Player.STATE_IDLE -> {
                            isBuffering = false
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    playerError = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Unable to connect to stream"
                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                            "Unsupported stream format"
                        else -> "Broadcast unavailable"
                    }
                    showUnavailable = true
                }
            }
            player.addListener(listener)
            // If player is already ready (e.g. reclaimed from PiP), sync state immediately
            if (player.playbackState == Player.STATE_READY) {
                isBuffering = false
                hasReceivedVideo = true
                showUnavailable = false
                playerError = null
            }
            onDispose {
                player.removeListener(listener)
                if (!handedOffToPip) {
                    player.release()
                }
            }
        }

        // If still buffering after 15s with no video, show fallback
        LaunchedEffect(isBuffering, hasReceivedVideo) {
            if (isBuffering && !hasReceivedVideo) {
                delay(15_000)
                if (isBuffering && !hasReceivedVideo) {
                    showUnavailable = true
                }
            }
        }
    }

    // Back handler: hand off player to PiP if stream is playing, then navigate back
    val handleBack = {
        if (player != null && hasReceivedVideo && !showUnavailable) {
            handedOffToPip = true
            PipStreamManager.startPip(
                player = player,
                addressableId = decodedId,
                title = activity.title,
                hostName = activity.hostAuthor?.username ?: activity.hostPubkey.take(8)
            )
        }
        onBackClick()
    }

    BackHandler { handleBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveStatusDot(status = activity.status)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = activity.title ?: "Live Stream",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Video player
            item(key = "player") {
                LiveStreamPlayerView(
                    player = player,
                    showUnavailable = showUnavailable,
                    playerError = playerError,
                    activity = activity,
                    onPipClick = { handleBack() }
                )
            }

            // Stream info
            item(key = "info") {
                LiveStreamInfo(
                    activity = activity,
                    onProfileClick = onProfileClick
                )
            }

            // Chat placeholder
            item(key = "chat_header") {
                Text(
                    text = "Live Chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item(key = "chat_placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kind 1311 live chat coming soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Video player view — receives the player from the parent screen.
 * Shows the PlayerView when stream is available, or the broadcast unavailable fallback.
 * Includes a PiP button overlay in the top-right corner.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LiveStreamPlayerView(
    player: ExoPlayer?,
    showUnavailable: Boolean,
    playerError: String?,
    activity: LiveActivity,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (player != null && !showUnavailable) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        this.player = player
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    }
                }
            )

            // PiP button overlay
            IconButton(
                onClick = onPipClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Picture in Picture",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else if (showUnavailable) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            BroadcastUnavailableContent(
                message = playerError ?: "No video input is being received from this stream"
            )
        }
    } else {
        BroadcastUnavailablePlaceholder(
            activity = activity,
            modifier = modifier
        )
    }
}

@Composable
private fun BroadcastUnavailablePlaceholder(
    activity: LiveActivity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (activity.imageUrl != null) {
            AsyncImage(
                model = activity.imageUrl,
                contentDescription = "Stream preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dim overlay on top of preview image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                BroadcastUnavailableContent(
                    message = if (activity.status == LiveActivityStatus.PLANNED) "Stream starting soon" else "Broadcast unavailable"
                )
            }
        } else {
            BroadcastUnavailableContent(
                message = if (activity.status == LiveActivityStatus.PLANNED) "Stream starting soon" else "Broadcast unavailable"
            )
        }
    }
}

@Composable
private fun BroadcastUnavailableContent(
    message: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.VideocamOff,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No video input is being received",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * Stream info section: host, title, summary, participant count, hashtags.
 */
@Composable
private fun LiveStreamInfo(
    activity: LiveActivity,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Host row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (activity.hostAuthor?.avatarUrl != null) {
                AsyncImage(
                    model = activity.hostAuthor.avatarUrl,
                    contentDescription = "Host avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Host",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.hostAuthor?.displayName
                        ?: activity.hostAuthor?.username
                        ?: activity.hostPubkey.take(12) + "...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveStatusDot(status = activity.status)
                    Text(
                        text = activity.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (activity.status) {
                            LiveActivityStatus.LIVE -> Color(0xFFEF4444)
                            LiveActivityStatus.PLANNED -> Color(0xFFF59E0B)
                            LiveActivityStatus.ENDED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    activity.currentParticipants?.let { count ->
                        if (count > 0) {
                            Text(
                                text = "$count watching",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Summary
        if (!activity.summary.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = activity.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hashtags
        if (activity.hashtags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = activity.hashtags.joinToString(" ") { "#$it" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }

        // Participants list
        if (activity.participants.size > 1) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${activity.participants.size} participants",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
