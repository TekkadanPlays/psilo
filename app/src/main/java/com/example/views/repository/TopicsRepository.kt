package com.example.views.repository

import android.util.Log
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
import okhttp3.OkHttpClient

/**
 * Data class for Kind 11 Topic Note
 */
data class TopicNote(
    val id: String,
    val author: Author,
    val title: String,
    val content: String,
    val hashtags: List<String>,
    val timestamp: Long,
    val replyCount: Int = 0,
    val relayUrl: String = ""
)

/**
 * Data class for Hashtag Statistics
 */
data class HashtagStats(
    val hashtag: String,
    val topicCount: Int,
    val totalReplies: Int,
    val latestActivity: Long,
    val topicIds: List<String> = emptyList()
)

/**
 * Repository for fetching and managing Kind 11 topic events and hashtag statistics.
 * Handles topic discovery, hashtag extraction, and relay-aware aggregation.
 *
 * Kind 11 events are topics that:
 * - Have a "title" tag
 * - Reference hashtags via "t" tags
 * - Can receive Kind 1111 replies
 */
class TopicsRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient.Builder().build()
    private val socketBuilder = BasicOkHttpWebSocket.Builder { _ -> okHttpClient }
    private val nostrClient = NostrClient(socketBuilder, scope)

    // All topics indexed by ID
    private val _topics = MutableStateFlow<Map<String, TopicNote>>(emptyMap())
    val topics: StateFlow<Map<String, TopicNote>> = _topics.asStateFlow()

    // Hashtag statistics aggregated from topics
    private val _hashtagStats = MutableStateFlow<List<HashtagStats>>(emptyList())
    val hashtagStats: StateFlow<List<HashtagStats>> = _hashtagStats.asStateFlow()

    // Topics grouped by hashtag
    private val _topicsByHashtag = MutableStateFlow<Map<String, List<TopicNote>>>(emptyMap())
    val topicsByHashtag: StateFlow<Map<String, List<TopicNote>>> = _topicsByHashtag.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var connectedRelays = listOf<String>()
    private var currentSubscription: NostrClientSubscription? = null

    companion object {
        private const val TAG = "TopicsRepository"
        private const val TOPIC_FETCH_TIMEOUT = 10000L
    }

    /**
     * Connect to relay URLs
     */
    fun connectToRelays(relayUrls: List<String>) {
        Log.d(TAG, "Connecting to ${relayUrls.size} relays for topics")
        connectedRelays = relayUrls

        try {
            nostrClient.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting client: ${e.message}", e)
            _error.value = "Failed to connect: ${e.message}"
        }
    }

    /**
     * Disconnect from all relays and clean up
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
        clearAllTopics()
    }

    /**
     * Fetch Kind 11 topics from connected relays
     *
     * @param relayUrls Optional list of relays to query (uses connected relays if not provided)
     * @param limit Maximum number of topics to fetch
     */
    suspend fun fetchTopics(
        relayUrls: List<String>? = null,
        limit: Int = 100
    ) {
        val targetRelays = relayUrls ?: connectedRelays
        if (targetRelays.isEmpty()) {
            Log.w(TAG, "No relays available to fetch topics")
            return
        }

        _isLoading.value = true
        _error.value = null

        try {
            // Close previous subscription
            currentSubscription?.destroy()

            Log.d(TAG, "Fetching Kind 11 topics from ${targetRelays.size} relays")

            // Create filter for Kind 11 topic notes
            val filter = Filter(
                kinds = listOf(11),
                limit = limit
            )

            // Create relay map
            val relayFilters = targetRelays.associate { url ->
                NormalizedRelayUrl(url) to listOf(filter)
            }

            // Subscribe with event handler
            currentSubscription = NostrClientSubscription(
                client = nostrClient,
                filter = { relayFilters },
                onEvent = { event ->
                    handleTopicEvent(event)
                }
            )

            // Give it time to receive topics
            delay(TOPIC_FETCH_TIMEOUT)
            _isLoading.value = false

            // Compute hashtag statistics after fetching
            computeHashtagStatistics()

            Log.d(TAG, "Finished fetching topics (${_topics.value.size} topics, ${_hashtagStats.value.size} hashtags)")

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching topics: ${e.message}", e)
            _error.value = "Failed to load topics: ${e.message}"
            _isLoading.value = false
        }
    }

    /**
     * Handle incoming Kind 11 topic event from relay
     */
    private fun handleTopicEvent(event: Event) {
        try {
            if (event.kind == 11) {
                val topic = convertEventToTopicNote(event)

                // Add to topics map (deduplicate by ID)
                val currentTopics = _topics.value.toMutableMap()
                if (!currentTopics.containsKey(topic.id)) {
                    currentTopics[topic.id] = topic
                    _topics.value = currentTopics

                    Log.d(TAG, "Added topic: ${topic.title} with hashtags: ${topic.hashtags.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling topic event: ${e.message}", e)
        }
    }

    /**
     * Convert Nostr Event to TopicNote
     */
    private fun convertEventToTopicNote(event: Event): TopicNote {
        val pubkeyHex = event.pubKey

        // Create author
        val author = Author(
            id = pubkeyHex,
            username = pubkeyHex.take(8) + "...",
            displayName = pubkeyHex.take(8) + "...",
            avatarUrl = null,
            isVerified = false
        )

        // Extract title from tags
        val tags = event.tags.map { it.toList() }
        val title = extractTitle(tags)

        // Extract hashtags from "t" tags
        val hashtags = tags
            .filter { tag -> tag.size >= 2 && tag[0] == "t" }
            .mapNotNull { tag -> tag.getOrNull(1)?.lowercase() }
            .distinct()

        return TopicNote(
            id = event.id,
            author = author,
            title = title,
            content = event.content,
            hashtags = hashtags,
            timestamp = event.createdAt * 1000L, // Convert to milliseconds
            replyCount = 0, // Will be updated when we fetch Kind 1111 replies
            relayUrl = "" // Could be set if we track which relay it came from
        )
    }

    /**
     * Extract title from event tags
     * Looks for "title" tag, falls back to first line of content
     */
    private fun extractTitle(tags: List<List<String>>): String {
        // Look for "title" tag
        val titleTag = tags.find { it.size >= 2 && it[0] == "title" }
        if (titleTag != null) {
            return titleTag[1]
        }

        // No title tag found - will use content preview instead
        return ""
    }

    /**
     * Compute hashtag statistics from collected topics
     */
    private fun computeHashtagStatistics() {
        val topics = _topics.value.values
        val hashtagMap = mutableMapOf<String, MutableList<TopicNote>>()

        // Group topics by hashtag
        topics.forEach { topic ->
            topic.hashtags.forEach { hashtag ->
                hashtagMap.getOrPut(hashtag) { mutableListOf() }.add(topic)
            }
        }

        // Build statistics
        val stats = hashtagMap.map { (hashtag, topicList) ->
            HashtagStats(
                hashtag = hashtag,
                topicCount = topicList.size,
                totalReplies = topicList.sumOf { it.replyCount },
                latestActivity = topicList.maxOfOrNull { it.timestamp } ?: 0L,
                topicIds = topicList.map { it.id }
            )
        }.sortedByDescending { it.topicCount } // Sort by most topics

        _hashtagStats.value = stats

        // Update topics by hashtag map
        _topicsByHashtag.value = hashtagMap.mapValues { it.value.sortedByDescending { topic -> topic.timestamp } }

        Log.d(TAG, "Computed stats for ${stats.size} hashtags")
    }

    /**
     * Get topics for a specific hashtag
     */
    fun getTopicsForHashtag(hashtag: String): List<TopicNote> {
        return _topicsByHashtag.value[hashtag.lowercase()] ?: emptyList()
    }

    /**
     * Get hashtag statistics
     */
    fun getHashtagStats(): List<HashtagStats> {
        return _hashtagStats.value
    }

    /**
     * Get all topics as a list sorted by timestamp
     */
    fun getAllTopics(): List<TopicNote> {
        return _topics.value.values.sortedByDescending { it.timestamp }
    }

    /**
     * Clear all topics and statistics
     */
    fun clearAllTopics() {
        _topics.value = emptyMap()
        _hashtagStats.value = emptyList()
        _topicsByHashtag.value = emptyMap()
    }

    /**
     * Refresh topics from relays
     */
    suspend fun refresh(relayUrls: List<String>? = null) {
        clearAllTopics()
        fetchTopics(relayUrls)
    }

    /**
     * Check if currently loading topics
     */
    fun isLoadingTopics(): Boolean = _isLoading.value
}
