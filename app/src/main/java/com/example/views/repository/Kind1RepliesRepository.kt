package com.example.views.repository

import android.util.Log
import com.example.views.data.Author
import com.example.views.data.Note
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
 * Repository for fetching and managing Kind 1 replies to Kind 1 notes using Quartz NostrClient.
 * Handles threaded conversations for regular notes following NIP-10 (Reply Tags).
 *
 * Kind 1 events are standard text notes. Replies are also Kind 1 events that:
 * - Reference the root note via "e" tags with "root" marker
 * - Reference the parent reply via "e" tags with "reply" marker
 * - Can be nested to create threaded conversations
 */
class Kind1RepliesRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder().build()
    private val socketBuilder = BasicOkHttpWebSocket.Builder { _ -> okHttpClient }
    private val nostrClient = NostrClient(socketBuilder, scope)

    // Replies for a specific note ID
    private val _replies = MutableStateFlow<Map<String, List<Note>>>(emptyMap())
    val replies: StateFlow<Map<String, List<Note>>> = _replies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var connectedRelays = listOf<String>()
    private val activeSubscriptions = mutableMapOf<String, NostrClientSubscription>()

    companion object {
        private const val TAG = "Kind1RepliesRepository"
        private const val REPLY_FETCH_TIMEOUT = 10000L
    }

    /**
     * Connect to relay URLs
     */
    fun connectToRelays(relayUrls: List<String>) {
        Log.d(TAG, "Connecting to ${relayUrls.size} relays for Kind 1 replies")
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
     * Fetch Kind 1 replies for a specific note
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

            Log.d(TAG, "Fetching Kind 1 replies for note ${noteId.take(8)}... from ${targetRelays.size} relays")

            // Create filter for Kind 1 replies
            // Using "e" tag to find all replies that reference this note
            val filter = Filter(
                kinds = listOf(1),
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
            if (event.kind == 1) {
                // Check if this event is actually a reply to our note
                val referencedNoteIds = extractReferencedNoteIds(event)
                if (noteId in referencedNoteIds) {
                    val reply = convertEventToNote(event)

                    // Add reply to the collection for this note
                    val currentReplies = _replies.value[noteId]?.toMutableList() ?: mutableListOf()

                    // Avoid duplicates
                    if (!currentReplies.any { it.id == reply.id }) {
                        currentReplies.add(reply)

                        // Update the flow with new replies sorted by timestamp
                        _replies.value = _replies.value + (noteId to currentReplies.sortedBy { it.timestamp })

                        Log.d(TAG, "Added reply from ${reply.author.username}: ${reply.content.take(50)}... (Total: ${currentReplies.size})")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling reply event: ${e.message}", e)
        }
    }

    /**
     * Extract all note IDs referenced in "e" tags
     */
    private fun extractReferencedNoteIds(event: Event): List<String> {
        val referencedIds = mutableListOf<String>()
        event.tags.forEach { tag ->
            if (tag.size >= 2 && tag[0] == "e") {
                referencedIds.add(tag[1])
            }
        }
        return referencedIds
    }

    /**
     * Parse thread relationship from "e" tags
     * Returns Triple of (rootId, replyToId, isDirectReply)
     */
    private fun parseThreadRelationship(event: Event): Triple<String?, String?, Boolean> {
        var rootId: String? = null
        var replyToId: String? = null

        val eTags = event.tags.filter { it.size >= 2 && it[0] == "e" }

        if (eTags.isEmpty()) {
            return Triple(null, null, false)
        }

        // Look for marked tags first (NIP-10 preferred format)
        eTags.forEach { tag ->
            val eventId = tag[1]
            val marker = tag.getOrNull(3)

            when (marker) {
                "root" -> rootId = eventId
                "reply" -> replyToId = eventId
            }
        }

        // Fallback to positional format if no markers found
        if (rootId == null && replyToId == null) {
            when (eTags.size) {
                1 -> {
                    // Single "e" tag is a direct reply to that note
                    rootId = eTags[0][1]
                    replyToId = eTags[0][1]
                }
                else -> {
                    // First "e" is root, last "e" is reply-to
                    rootId = eTags.first()[1]
                    replyToId = eTags.last()[1]
                }
            }
        }

        // Determine if this is a direct reply to root
        val isDirectReply = rootId == replyToId || replyToId == null

        return Triple(rootId, replyToId, isDirectReply)
    }

    /**
     * Convert Nostr Event to Note data model
     */
    private fun convertEventToNote(event: Event): Note {
        val pubkeyHex = event.pubKey

        // Use default author for now (should be enhanced with profile lookup)
        val author = Author(
            id = pubkeyHex,
            username = pubkeyHex.take(8) + "...",
            displayName = pubkeyHex.take(8) + "...",
            avatarUrl = null,
            isVerified = false
        )

        // Extract hashtags from tags
        val hashtags = event.tags.toList()
            .filter { tag -> tag.size >= 2 && tag[0] == "t" }
            .mapNotNull { tag -> tag.getOrNull(1) }

        // Extract image URLs from content
        val imageUrlPattern = Regex("""https?://[^\s]+\.(jpg|jpeg|png|gif|webp)""", RegexOption.IGNORE_CASE)
        val mediaUrls = imageUrlPattern.findAll(event.content).map { it.value }.toList()

        return Note(
            id = event.id,
            author = author,
            content = event.content,
            timestamp = event.createdAt * 1000L, // Convert to milliseconds
            likes = 0,
            shares = 0,
            comments = 0,
            isLiked = false,
            hashtags = hashtags,
            mediaUrls = mediaUrls
        )
    }

    /**
     * Get replies for a specific note ID
     */
    fun getRepliesForNote(noteId: String): List<Note> {
        return _replies.value[noteId] ?: emptyList()
    }

    /**
     * Get reply count for a specific note
     */
    fun getReplyCount(noteId: String): Int {
        return _replies.value[noteId]?.size ?: 0
    }

    /**
     * Build threaded structure from flat list of replies
     * Returns a map of parentId to list of child notes
     */
    fun buildThreadStructure(noteId: String): Map<String, List<Note>> {
        val replies = getRepliesForNote(noteId)
        val threadMap = mutableMapOf<String, MutableList<Note>>()

        replies.forEach { reply ->
            // Parse the event to get thread relationship
            // For now, we'll use a simplified approach
            // In a full implementation, we'd parse the original event
            val parentId = noteId // Simplified: assume all replies are direct

            if (!threadMap.containsKey(parentId)) {
                threadMap[parentId] = mutableListOf()
            }
            threadMap[parentId]?.add(reply)
        }

        return threadMap
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
