# Topics and Thread Views Implementation Plan

## Architecture Overview

### 1. Home Feed (Kind 1 Notes)
- **Feed**: Shows kind 1 regular notes
- **Thread View**: Shows kind 1 replies using "e" tags (like Amethyst)
- **Reply Structure**: 
  - Uses `replyTo` field on notes
  - "e" tags with "root" and "reply" markers
  - Hierarchical threading based on parent references

### 2. Topics Feed (Kind 11 Topics)
- **Main View**: Hashtag list with statistics (like RelayTools)
- **Topic Feed**: List of kind 11 threads under selected hashtag
- **Thread View**: Shows kind 1111 replies to kind 11 topics
- **Reply Structure**:
  - Kind 11 = Topic starter (has title + hashtag)
  - Kind 1111 = Threaded replies to kind 11
  - Uses "E" tags for root and "e" tags for parent

## Data Models Needed

### Kind 1 Thread Structure
```kotlin
// Already exists in Note.kt
data class Note(
    val id: String,
    val author: Author,
    val content: String,
    val replyTo: List<Note>? = null,  // Kind 1 parent notes
    val replies: List<Note> = emptyList()  // Kind 1 child replies
)
```

### Kind 11 Topic Structure
```kotlin
data class TopicNote(
    val id: String,
    val author: Author,
    val title: String,  // From "title" tag
    val content: String,
    val hashtags: List<String>,  // From "t" tags
    val replyCount: Int = 0,  // Count of kind 1111 replies
    val timestamp: Long
)
```

### Hashtag Statistics
```kotlin
data class HashtagStats(
    val hashtag: String,
    val topicCount: Int,
    val totalReplies: Int,
    val latestActivity: Long
)
```

## Repositories Needed

### 1. Kind1RepliesRepository
- Fetch kind 1 replies to kind 1 notes
- Filter: `kinds = [1], tags = {"e": [noteId]}`
- Build reply tree using "e" tag markers (root/reply)

### 2. Kind11TopicsRepository
- Fetch kind 11 topics from relays
- Filter: `kinds = [11], tags = {"t": [hashtag]}`
- Extract title from tags
- Group by hashtags

### 3. Kind1111RepliesRepository (already created)
- Fetch kind 1111 replies to kind 11 topics
- Filter: `kinds = [1111], tags = {"E": [topicId]}`
- Build threaded structure

## UI Components Needed

### 1. TopicsListScreen (Hashtag Discovery)
```
+----------------------------------+
| topics                     [≡][⚙] |
+----------------------------------+
| #bitcoin              42 topics  |
| Latest: 2m ago       128 replies |
+----------------------------------+
| #nostr                28 topics  |
| Latest: 5m ago        89 replies |
+----------------------------------+
```

### 2. TopicFeedScreen (Kind 11 List)
```
+----------------------------------+
| #bitcoin               [≡][⚙]    |
+----------------------------------+
| Why Bitcoin matters              |
| by @alice • 42 replies • 2h ago  |
| Discussion about Bitcoin's...    |
+----------------------------------+
| Bitcoin scaling solutions        |
| by @bob • 15 replies • 4h ago    |
| Let's talk about Lightning...    |
+----------------------------------+
```

### 3. Kind1ThreadView (Home Thread)
```
+----------------------------------+
| thread                [←][⚙]     |
+----------------------------------+
| [Parent Note]                    |
| Original kind 1 note content     |
+----------------------------------+
| ↳ @alice • 2m ago               |
|   First reply (kind 1)           |
|   ↳ @bob • 1m ago               |
|     Nested reply (kind 1)        |
+----------------------------------+
```

### 4. Kind11ThreadView (Topic Thread)
```
+----------------------------------+
| #bitcoin               [←][⚙]    |
+----------------------------------+
| [Topic: Why Bitcoin matters]     |
| Original kind 11 topic content   |
+----------------------------------+
| ↳ @alice • 2m ago               |
|   First reply (kind 1111)        |
|   ↳ @bob • 1m ago               |
|     Nested reply (kind 1111)     |
+----------------------------------+
```

