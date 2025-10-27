# Phase 1: Home Thread View Implementation - COMPLETE

## Overview
Phase 1 implements Kind 1 replies to Kind 1 notes for the home feed, enabling proper threaded conversations following Nostr NIP-10 standards.

## What Was Implemented

### 1. Kind1RepliesRepository ✅
**File**: `app/src/main/java/com/example/views/repository/Kind1RepliesRepository.kt`

- Fetches Kind 1 replies to Kind 1 notes using Quartz NostrClient
- Parses "e" tags to extract thread relationships (root/reply markers)
- Filters replies that reference the parent note
- Manages WebSocket subscriptions per note
- Provides state flows for replies, loading state, and errors
- Supports multiple relay connections

**Key Features**:
- Thread relationship parsing (root, reply-to, direct reply detection)
- Duplicate detection and prevention
- Automatic connection management
- Error handling and recovery

### 2. Kind1RepliesViewModel ✅
**File**: `app/src/main/java/com/example/views/viewmodel/Kind1RepliesViewModel.kt`

- Manages UI state for Kind 1 replies
- Observes repository flows and updates UI state
- Provides sorting options (chronological, reverse, most liked)
- Handles reply interactions (like, refresh)
- Manages lifecycle (cleanup on cleared)

**State Management**:
- `Kind1RepliesUiState` data class for immutable state
- `Kind1ReplySortOrder` enum for sort preferences
- Reactive state updates via StateFlow

### 3. ModernThreadViewScreen Updates ✅
**File**: `app/src/main/java/com/example/views/ui/screens/ModernThreadViewScreen.kt`

**Added Parameters**:
- `replyKind: Int = 1111` - Determines which reply type to load (1 or 1111)
- `kind1RepliesViewModel: Kind1RepliesViewModel` - ViewModel for Kind 1 replies

**Dual-Mode Support**:
- Automatically selects the correct ViewModel based on `replyKind`
- Converts Kind1RepliesUiState to ThreadRepliesUiState for compatibility
- Uses existing ThreadReply conversion functions (`Note.toThreadReply()`)

### 4. Navigation Updates ✅
**File**: `app/src/main/java/com/example/views/ui/navigation/RibbitNavigation.kt`

**Route Changes**:
- Updated route: `"thread/{noteId}?replyKind={replyKind}"`
- Added `replyKind` argument with default value of 1 (Kind 1 for home feed)

**Navigation Function**:
- Updated `navigateToThread(noteId: String, replyKind: Int = 1)`
- Default behavior: home feed threads use Kind 1 replies
- Future: topics can pass `replyKind = 1111` for Kind 1111 replies

## How It Works

### Home Feed Flow
```
DashboardScreen (Kind 1 notes)
  ↓ User taps a note
ModernThreadViewScreen (replyKind = 1)
  ↓ Loads from Kind1RepliesViewModel
Kind1RepliesRepository
  ↓ Fetches Kind 1 replies with "e" tags referencing parent
Relay Network
  ↓ Returns Kind 1 events
Display threaded replies
```

### Thread Relationship Parsing (NIP-10)
```json
{
  "kind": 1,
  "tags": [
    ["e", "<root-note-id>", "<relay>", "root"],
    ["e", "<parent-note-id>", "<relay>", "reply"],
    ["p", "<author-pubkey>"]
  ]
}
```

- **root**: The original note being replied to
- **reply**: The direct parent (can be root or another reply)
- **Fallback**: If no markers, first "e" = root, last "e" = reply-to

## Testing Checklist

- [ ] Home feed displays Kind 1 notes
- [ ] Tapping a note navigates to thread view
- [ ] Thread view loads Kind 1 replies from relays
- [ ] Replies display with proper author info
- [ ] Reply timestamps are formatted correctly
- [ ] Loading state shows during reply fetch
- [ ] Error state displays if fetch fails
- [ ] Pull-to-refresh works for replies
- [ ] Back navigation returns to home feed
- [ ] Thread state persists across navigation
- [ ] Multiple relay support works correctly
- [ ] Duplicate replies are prevented
- [ ] Sort options work (chronological, etc.)

## Architecture Decisions

