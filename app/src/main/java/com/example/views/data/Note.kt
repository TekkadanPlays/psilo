package com.example.views.data

import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Note(
    val id: String,
    val author: Author,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0,
    val shares: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
    val isShared: Boolean = false,
    val mediaUrls: List<String> = emptyList(),
    val hashtags: List<String> = emptyList()
)

@Serializable
data class Author(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false
)

@Serializable
data class Comment(
    val id: String,
    val author: Author,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0,
    val isLiked: Boolean = false
)

@Serializable
data class WebSocketMessage(
    val type: String,
    val data: String
)

enum class NoteAction {
    LIKE, UNLIKE, SHARE, COMMENT, DELETE
}

@Serializable
data class NoteUpdate(
    val noteId: String,
    val action: String,
    val userId: String,
    val timestamp: Long
)