## Navigation Flow

### Home Feed Flow
```
DashboardScreen (kind 1 feed)
  → tap note
  → Kind1ThreadView (kind 1 replies)
```

### Topics Flow
```
TopicsListScreen (hashtag list)
  → tap hashtag (#bitcoin)
  → TopicFeedScreen (kind 11 topics with #bitcoin)
    → tap kind 11 topic
    → Kind11ThreadView (kind 1111 replies)
```

## Implementation Steps

### Phase 1: Fix Home Thread View (Kind 1 Replies)
1. Create `Kind1RepliesRepository`
   - Subscribe to kind 1 events with "e" tags
   - Parse "root" and "reply" markers
   - Build reply tree

2. Update `ModernThreadViewScreen`
   - Accept flag for reply type (kind 1 vs kind 1111)
   - Use appropriate repository based on flag
   - Display kind 1 replies for home feed

3. Wire up navigation
   - DashboardScreen → Kind1ThreadView

### Phase 2: Topics Hashtag Discovery
1. Create `Kind11TopicsRepository`
   - Subscribe to kind 11 events
   - Extract hashtags from "t" tags
   - Calculate statistics

2. Create `TopicsListScreen`
   - Display hashtag list
   - Show topic count and reply count
   - Tap → navigate to TopicFeedScreen

3. Update bottom navigation
   - Replace messages with topics (#)

### Phase 3: Topic Feed View
1. Create `TopicFeedScreen`
   - Filter kind 11 by selected hashtag
   - Display list of topics
   - Show title, author, reply count
   - Tap → navigate to Kind11ThreadView

2. Update header
   - Show selected hashtag (#bitcoin)
   - Add sort dropdown (latest, popular, most replies)

### Phase 4: Topic Thread View
1. Update `ModernThreadViewScreen`
   - Support both kind 1 and kind 1111 replies
   - Use flag to determine which repository
   - Display appropriately

2. Wire up navigation
   - TopicFeedScreen → Kind11ThreadView

## Tag Structure Reference

### Kind 1 Reply Tags (Home Thread)
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

### Kind 11 Topic Tags
```json
{
  "kind": 11,
  "tags": [
    ["title", "Why Bitcoin matters"],
    ["t", "bitcoin"],
    ["t", "cryptocurrency"]
  ],
  "content": "Long form discussion about Bitcoin..."
}
```

### Kind 1111 Reply Tags (Topic Thread)
```json
{
  "kind": 1111,
  "tags": [
    ["E", "<kind-11-topic-id>", "<relay>", "root"],
    ["e", "<parent-reply-id>", "<relay>", "reply"],
    ["p", "<author-pubkey>"]
  ]
}
```

## Key Differences

| Feature | Home Thread | Topic Thread |
|---------|-------------|--------------|
| Parent | Kind 1 | Kind 11 |
| Replies | Kind 1 | Kind 1111 |
| Root tag | "e" lowercase | "E" uppercase |
| Has title | No | Yes |
| Has hashtags | Optional | Required |

## Testing Checklist

- [ ] Home feed shows only kind 1 notes
- [ ] Tapping kind 1 note shows kind 1 replies
- [ ] Kind 1 thread view displays correctly
- [ ] Topics list shows hashtags with stats
- [ ] Tapping hashtag shows kind 11 topics
- [ ] Topic feed shows only topics with selected hashtag
- [ ] Tapping kind 11 topic shows kind 1111 replies
- [ ] Kind 1111 thread view displays correctly
- [ ] Navigation works correctly between all screens
- [ ] Back button behavior is correct

## Next Session Goals

1. Create `Kind1RepliesRepository`
2. Update `ModernThreadViewScreen` to support both reply types
3. Test home thread view with kind 1 replies
4. Create `TopicsListScreen` with hashtag discovery
5. Wire up navigation properly