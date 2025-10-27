package com.example.views.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.views.data.Note
import com.example.views.repository.Kind1RepliesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class Kind1RepliesUiState(
    val note: Note? = null,
    val replies: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalReplyCount: Int = 0,
    val sortOrder: Kind1ReplySortOrder = Kind1ReplySortOrder.CHRONOLOGICAL
)

enum class Kind1ReplySortOrder {
    CHRONOLOGICAL,           // Oldest first
    REVERSE_CHRONOLOGICAL,   // Newest first
    MOST_LIKED               // Most liked first
}

/**
 * ViewModel for managing Kind 1 replies to Kind 1 notes
 * Handles fetching, organizing, and displaying threaded conversations for home feed
 */
class Kind1RepliesViewModel : ViewModel() {
    private val repository = Kind1RepliesRepository()

    private val _uiState = MutableStateFlow(Kind1RepliesUiState())
    val uiState: StateFlow<Kind1RepliesUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "Kind1RepliesViewModel"
    }

    init {
        observeRepliesFromRepository()
    }

    /**
     * Observe replies from the repository
     */
    private fun observeRepliesFromRepository() {
        viewModelScope.launch {
            repository.replies.collect { repliesMap ->
                val currentNote = _uiState.value.note
                if (currentNote != null) {
                    val replies = repliesMap[currentNote.id] ?: emptyList()
                    updateRepliesState(replies)
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
     * Load Kind 1 replies for a specific note
     */
    fun loadRepliesForNote(note: Note, relayUrls: List<String>) {
        Log.d(TAG, "Loading Kind 1 replies for note ${note.id.take(8)}... from ${relayUrls.size} relays")

        _uiState.value = _uiState.value.copy(
            note = note,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                // Connect to relays if not already connected
                repository.connectToRelays(relayUrls)

                // Fetch replies for this note
                repository.fetchRepliesForNote(
                    noteId = note.id,
                    relayUrls = relayUrls,
                    limit = 200
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading replies: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load replies: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Update replies state with sorting
     */
    private fun updateRepliesState(replies: List<Note>) {
        val sortedReplies = when (_uiState.value.sortOrder) {
            Kind1ReplySortOrder.CHRONOLOGICAL -> replies.sortedBy { it.timestamp }
            Kind1ReplySortOrder.REVERSE_CHRONOLOGICAL -> replies.sortedByDescending { it.timestamp }
            Kind1ReplySortOrder.MOST_LIKED -> replies.sortedByDescending { it.likes }
        }

        _uiState.value = _uiState.value.copy(
            replies = sortedReplies,
            totalReplyCount = replies.size,
            isLoading = false
        )

        Log.d(TAG, "Updated replies state: ${replies.size} replies")
    }

    /**
     * Change sort order for replies
     */
    fun setSortOrder(sortOrder: Kind1ReplySortOrder) {
        if (_uiState.value.sortOrder != sortOrder) {
            _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
            updateRepliesState(_uiState.value.replies)
        }
    }

    /**
     * Refresh replies for current note
     */
    fun refreshReplies(relayUrls: List<String>) {
        val currentNote = _uiState.value.note
        if (currentNote != null) {
            loadRepliesForNote(currentNote, relayUrls)
        }
    }

    /**
     * Like a reply
     */
    fun likeReply(replyId: String) {
        viewModelScope.launch {
            val currentReplies = _uiState.value.replies
            val updatedReplies = currentReplies.map { reply ->
                if (reply.id == replyId) {
                    reply.copy(
                        likes = reply.likes + (if (reply.isLiked) -1 else 1),
                        isLiked = !reply.isLiked
                    )
                } else {
                    reply
                }
            }
            updateRepliesState(updatedReplies)
        }
    }

    /**
     * Build threaded structure for display
     * Returns map of parent ID to child notes for hierarchical rendering
     */
    fun buildThreadStructure(): Map<String, List<Note>> {
        val noteId = _uiState.value.note?.id ?: return emptyMap()
        return repository.buildThreadStructure(noteId)
    }

    /**
     * Clear all replies and reset state
     */
    fun clearReplies() {
        repository.clearAllReplies()
        _uiState.value = Kind1RepliesUiState()
    }

    /**
     * Clear replies for specific note
     */
    fun clearRepliesForNote(noteId: String) {
        repository.clearRepliesForNote(noteId)
        if (_uiState.value.note?.id == noteId) {
            _uiState.value = _uiState.value.copy(
                replies = emptyList(),
                totalReplyCount = 0
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectAll()
    }
}
