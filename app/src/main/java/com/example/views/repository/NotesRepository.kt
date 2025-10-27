package com.example.views.repository

import android.util.Log
import com.example.views.data.Note
import com.example.views.data.Author
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
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Repository for fetching and managing Nostr notes using Quartz NostrClient.
 * Handles kind-1 (text note) events from specified relays.
 */
class NotesRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder().build()
    private val socketBuilder = BasicOkHttpWebSocket.Builder { _ -> okHttpClient }
    private val nostrClient = NostrClient(socketBuilder, scope)

    // Visible notes (shown in the feed)
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    // Cached notes (buffered until user pulls to refresh)
    private val cachedNotes = mutableListOf<Note>()
    private val _cachedNotesCount = MutableStateFlow(0)
    val cachedNotesCount: StateFlow<Int> = _cachedNotesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var connectedRelays = listOf<String>()
    private var currentSubscription: NostrClientSubscription? = null

    // Track if this is the first batch to auto-flush
    private var isFirstBatch = true
    private var firstBatchFlushed = false

    companion object {
        private const val TAG = "NotesRepository"
        private const val FIRST_BATCH_SIZE = 10 // Auto-flush first 10 notes
        private const val FIRST_BATCH_DELAY = 2000L // Wait 2 seconds before auto-flush
    }

    /**
     * Connect to relay URLs and connect the client
     */
    fun connectToRelays(relayUrls: List<String>) {
        Log.d(TAG, "Connecting to ${relayUrls.size} relays: $relayUrls")
        connectedRelays = relayUrls

        try {
            nostrClient.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting client: ${e.message}", e)
            _error.value = "Failed to connect: ${e.message}"
        }
    }

    /**
     * Disconnect from all relays
     */
    fun disconnectAll() {
        Log.d(TAG, "Disconnecting from all relays")
        try {
            currentSubscription?.destroy()
            currentSubscription = null
            nostrClient.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
        connectedRelays = emptyList()
        _notes.value = emptyList()

        // Reset first batch tracking
        isFirstBatch = true
        firstBatchFlushed = false
    }

    /**
     * Subscribe to kind-1 notes from connected relays
     */
    suspend fun subscribeToNotes(limit: Int = 100) {
        if (connectedRelays.isEmpty()) {
            Log.w(TAG, "No relays connected")
            return
        }

        _isLoading.value = true
        _error.value = null
        _notes.value = emptyList()

        // Reset first batch tracking for new subscription
        isFirstBatch = true
        firstBatchFlushed = false

        try {
            // Close previous subscription if exists
            currentSubscription?.destroy()

            Log.d(TAG, "Subscribing to kind-1 notes from ${connectedRelays.size} relays (limit: $limit)")

            // Create filter for kind-1 text notes
            val filter = Filter(
                kinds = listOf(1),
                limit = limit
            )

            // Create relay map (all relays get same filter)
            val relayFilters = connectedRelays.associate { url ->
                NormalizedRelayUrl(url) to listOf(filter)
            }

            // Subscribe with event handler
            currentSubscription = NostrClientSubscription(
                client = nostrClient,
                filter = { relayFilters },
                onEvent = { event ->
                    handleEvent(event)
                }
            )

            // Launch auto-flush for first batch
            scope.launch {
                delay(FIRST_BATCH_DELAY)
                autoFlushFirstBatch()
            }

            // Give it a moment to connect and start receiving
            delay(2000)
            _isLoading.value = false

            Log.d(TAG, "Subscription active for ${connectedRelays.size} relays")

        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to notes: ${e.message}", e)
            _error.value = "Failed to load notes: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Subscribe to notes from a specific relay only
     */
    suspend fun subscribeToRelayNotes(relayUrl: String, limit: Int = 100) {
        _isLoading.value = true
        _error.value = null
        _notes.value = emptyList()

        // Reset first batch tracking for new subscription
        isFirstBatch = true
        firstBatchFlushed = false

        try {
            // Close previous subscription
            currentSubscription?.destroy()

            Log.d(TAG, "Subscribing to notes from single relay: $relayUrl")

            // Create filter for kind-1 text notes
            val filter = Filter(
                kinds = listOf(1),
                limit = limit
            )

            // Single relay filter
            val relayFilters = mapOf(
                NormalizedRelayUrl(relayUrl) to listOf(filter)
            )

            // Subscribe with event handler
            currentSubscription = NostrClientSubscription(
                client = nostrClient,
                filter = { relayFilters },
                onEvent = { event ->
                    handleEvent(event)
                }
            )

            // Launch auto-flush for first batch
            scope.launch {
                delay(FIRST_BATCH_DELAY)
                autoFlushFirstBatch()
            }

            // Give it a moment to connect and start receiving
            delay(2000)
            _isLoading.value = false

            Log.d(TAG, "Subscription active for relay: $relayUrl")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading notes from relay: ${e.message}", e)
            _error.value = "Failed to load notes from relay: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Subscribe to notes from a specific author (for announcements)
     */
    suspend fun subscribeToAuthorNotes(relayUrls: List<String>, authorPubkey: String, limit: Int = 50) {
        if (relayUrls.isEmpty()) {
            Log.w(TAG, "No relays provided for author subscription")
            return
        }

        _isLoading.value = true
        _error.value = null
        _notes.value = emptyList()

        // Reset first batch tracking for new subscription
        isFirstBatch = true
        firstBatchFlushed = false

        try {
            currentSubscription?.destroy()

            Log.d(TAG, "Subscribing to notes from author ${authorPubkey.take(8)}... on ${relayUrls.size} relays")

            // Create filter for kind-1 notes from specific author
            val filter = Filter(
                kinds = listOf(1),
                authors = listOf(authorPubkey),
                limit = limit
            )

            val relayFilters = relayUrls.associate { url ->
                NormalizedRelayUrl(url) to listOf(filter)
            }

            currentSubscription = NostrClientSubscription(
                client = nostrClient,
                filter = { relayFilters },
                onEvent = { event ->
                    handleEvent(event)
                }
            )

            // Launch auto-flush for first batch
            scope.launch {
                delay(FIRST_BATCH_DELAY)
                autoFlushFirstBatch()
            }

            delay(2000)
            _isLoading.value = false

            Log.d(TAG, "Author subscription active")

        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to author notes: ${e.message}", e)
            _error.value = "Failed to load author notes: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Handle incoming event from relay
     */
    private fun handleEvent(event: Event) {
        try {
            if (event.kind == 1) {
                val note = convertEventToNote(event)

                // Add to cached notes buffer if not duplicate
                synchronized(cachedNotes) {
                    val allNotes = _notes.value + cachedNotes
                    if (!allNotes.any { it.id == note.id }) {
                        cachedNotes.add(note)
                        _cachedNotesCount.value = cachedNotes.size
                        Log.d(TAG, "Cached note from ${note.author.username}: ${note.content.take(50)}... (Cached: ${cachedNotes.size})")

                        // Check if we should auto-flush first batch
                        if (isFirstBatch && !firstBatchFlushed && cachedNotes.size >= FIRST_BATCH_SIZE) {
                            scope.launch {
                                delay(500) // Brief delay to let a few more notes arrive
                                autoFlushFirstBatch()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling event: ${e.message}", e)
        }
    }

    /**
     * Auto-flush the first batch of notes to immediately populate the feed
     */
    private fun autoFlushFirstBatch() {
        synchronized(cachedNotes) {
            if (firstBatchFlushed || cachedNotes.isEmpty()) {
                return
            }

            firstBatchFlushed = true
            val currentNotes = _notes.value
            val newNotes = (currentNotes + cachedNotes).sortedByDescending { it.timestamp }
            _notes.value = newNotes

            Log.d(TAG, "Auto-flushed first batch: ${cachedNotes.size} notes to feed")

            cachedNotes.clear()
            _cachedNotesCount.value = 0
            isFirstBatch = false
        }
    }

    /**
     * Flush cached notes to visible notes (called on pull-to-refresh)
     */
    fun flushCachedNotes() {
        synchronized(cachedNotes) {
            if (cachedNotes.isEmpty()) {
                Log.d(TAG, "No cached notes to flush")
                return
            }

            val currentNotes = _notes.value
            val newNotes = (currentNotes + cachedNotes).sortedByDescending { it.timestamp }
            _notes.value = newNotes

            Log.d(TAG, "Flushed ${cachedNotes.size} cached notes to feed (Total: ${newNotes.size})")

            cachedNotes.clear()
            _cachedNotesCount.value = 0
        }
    }

    /**
     * Convert Nostr Event to our Note data model
     */
    private fun convertEventToNote(event: Event): Note {
        val pubkeyHex = event.pubKey

        // Use default author for now
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
            mediaUrls = emptyList()
        )
    }

    /**
     * Clear all notes (both visible and cached)
     */
    fun clearNotes() {
        _notes.value = emptyList()
        synchronized(cachedNotes) {
            cachedNotes.clear()
            _cachedNotesCount.value = 0
        }
    }

    /**
     * Refresh notes (resubscribe)
     */
    suspend fun refresh() {
        Log.d(TAG, "Refreshing notes from ${connectedRelays.size} relays")
        subscribeToNotes()
    }

    /**
     * Get currently connected relay URLs
     */
    fun getConnectedRelays(): List<String> = connectedRelays

    /**
     * Check if connected to any relays
     */
    fun isConnected(): Boolean = connectedRelays.isNotEmpty()
}
