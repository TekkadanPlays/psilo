package com.example.views.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.views.repository.TopicsRepository
import com.example.views.repository.TopicNote
import com.example.views.repository.HashtagStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class TopicsUiState(
    val hashtagStats: List<HashtagStats> = emptyList(),
    val allTopics: List<TopicNote> = emptyList(),
    val selectedHashtag: String? = null,
    val topicsForSelectedHashtag: List<TopicNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectedRelays: List<String> = emptyList(),
    val sortOrder: HashtagSortOrder = HashtagSortOrder.MOST_TOPICS,
    val isViewingHashtagFeed: Boolean = false
)

enum class HashtagSortOrder {
    MOST_TOPICS,      // Sort by topic count
    MOST_ACTIVE,      // Sort by latest activity
    MOST_REPLIES,     // Sort by total reply count
    ALPHABETICAL      // Sort alphabetically
}

/**
 * ViewModel for managing topics and hashtag discovery
 * Handles Kind 11 topic fetching and hashtag statistics
 */
class TopicsViewModel : ViewModel() {
    private val repository = TopicsRepository()

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "TopicsViewModel"
    }

    init {
        observeRepositoryFlows()
    }

    /**
     * Observe repository state flows
     */
    private fun observeRepositoryFlows() {
        viewModelScope.launch {
            repository.hashtagStats.collect { stats ->
                val sortedStats = sortHashtagStats(stats, _uiState.value.sortOrder)
                _uiState.value = _uiState.value.copy(hashtagStats = sortedStats)
            }
        }

        viewModelScope.launch {
            repository.topics.collect { topicsMap ->
                val allTopics = topicsMap.values.sortedByDescending { it.timestamp }
                _uiState.value = _uiState.value.copy(allTopics = allTopics)

                // Update selected hashtag topics if one is selected
                val selectedHashtag = _uiState.value.selectedHashtag
                if (selectedHashtag != null) {
                    val topicsForHashtag = repository.getTopicsForHashtag(selectedHashtag)
                    _uiState.value = _uiState.value.copy(topicsForSelectedHashtag = topicsForHashtag)
                }
            }
        }

        viewModelScope.launch {
            repository.isLoading.collect { isLoading ->
                _uiState.value = _uiState.value.copy(isLoading = isLoading)
            }
        }

        viewModelScope.launch {
            repository.error.collect { error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(error = error)
                }
            }
        }
    }

    /**
     * Load topics from specified relays
     */
    fun loadTopicsFromRelays(relayUrls: List<String>) {
        Log.d(TAG, "Loading topics from ${relayUrls.size} relays")

        _uiState.value = _uiState.value.copy(
            connectedRelays = relayUrls,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                repository.connectToRelays(relayUrls)
                repository.fetchTopics(relayUrls, limit = 200)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading topics: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load topics: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Refresh topics from current relays
     */
    fun refreshTopics() {
        val currentRelays = _uiState.value.connectedRelays
        if (currentRelays.isEmpty()) {
            Log.w(TAG, "No relays configured for refresh")
            return
        }

        viewModelScope.launch {
            try {
                repository.refresh(currentRelays)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing topics: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to refresh topics: ${e.message}"
                )
            }
        }
    }

    /**
     * Select a hashtag to view its topics
     */
    fun selectHashtag(hashtag: String?) {
        val topicsForHashtag = if (hashtag != null) {
            repository.getTopicsForHashtag(hashtag)
        } else {
            emptyList()
        }

        _uiState.value = _uiState.value.copy(
            selectedHashtag = hashtag,
            topicsForSelectedHashtag = topicsForHashtag,
            isViewingHashtagFeed = hashtag != null
        )
    }

    /**
     * Clear selected hashtag and return to hashtag list
     */
    fun clearSelectedHashtag() {
        _uiState.value = _uiState.value.copy(
            selectedHashtag = null,
            topicsForSelectedHashtag = emptyList(),
            isViewingHashtagFeed = false
        )
    }

    /**
     * Change sort order for hashtags
     */
    fun setSortOrder(sortOrder: HashtagSortOrder) {
        if (_uiState.value.sortOrder != sortOrder) {
            val sortedStats = sortHashtagStats(_uiState.value.hashtagStats, sortOrder)
            _uiState.value = _uiState.value.copy(
                sortOrder = sortOrder,
                hashtagStats = sortedStats
            )
        }
    }

    /**
     * Sort hashtag statistics based on sort order
     */
    private fun sortHashtagStats(
        stats: List<HashtagStats>,
        sortOrder: HashtagSortOrder
    ): List<HashtagStats> {
        return when (sortOrder) {
            HashtagSortOrder.MOST_TOPICS -> stats.sortedByDescending { it.topicCount }
            HashtagSortOrder.MOST_ACTIVE -> stats.sortedByDescending { it.latestActivity }
            HashtagSortOrder.MOST_REPLIES -> stats.sortedByDescending { it.totalReplies }
            HashtagSortOrder.ALPHABETICAL -> stats.sortedBy { it.hashtag.lowercase() }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear all topics and reset state
     */
    fun clearTopics() {
        repository.clearAllTopics()
        _uiState.value = TopicsUiState()
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectAll()
    }
}