### 1. Separate Repositories
**Why**: Kind 1 and Kind 1111 have different filtering needs and tag structures
- Kind 1: Uses lowercase "e" tags, standard reply format
- Kind 1111: Uses uppercase "E" and "e" tags, forum-style threading

### 2. Dual ViewModel Pattern
**Why**: Clean separation of concerns while maintaining code reuse
- Each ViewModel manages its own repository
- ModernThreadViewScreen acts as a unified view layer
- State conversion happens at the screen level

### 3. Navigation Parameter
**Why**: Single screen handles both thread types with different data sources
- `replyKind` parameter explicitly declares intent
- Default to Kind 1 (home feed is primary use case)
- Easy to extend for future thread types

### 4. State Conversion
**Why**: Reuse existing ThreadReply components and logic
- `Note.toThreadReply()` converts home feed notes to thread format
- Maintains compatibility with existing UI components
- Minimal code duplication

## Known Limitations

1. **Thread Level Calculation**: Current implementation uses simplified thread level detection. Full nested threading requires parsing the complete "e" tag chain.

2. **Profile Lookup**: Author info uses pubkey truncation instead of profile metadata (kind 0). Future enhancement: integrate profile repository.

3. **Media Detection**: Uses regex for image URLs. Could be enhanced with NIP-92 (Media Attachments) support.

4. **Relay Selection**: Uses user's favorite/default category. Could add per-thread relay selection.

## Performance Considerations

- Repository uses coroutine scope with SupervisorJob for lifecycle management
- WebSocket subscriptions are cached per note to avoid redundant connections
- Reply fetch timeout of 10 seconds prevents indefinite loading
- Duplicate detection prevents unnecessary state updates
- State flows use `asStateFlow()` for read-only external access

## Next Steps: Phase 2 - Topics & Hashtag Discovery

### Goals
1. Create `TopicsRepository` for Kind 11 topic discovery
2. Implement hashtag extraction and statistics
3. Build `TopicsListScreen` for hashtag browsing
4. Wire up navigation: Topics → Topic Feed → Topic Thread
5. Update TopicsScreen to show hashtags instead of Kind 1 feed

### New Components Needed
- `TopicsRepository.kt` - Fetch Kind 11 topics, extract hashtags
- `TopicsViewModel.kt` - Manage hashtag statistics and topic lists
- `TopicFeedScreen.kt` - Display Kind 11 topics for a specific hashtag
- `Kind11ThreadView.kt` - Display Kind 11 topic with Kind 1111 replies

### Data Models Needed
```kotlin
data class TopicNote(
    val id: String,
    val author: Author,
    val title: String,
    val content: String,
    val hashtags: List<String>,
    val replyCount: Int,
    val timestamp: Long
)

data class HashtagStats(
    val hashtag: String,
    val topicCount: Int,
    val totalReplies: Int,
    val latestActivity: Long
)
```

## References

- **NIP-10**: Reply Tags and Thread References
  - https://github.com/nostr-protocol/nips/blob/master/10.md
  
- **NIP-22**: Event `created_at` Limits (Threading Context)
  - https://github.com/nostr-protocol/nips/blob/master/22.md

- **Amethyst Threading**: Reference implementation
  - `amethyst/app/src/main/java/com/vitorpamplona/amethyst/ui/note/ThreadView.kt`

- **RelayTools Topics**: Hashtag discovery pattern
  - `RelayTools-android-master/app/src/main/java/com/example/relaytools/ui/TopicsView.kt`

## Commit Message Suggestion
```
feat: Implement Phase 1 - Kind 1 home feed threading

- Add Kind1RepliesRepository for fetching Kind 1 replies
- Add Kind1RepliesViewModel for managing reply state
- Update ModernThreadViewScreen to support both Kind 1 and Kind 1111
- Add replyKind navigation parameter for thread type selection
- Implement NIP-10 reply tag parsing for thread relationships
- Add state conversion between Note and ThreadReply models

Closes #XX (home feed threading)
Part of Topics & Threading implementation roadmap
```

---

**Date Completed**: 2024
**Status**: ✅ Ready for Testing
**Next Phase**: Phase 2 - Topics & Hashtag Discovery