package com.example.views.data

import kotlinx.serialization.Serializable

/**
 * Thread reply from Kind 1111 events (NIP-22)
 * Replies to notes following forum-style threading
 *
 * Kind 1111 events are used for threaded conversations where:
 * - Root note is typically a kind 1 event
 * - Replies are kind 1111 events
 * - Each reply can have nested replies
 */
@Serializable
data class ThreadReply(
    val id: String,
    val author: Author,
    val content: String,
    val timestamp: Long,
    val likes: Int = 0,
    val shares: Int = 0,
    val replies: Int = 0,
    val isLiked: Boolean = false,
    val hashtags: List<String> = emptyList(),
    val mediaUrls: List<String> = emptyList(),

    // Thread relationship fields (NIP-22)
    val rootNoteId: String? = null,      // The original note being replied to
    val replyToId: String? = null,       // Direct reply to (can be root or another reply)
    val threadLevel: Int = 0             // Nesting level in thread (0 = direct reply to root)
) {
    /**
     * Get short content preview (first 100 characters)
     */
    val shortContent: String
        get() = if (content.length > 100) {
            content.take(100) + "..."
        } else {
            content
        }

    /**
     * Check if this is a direct reply to the root note
     */
    val isDirectReply: Boolean
        get() = replyToId == rootNoteId || threadLevel == 0

    /**
     * Extract image URLs from content (simple regex for now)
     */
    fun extractImageUrls(): List<String> {
        val imageUrlPattern = Regex("""https?://[^\s]+\.(jpg|jpeg|png|gif|webp)""", RegexOption.IGNORE_CASE)
        return imageUrlPattern.findAll(content).map { it.value }.toList()
    }

    companion object {
        /**
         * Parse tags to extract thread relationship information
         *
         * NIP-22 format:
         * - ["e", <root-id>, <relay-url>, "root"] - Root event reference
         * - ["e", <parent-id>, <relay-url>, "reply"] - Parent reply reference
         * - ["p", <pubkey>] - Author being replied to
         */
        fun parseThreadTags(tags: List<List<String>>): Triple<String?, String?, Int> {
            var rootId: String? = null
            var replyToId: String? = null
            var level = 0

            tags.forEach { tag ->
                if (tag.isNotEmpty() && tag[0] == "e") {
                    val eventId = tag.getOrNull(1)
                    val marker = tag.getOrNull(3)

                    when (marker) {
                        "root" -> rootId = eventId
                        "reply" -> replyToId = eventId
                        else -> {
                            // If no marker, first "e" tag is root, second is reply
                            if (rootId == null) {
                                rootId = eventId
                            } else if (replyToId == null) {
                                replyToId = eventId
                            }
                        }
                    }
                }
            }

            // Calculate thread level: if parent == root, it's a top-level reply (0)
            // Otherwise it's a nested reply (1+)
            level = if (replyToId == rootId || replyToId == null) 0 else 1

            return Triple(rootId, replyToId, level)
        }
    }
}

/**
 * Threaded reply structure for hierarchical display
 * Organizes replies into a tree structure for nested threading
 */
@Serializable
data class ThreadedReply(
    val reply: ThreadReply,
    val children: List<ThreadedReply> = emptyList(),
    val level: Int = 0
) {
    /**
     * Get total reply count including all nested children
     */
    val totalReplies: Int
        get() = children.size + children.sumOf { it.totalReplies }

    /**
     * Flatten the thread into a list for display
     */
    fun flatten(): List<Pair<ThreadReply, Int>> {
        val result = mutableListOf<Pair<ThreadReply, Int>>()
        result.add(reply to level)
        children.forEach { child ->
            result.addAll(child.flatten())
        }
        return result
    }
}

/**
 * Note with its threaded replies
 * Combines a root note with all its replies organized hierarchically
 */
@Serializable
data class NoteWithReplies(
    val note: Note,
    val replies: List<ThreadReply> = emptyList(),
    val totalReplyCount: Int = 0,
    val isLoadingReplies: Boolean = false
) {
    /**
     * Get threaded replies organized by nesting level
     */
    val threadedReplies: List<ThreadedReply>
        get() = organizeRepliesIntoThreads()

    /**
     * Get direct replies to the main note (level 0)
     */
    val directReplies: List<ThreadReply>
        get() = replies.filter { it.isDirectReply }

    /**
     * Organize flat list of replies into threaded structure
     */
    private fun organizeRepliesIntoThreads(): List<ThreadedReply> {
        if (replies.isEmpty()) return emptyList()

        val replyMap = replies.associateBy { it.id }

        // Build threaded structure recursively
        fun buildThreadedReply(reply: ThreadReply, level: Int = 0): ThreadedReply {
            val children = replies
                .filter { it.replyToId == reply.id }
                .map { buildThreadedReply(it, level + 1) }
                .sortedBy { it.reply.timestamp }

            return ThreadedReply(
                reply = reply,
                children = children,
                level = level
            )
        }

        // Find root-level replies (those that reply directly to the note)
        val rootReplyIds = replies
            .filter { reply ->
                reply.replyToId == note.id ||
                reply.replyToId == null ||
                replies.none { it.id == reply.replyToId }
            }
            .map { buildThreadedReply(it) }
            .sortedBy { it.reply.timestamp }

        return rootReplyIds
    }
}

/**
 * Helper extension functions for thread replies
 */

/**
 * Convert a Note to a ThreadReply for consistent handling
 */
fun Note.toThreadReply(replyToId: String? = null, threadLevel: Int = 0): ThreadReply {
    return ThreadReply(
        id = id,
        author = author,
        content = content,
        timestamp = timestamp,
        likes = likes,
        shares = shares,
        replies = comments, // Map comments count to replies
        isLiked = isLiked,
        hashtags = hashtags,
        mediaUrls = mediaUrls,
        rootNoteId = replyToId,
        replyToId = replyToId,
        threadLevel = threadLevel
    )
}

/**
 * Convert a ThreadReply back to a Note for display compatibility
 */
fun ThreadReply.toNote(): Note {
    return Note(
        id = id,
        author = author,
        content = content,
        timestamp = timestamp,
        likes = likes,
        shares = shares,
        comments = replies, // Map replies count to comments
        isLiked = isLiked,
        hashtags = hashtags,
        mediaUrls = mediaUrls
    )
}
