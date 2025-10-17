package com.example.views.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.views.data.Note
import com.example.views.data.NoteUpdate
import com.example.views.data.SampleData
import com.example.views.network.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentDestination: String = "home"
)

class DashboardViewModel : ViewModel() {
    private val webSocketClient = WebSocketClient()
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
        connectWebSocket()
    }
    
    private fun loadInitialData() {
        _uiState.value = _uiState.value.copy(
            notes = SampleData.sampleNotes,
            isLoading = false
        )
    }
    
    private fun connectWebSocket() {
        viewModelScope.launch {
            try {
                webSocketClient.connect()
                webSocketClient.loadNotes(SampleData.sampleNotes)
                
                // Listen for real-time updates
                webSocketClient.realTimeUpdates.collect { update ->
                    handleRealTimeUpdate(update)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Connection error: ${e.message}"
                )
            }
        }
    }
    
    private fun handleRealTimeUpdate(update: NoteUpdate) {
        val currentNotes = _uiState.value.notes.toMutableList()
        val noteIndex = currentNotes.indexOfFirst { it.id == update.noteId }
        
        if (noteIndex != -1) {
            val note = currentNotes[noteIndex]
            val updatedNote = when (update.action) {
                "like" -> note.copy(likes = note.likes + 1, isLiked = true)
                "unlike" -> note.copy(likes = note.likes - 1, isLiked = false)
                "share" -> note.copy(shares = note.shares + 1, isShared = true)
                else -> note
            }
            currentNotes[noteIndex] = updatedNote
            _uiState.value = _uiState.value.copy(notes = currentNotes)
        }
    }
    
    
    
    
    fun toggleLike(noteId: String) {
        viewModelScope.launch {
            val currentNotes = _uiState.value.notes.toMutableList()
            val noteIndex = currentNotes.indexOfFirst { it.id == noteId }
            
            if (noteIndex != -1) {
                val note = currentNotes[noteIndex]
                val updatedNote = if (note.isLiked) {
                    note.copy(
                        likes = note.likes - 1,
                        isLiked = false
                    )
                } else {
                    note.copy(
                        likes = note.likes + 1,
                        isLiked = true
                    )
                }
                currentNotes[noteIndex] = updatedNote
                _uiState.value = _uiState.value.copy(notes = currentNotes)
                
                // Send update via WebSocket
                val update = NoteUpdate(
                    noteId = noteId,
                    action = if (updatedNote.isLiked) "like" else "unlike",
                    userId = "current_user", // In a real app, get from auth
                    timestamp = System.currentTimeMillis()
                )
                webSocketClient.sendNoteUpdate(update)
            }
        }
    }
    
    fun shareNote(noteId: String) {
        viewModelScope.launch {
            val currentNotes = _uiState.value.notes.toMutableList()
            val noteIndex = currentNotes.indexOfFirst { it.id == noteId }
            
            if (noteIndex != -1) {
                val note = currentNotes[noteIndex]
                val updatedNote = note.copy(
                    shares = note.shares + 1,
                    isShared = true
                )
                currentNotes[noteIndex] = updatedNote
                _uiState.value = _uiState.value.copy(notes = currentNotes)
                
                // Send update via WebSocket
                val update = NoteUpdate(
                    noteId = noteId,
                    action = "share",
                    userId = "current_user",
                    timestamp = System.currentTimeMillis()
                )
                webSocketClient.sendNoteUpdate(update)
            }
        }
    }
    
    fun commentOnNote(noteId: String) {
        // In a real app, this would open a comment dialog or navigate to comments
        viewModelScope.launch {
            val currentNotes = _uiState.value.notes.toMutableList()
            val noteIndex = currentNotes.indexOfFirst { it.id == noteId }
            
            if (noteIndex != -1) {
                val note = currentNotes[noteIndex]
                val updatedNote = note.copy(comments = note.comments + 1)
                currentNotes[noteIndex] = updatedNote
                _uiState.value = _uiState.value.copy(notes = currentNotes)
            }
        }
    }
    
    fun openProfile(userId: String) {
        // In a real app, this would navigate to the user's profile
        // For now, we'll just update the state
    }
    
    fun onSidebarItemClick(itemId: String) {
        when (itemId) {
            "profile" -> openProfile("current_user")
            "settings" -> {
                // Navigate to settings
            }
            "logout" -> {
                // Handle logout
            }
            else -> {
                // Handle other menu items
            }
        }
    }
    
    
    fun onMoreOptionClick(option: String) {
        // In a real app, this would handle the selected option
        // For now, we'll just update the state
    }
    
    fun navigateToDestination(destination: String) {
        _uiState.value = _uiState.value.copy(
            currentDestination = destination
        )
        // In a real app, this would navigate to different screens
        // For now, we'll just update the state
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketClient.cleanup()
    }
}
