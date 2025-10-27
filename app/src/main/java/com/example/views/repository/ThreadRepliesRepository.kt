package com.example.views.repository

import android.util.Log
import com.example.views.data.Author
import com.example.views.data.ThreadReply
import com.vitorpamplona.quartz.nip01Core.relay.client.NostrClient
import com.vitorpamplona.quartz.nip01Core.relay.client.NostrClientSubscription
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.sockets.okhttp.BasicOkHttpWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

/**
 * Repository for fetching and managing kind 1111 thread replies using Quartz NostrClient.
 * Handles threaded conversations following NIP-22 (Threaded Replies).
 *
 * Kind 1111 events are replies that:
 * - Reference the root note via "e" tags with "root" marker
 * - Reference the parent reply via "e" tags with "reply" marker
 * - Can be nested to create threaded conversations
 */
class ThreadRepliesRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder().build()
    private val socketBuilder = BasicOkHttpWebSocket.Builder { _ -> okHttpClient }
    private val nostrClient = NostrClient(socketBuilder, scope)

    // Replies for a specific note ID
    private val _replies = MutableStateFlow<Map<String, List<ThreadReply>>>(emptyMap())
    val replies: StateFlow<Map<String, List<ThreadReply>>> = _replies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var connectedRelays = listOf<String>()
    private val activeSubscriptions = mutableMapOf<String, NostrClientSubscription>()

    companion object {
        private const val TAG = "ThreadRepliesRepository"
        private const val REPLY_FETCH_TIMEOUT = 10000L
    }

    /**
     * Connect to relay URLs
     */
    fun connectToRelays(relayUrls: List<String>) {
        Log.d(TAG, "Connecting to ${relayUrls.size} relays for thread replies")
        connectedRelays = relayUrls

        try {
            nostrClient.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting client: ${e.message}", e)
            _error.value = "Failed to connect: ${e.message}"
        }
    }

    /**
     * Disconnect from all relays and clean up subscriptions
     */
    fun disconnectAll() {
        Log.d(TAG, "Disconnecting from all relays")
        try {
            activeSubscriptions.values.forEach { it.destroy() }
            activeSubscriptions.clear()
            nostrClient.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
        connectedRelays = emptyList()
        _replies.value = emptyMap()
    }

    /**
     * Fetch kind 1111 replies for a specific note
     *
     * @param noteId The ID of the note to fetch replies for
     * @param relayUrls Optional list of relays to query (uses connected relays if not provided)
     * @param limit Maximum number of replies to fetch
     */
    suspend fun fetchRepliesForNote(
        noteId: String,
        relayUrls: List<String>? = null,
        limit: Int = 100
    ) {
        val targetRelays = relayUrls ?: connectedRelays
        if (targetRelays.isEmpty()) {
            Log.w(TAG, "No relays available to fetch replies")
            return
        }

        _isLoading.value = true
        _error.value = null

        try {
            // Close previous subscription for this note if exists
            activeSubscriptions[noteId]?.destroy()

            Log.d(TAG, "Fetching kind 1111 replies for note ${noteId.take(8)}... from ${targetRelays.size} relays")

            // Create filter for kind 1111 replies
            // Using "e" tag to find all replies that reference this note
            val filter = Filter(
                kinds = listOf(1111),
                tags = mapOf("e" to listOf(noteId)), // Find all replies referencing this note
                limit = limit
            )

            // Create relay map
            val relayFilters = targetRelays.associate { url ->
                NormalizedRelayUrl(url) to listOf(filter)
            }

            // Subscribe with event handler
            val subscription = NostrClientSubscription(
                client = nostrClient,
                filter = { relayFilters },
                onEvent = { event ->
                    handleReplyEvent(noteId, event)
                }
            )

            activeSubscriptions[noteId] = subscription

            // Give it time to receive replies
            delay(REPLY_FETCH_TIMEOUT)
            _isLoading.value = false

            Log.d(TAG, "Finished fetching replies for note ${noteId.take(8)}... (${getRepliesForNote(noteId).size} replies)")

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching replies: ${e.message}", e)
            _error.value = "Failed to load replies: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Handle incoming reply event from relay
     */
    private fun handleReplyEvent(noteId: String, event: Event) {
        try {
            if (event.kind == 1111) {
                val reply = convertEventToThreadReply(event)

                // Add reply to the collection for this note
                val currentReplies = _replies.value[noteId]?.toMutableList() ?: mutableListOf()

                // Avoid duplicates
                if (!currentReplies.any { it.id == reply.id }) {
                    currentReplies.add(reply)

                    // Update the flow with new replies
                    _replies.value = _replies.value + (noteId to currentReplies.sortedBy { it.timestamp })

                    Log.d(TAG, "Added reply from ${reply.author.username}: ${reply.content.take(50)}... (Total: ${currentReplies.size})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling reply event: ${e.message}", e)
        }
    }

    /**
     * Convert Nostr Event to ThreadReply data model
     */
    private fun convertEventToThreadReply(event: Event): ThreadReply {
        val pubkeyHex = event.pubKey

        // Use default author for now (should be enhanced with profile lookup)
        val author = Author(
            id = pubkeyHex,
            username = pubkeyHex.take(8) + "...",
            displayName = pubkeyHex.take(8) + "...",
            avatarUrl = null,
            isVerified = false
        )

        // Extract thread relationship from tags
        val tags = event.tags.map { it.toList() }
        val (rootId, replyToId, threadLevel) = ThreadReply.parseThreadTags(tags)

        // Extract hashtags from tags
        val hashtags = tags
            .filter { tag -> tag.size >= 2 && tag[0] == "t" }
            .mapNotNull { tag -> tag.getOrNull(1) }

        // Extract image URLs from content
        val imageUrlPattern = Regex("""https?://[^\s]+\.(jpg|jpeg|png|gif|webp)""", RegexOption.IGNORE_CASE)
        val mediaUrls = imageUrlPattern.findAll(event.content).map { it.value }.toList()

        return ThreadReply(
            id = event.id,
            author = author,
            content = event.content,
            timestamp = event.createdAt * 1000L, // Convert to milliseconds
            likes = 0,
            shares = 0,
            replies = 0,
            isLiked = false,
            hashtags = hashtags,
            mediaUrls = mediaUrls,
            rootNoteId = rootId,
            replyToId = replyToId,
            threadLevel = threadLevel
        )
    }

    /**
     * Get replies for a specific note ID
     */
    fun getRepliesForNote(noteId: String): List<ThreadReply> {
        return _replies.value[noteId] ?: emptyList()
    }

    /**
     * Get reply count for a specific note
     */
    fun getReplyCount(noteId: String): Int {
        return _replies.value[noteId]?.size ?: 0
    }

    /**
     * Clear replies for a specific note
     */
    fun clearRepliesForNote(noteId: String) {
        activeSubscriptions[noteId]?.destroy()
        activeSubscriptions.remove(noteId)
        _replies.value = _replies.value - noteId
    }

    /**
     * Clear all replies
     */
    fun clearAllReplies() {
        activeSubscriptions.values.forEach { it.destroy() }
        activeSubscriptions.clear()
        _replies.value = emptyMap()
    }

    /**
     * Check if currently loading replies
     */
    fun isLoadingReplies(): Boolean = _isLoading.value
}
