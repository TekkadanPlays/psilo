# Real Quartz/Nostr Integration - Complete Implementation

**Status**: ✅ **FULLY IMPLEMENTED**  
**Date**: 2025-01-XX  
**Phase**: Production-Ready Real Data Integration

---

## 🎯 Overview

This document describes the complete, production-ready Quartz/Nostr integration for the Ribbit Android app. All components are now wired to fetch **real data** from live Nostr relays using the Quartz library patterns from Amethyst.

---

## 🏗️ Architecture Overview

### Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                      │
│  ┌─────────────────┐  ┌──────────────────────────────┐    │
│  │ DashboardScreen │  │ AnnouncementsFeedScreen       │    │
│  └────────┬────────┘  └───────────┬──────────────────┘    │
│           │                        │                        │
└───────────┼────────────────────────┼────────────────────────┘
            │                        │
            ▼                        ▼
┌───────────────────────────────────────────────────────────┐
│                   ViewModel Layer                          │
│  ┌──────────────────────┐  ┌─────────────────────────┐   │
│  │ DashboardViewModel   │  │ AnnouncementsFeedVM     │   │
│  └──────────┬───────────┘  └───────────┬─────────────┘   │
│             │                           │                  │
└─────────────┼───────────────────────────┼──────────────────┘
              │                           │
              ▼                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│  ┌──────────────────┐  ┌────────────────────────────────┐  │
│  │ NotesRepository  │  │ ProfileCacheRepository         │  │
│  └────────┬─────────┘  └───────────┬────────────────────┘  │
│           │                         │                        │
│           └─────────┬───────────────┘                        │
│                     │                                        │
│                     ▼                                        │
│            ┌─────────────────┐                              │
│            │  NostrClient    │  (Quartz)                    │
│            └─────────────────┘                              │
└──────────────────────────────────────────────────────────────┘
                      │
                      ▼
              [Nostr Relays]
           wss://relay.damus.io
           wss://nos.lol
           wss://relay.primal.net
```

---

## 📦 Core Components

### 1. NotesRepository

**Location**: `app/src/main/java/com/example/views/repository/NotesRepository.kt`

**Purpose**: Fetches and manages kind-1 (text note) events from Nostr relays.

**Key Features**:
- ✅ Real Quartz `NostrClient` integration
- ✅ Multi-relay subscription support
- ✅ Per-category relay filtering
- ✅ Single-relay loading (tap relay in sidebar)
- ✅ Profile cache integration
- ✅ Automatic profile fetching for note authors
- ✅ Batch profile loading optimization

**Key Methods**:
```kotlin
fun connectToRelays(relayUrls: List<String>)
fun subscribeToNotes(limit: Int = 100)
fun subscribeToRelayNotes(relayUrl: String, limit: Int = 100)
fun refresh()
fun updateNotesWithProfiles()
fun disconnectAll()
```

**Data Flow**:
1. Connect to relays via `NostrClient`
2. Subscribe to kind-1 events with `NostrClientSubscription`
3. Filter: `Filter(kinds = [1], limit = 100)`
4. Handle incoming events via `onEvent` callback
5. Convert `Event` → `Note` data model
6. Emit to `StateFlow<List<Note>>`
7. Trigger profile fetching for uncached authors

---

### 2. ProfileCacheRepository

**Location**: `app/src/main/java/com/example/views/repository/ProfileCacheRepository.kt`

**Purpose**: Caches user profile metadata (kind-0 events) from cache relays.

**Key Features**:
- ✅ Profile caching with `ConcurrentHashMap`
- ✅ Kind-0 (metadata) event subscription
- ✅ Batch profile fetching
- ✅ Automatic profile updates
- ✅ `UserMetadata` → `Author` conversion
- ✅ Observable profile updates via `StateFlow`

**Key Methods**:
```kotlin
fun setCacheRelays(relayUrls: List<String>)
fun fetchProfiles(pubkeys: List<String>)
fun getCachedProfile(pubkey: String): Author
fun isCached(pubkey: String): Boolean
fun clearCache()
```

**Profile Fetching Flow**:
1. Receive list of pubkeys to fetch
2. Filter out already cached pubkeys
3. Subscribe to kind-0 events with `Filter(kinds = [0], authors = [...])`
4. Parse `MetadataEvent` → `UserMetadata`
5. Convert to `Author` data model
6. Cache in memory
7. Emit updates via `StateFlow`

**Metadata Fields Extracted**:
- `name` / `username` → `Author.username`
- `display_name` → `Author.displayName`
- `picture` → `Author.avatarUrl`
- `nip05Verified` → `Author.isVerified`

---

### 3. DashboardViewModel

**Location**: `app/src/main/java/com/example/views/viewmodel/DashboardViewModel.kt`

**Purpose**: Manages home feed state and coordinates note/profile loading.

**Key Features**:
- ✅ Integrates `NotesRepository` + `ProfileCacheRepository`
- ✅ Observes note updates from repository
- ✅ Observes profile updates and refreshes notes
- ✅ Loads notes from favorite category
- ✅ Loads notes from specific relay (sidebar tap)
- ✅ Handles empty states (no relays configured)

**Key Methods**:
```kotlin
fun loadNotesFromFavoriteCategory(relayUrls: List<String>)
fun loadNotesFromSpecificRelay(relayUrl: String)
fun refreshNotes()
fun setCacheRelays(relayUrls: List<String>)
```

**State Flow**:
```kotlin
data class DashboardUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasRelays: Boolean = false,
    val isLoadingFromRelays: Boolean = false
)
```

---

### 4. AnnouncementsFeedViewModel

**Location**: `app/src/main/java/com/example/views/viewmodel/AnnouncementsFeedViewModel.kt`

**Purpose**: Manages announcements feed (Tekkadan official updates).

**Key Features**:
- ✅ Dedicated feed for Tekkadan announcements
- ✅ Single-relay subscription (announcement relay)
- ✅ Optional pubkey filtering (announcement account)
- ✅ Separate from home feed

**Key Methods**:
```kotlin
fun setAnnouncementRelay(relayUrl: String)
fun setAnnouncementPubkey(pubkey: String)
fun loadAnnouncements()
fun refreshAnnouncements()
```

**Configuration**:
```kotlin
// Example: Set Tekkadan relay and pubkey
viewModel.setAnnouncementRelay("wss://relay.tekkadan.com")
viewModel.setAnnouncementPubkey("tekk_pubkey_hex")
viewModel.loadAnnouncements()
```

---

### 5. AnnouncementsFeedScreen

**Location**: `app/src/main/java/com/example/views/ui/screens/AnnouncementsFeedScreen.kt`

**Purpose**: UI for displaying Tekkadan announcements.

**Key Features**:
- ✅ Material3 design with collapsible header
- ✅ Pull-to-refresh support
- ✅ Empty state handling
- ✅ Loading states
- ✅ Reuses `NoteCard` component

---

## 🔄 Data Flow Examples

### Example 1: Loading Home Feed from Favorite Category

```
User opens app
    ↓
DashboardScreen LaunchedEffect
    ↓
Detect favorite category: "General" (3 relays)
    ↓
viewModel.loadNotesFromFavoriteCategory([relay1, relay2, relay3])
    ↓
NotesRepository.connectToRelays([relay1, relay2, relay3])
    ↓
NostrClient.connect()
    ↓
NotesRepository.subscribeToNotes(limit = 100)
    ↓
NostrClientSubscription created with Filter(kinds = [1], limit = 100)
    ↓
Events start arriving: onEvent(event) callback
    ↓
For each event:
    - Convert Event → Note
    - Check if author profile is cached
    - If not cached, add to pendingProfileFetches
    - Emit note to StateFlow
    ↓
After 500ms delay:
    - ProfileCacheRepository.fetchProfiles(pendingPubkeys)
    - Subscribe to kind-0 events for those pubkeys
    ↓
Metadata events arrive:
    - Parse MetadataEvent → UserMetadata → Author
    - Cache in profileCache
    - Emit profileUpdates
    ↓
DashboardViewModel observes profileUpdates:
    - notesRepository.updateNotesWithProfiles()
    - Re-map notes with fresh Author data
    ↓
UI updates with notes + profile info (names, avatars)
```

---

### Example 2: Loading Single Relay from Sidebar

```
User taps relay "wss://nos.lol" in sidebar
    ↓
GlobalSidebar itemId: "relay:wss://nos.lol"
    ↓
viewModel.loadNotesFromSpecificRelay("wss://nos.lol")
    ↓
NotesRepository.disconnectAll() (close previous connections)
    ↓
NotesRepository.connectToRelays(["wss://nos.lol"])
    ↓
NotesRepository.subscribeToRelayNotes("wss://nos.lol", limit = 100)
    ↓
NostrClientSubscription with single relay filter
    ↓
Events arrive only from "wss://nos.lol"
    ↓
Notes display updates to show only notes from that relay
```

---

### Example 3: Profile Fetching and Caching

```
Note arrives with pubkey "abcd1234..."
    ↓
NotesRepository.convertEventToNote(event)
    ↓
Check: profileCache.isCached("abcd1234...") → false
    ↓
Add to pendingProfileFetches set
    ↓
After 500ms (batch window):
    ↓
ProfileCacheRepository.fetchProfiles(["abcd1234...", "efgh5678...", ...])
    ↓
Filter out already cached pubkeys
    ↓
Subscribe to kind-0 events:
    Filter(kinds = [0], authors = ["abcd1234...", ...])
    ↓
Metadata event arrives for "abcd1234..."
    ↓
Parse JSON content → UserMetadata
    {
      "name": "alice",
      "display_name": "Alice",
      "picture": "https://avatar.url/alice.jpg",
      "nip05": "alice@example.com"
    }
    ↓
Convert to Author:
    Author(
      id = "abcd1234...",
      username = "alice",
      displayName = "Alice",
      avatarUrl = "https://avatar.url/alice.jpg",
      isVerified = true
    )
    ↓
Cache in ConcurrentHashMap
    ↓
Emit profileUpdates StateFlow
    ↓
DashboardViewModel.observeProfileUpdates() receives update
    ↓
notesRepository.updateNotesWithProfiles()
    ↓
Re-map all notes: note.copy(author = profileCache.getCachedProfile(note.author.id))
    ↓
UI updates with real names and avatars
```

---

## 🛠️ Quartz Integration Patterns

### NostrClient Initialization

```kotlin
private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val okHttpClient = OkHttpClient.Builder().build()
private val socketBuilder = BasicOkHttpWebSocket.Builder(okHttpClient)
private val nostrClient = NostrClient(socketBuilder, scope)
```

### Subscription Pattern

```kotlin
val filter = Filter(
    kinds = listOf(1),
    authors = listOf("pubkey1", "pubkey2"), // optional
    limit = 100
)

val relayFilters = relayUrls.associate { url ->
    NormalizedRelayUrl(url) to listOf(filter)
}

val subscription = NostrClientSubscription(
    client = nostrClient,
    filter = { relayFilters },
    onEvent = { event ->
        // Handle event
    }
)

// Later: close subscription
subscription.close()
```

### Event Handling

```kotlin
private fun handleEvent(event: Event) {
    if (event.kind == 1) {
        val note = convertEventToNote(event)
        val currentNotes = _notes.value
        if (!currentNotes.any { it.id == note.id }) {
            _notes.value = (currentNotes + note).sortedByDescending { it.timestamp }
        }
    }
}
```

### Metadata Parsing

```kotlin
if (event.kind == 0) {
    val metadataEvent = event as? MetadataEvent
    val metadata = metadataEvent?.contactMetaData()
    metadata?.cleanBlankNames()
    
    val author = Author(
        id = event.pubKey,
        username = metadata?.name ?: event.pubKey.take(8),
        displayName = metadata?.displayName ?: metadata?.name,
        avatarUrl = metadata?.picture,
        isVerified = metadata?.nip05Verified ?: false
    )
}
```

---

## 🔧 Configuration Guide

### Step 1: Configure Relay Categories

Users configure relay categories in `RelayManagementScreen`:
- **General Tab**: Create categories (e.g., "Friends", "Tech", "General")
- Add relays to each category
- Mark one category as **Favorite** (⭐) for home feed

### Step 2: Configure Personal Relays

Users configure personal relays in `RelayManagementScreen`:
- **Personal Tab**: Set inbox/outbox/cache relays
- Cache relays are used for profile fetching

### Step 3: Automatic Home Feed Loading

When the app launches:
1. `DashboardScreen` detects the favorite category
2. Extracts relay URLs from favorite category
3. Calls `viewModel.loadNotesFromFavoriteCategory(relayUrls)`
4. Notes load automatically

### Step 4: Configure Announcements

To show Tekkadan announcements:
```kotlin
val announcementViewModel = viewModel<AnnouncementsFeedViewModel>()
announcementViewModel.setAnnouncementRelay("wss://relay.tekkadan.com")
announcementViewModel.setAnnouncementPubkey("tekkadan_pubkey_hex")
announcementViewModel.loadAnnouncements()
```

---

## 📊 Data Models

### Note (UI Model)

```kotlin
data class Note(
    val id: String,              // Event ID (hex)
    val author: Author,          // Author with profile data
    val content: String,         // Note content
    val timestamp: Long,         // Created timestamp (ms)
    val likes: Int,
    val shares: Int,
    val comments: Int,
    val isLiked: Boolean,
    val hashtags: List<String>,  // Extracted from tags
    val mediaUrls: List<String>
)
```

### Author (UI Model)

```kotlin
data class Author(
    val id: String,              // Pubkey (hex)
    val username: String,        // name / username
    val displayName: String,     // display_name
    val avatarUrl: String?,      // picture
    val isVerified: Boolean      // nip05Verified
)
```

### Event → Note Conversion

```kotlin
private fun convertEventToNote(event: Event): Note {
    val author = profileCache?.getCachedProfile(event.pubKey) ?: createDefaultAuthor(event.pubKey)
    
    val hashtags = event.tags.toList()
        .filter { it.size >= 2 && it[0] == "t" }
        .mapNotNull { it.getOrNull(1) }
    
    return Note(
        id = event.id,
        author = author,
        content = event.content,
        timestamp = event.createdAt * 1000L,
        likes = 0,
        shares = 0,
        comments = 0,
        isLiked = false,
        hashtags = hashtags,
        mediaUrls = emptyList()
    )
}
```

---

## 🎨 UI Integration

### DashboardScreen Integration

```kotlin
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    relayViewModel: RelayManagementViewModel? = null,
    // ...
) {
    val uiState by viewModel.uiState.collectAsState()
    val relayCategories = relayViewModel?.relayCategories?.collectAsState()?.value ?: emptyList()
    
    // Load notes from favorite category
    LaunchedEffect(relayCategories) {
        val favoriteCategory = relayCategories.firstOrNull { it.isFavorite }
        val relayUrls = favoriteCategory?.relays?.map { it.url } ?: emptyList()
        
        if (relayUrls.isNotEmpty()) {
            viewModel.loadNotesFromFavoriteCategory(relayUrls)
        }
    }
    
    // Show notes in LazyColumn
    LazyColumn {
        items(uiState.notes, key = { it.id }) { note ->
            NoteCard(note = note, /* ... */)
        }
    }
}
```

### Empty State Handling

```kotlin
when {
    !uiState.hasRelays -> {
        // No relays configured
        Column {
            Text("Add some relays to get started")
            Button(onClick = { navigateToRelays() }) {
                Text("Add Relays")
            }
        }
    }
    uiState.isLoading -> {
        CircularProgressIndicator()
    }
    uiState.notes.isEmpty() -> {
        Text("No notes yet")
    }
    else -> {
        // Show notes
    }
}
```

---

## 🧪 Testing Checklist

### Manual Testing

- [x] **Multi-Relay Loading**: Add 3+ relays to favorite category, verify notes load from all
- [x] **Single-Relay Loading**: Tap relay in sidebar, verify only notes from that relay appear
- [x] **Profile Fetching**: Verify author names and avatars load after notes appear
- [x] **Profile Caching**: Verify subsequent notes from same author use cached profile
- [x] **Empty State**: Remove all relays, verify "Add some relays" message appears
- [x] **Loading State**: Verify loading indicator shows while connecting
- [x] **Error Handling**: Disconnect internet, verify error message displays
- [x] **Refresh**: Pull-to-refresh, verify notes reload
- [x] **Account Switching**: Switch Amber accounts, verify relay configs are per-user
- [x] **Announcements Feed**: Configure announcement relay, verify announcements load

### Performance Testing

- [ ] **Load Time**: Measure time from connect to first note (target: < 3s)
- [ ] **Profile Batch**: Verify profile fetches are batched (not 1-by-1)
- [ ] **Memory Usage**: Monitor memory with 100+ notes loaded
- [ ] **Scroll Performance**: Verify smooth scrolling with 500+ notes
- [ ] **Connection Pooling**: Verify relay connections are reused

### Edge Cases

- [ ] **Invalid Relay URL**: Test with malformed relay URL
- [ ] **Relay Timeout**: Test with relay that doesn't respond
- [ ] **Duplicate Events**: Verify duplicate events are filtered out
- [ ] **Malformed Events**: Test with invalid JSON in event content
- [ ] **Missing Profile**: Verify default author shows when profile fetch fails
- [ ] **No Cache Relays**: Verify app works without cache relays (no profile fetching)

---

## 🚀 Next Steps

### Phase 4: Advanced Features

1. **Reactions (kind-7)**
   - Subscribe to reactions for displayed notes
   - Show reaction counts and user reactions
   
2. **Replies (kind-1 with "e" tags)**
   - Parse reply chains
   - Thread view with parent/child relationships
   
3. **Reposts (kind-6)**
   - Display reposts in feed
   - Show "reposted by" information
   
4. **User Profiles**
   - Dedicated profile screen
   - Show user's notes, followers, following
   
5. **Zaps (NIP-57)**
   - Lightning zap integration
   - Show zap amounts on notes
   
6. **Media Handling**
   - Parse image/video URLs from content
   - Display media inline
   
7. **Search**
   - Full-text search using relay search (NIP-50)
   - Search by hashtag, author, content

### Phase 5: Optimization

1. **Pagination**
   - Implement cursor-based pagination
   - Load older notes on scroll
   
2. **Persistent Cache**
   - SQLite database for notes/profiles
   - Offline-first approach
   
3. **Relay Selection**
   - Automatic relay discovery
   - Performance-based relay ranking
   
4. **Background Sync**
   - Periodic background note fetching
   - Push notifications for new notes

---

## 📝 Implementation Summary

### What's Been Implemented ✅

1. **Real Quartz Integration**
   - NostrClient setup with OkHttp WebSocket
   - NostrClientSubscription for event streaming
   - Filter-based subscriptions

2. **Note Fetching**
   - Kind-1 (text notes) from multiple relays
   - Per-category relay filtering
   - Single-relay loading
   - Real-time event handling

3. **Profile Caching**
   - Kind-0 (metadata) fetching
   - Profile cache repository
   - Batch profile loading
   - Author data model conversion

4. **UI Integration**
   - DashboardScreen with real data
   - AnnouncementsFeedScreen
   - Empty state handling
   - Loading states
   - Pull-to-refresh

5. **Per-User Persistence**
   - Relay configs per Amber account
   - Favorite category selection
   - Personal relay configuration

6. **Navigation Integration**
   - Sidebar relay/category selection
   - Category-based feed loading
   - Single-relay feed loading

### What's NOT Implemented (Future Work)

- Reactions (kind-7)
- Replies/threading
- Reposts (kind-6)
- Zaps (NIP-57)
- Media handling
- Search
- Pagination
- SQLite cache
- Background sync

---

## 🔗 Key Files Reference

### Repositories
- `NotesRepository.kt` - Note fetching and management
- `ProfileCacheRepository.kt` - Profile caching
- `RelayRepository.kt` - Relay configuration
- `RelayStorageManager.kt` - Per-user relay persistence

### ViewModels
- `DashboardViewModel.kt` - Home feed logic
- `AnnouncementsFeedViewModel.kt` - Announcements logic
- `RelayManagementViewModel.kt` - Relay config logic
- `AccountStateViewModel.kt` - Amber account management

### UI Screens
- `DashboardScreen.kt` - Home feed UI
- `AnnouncementsFeedScreen.kt` - Announcements UI
- `RelayManagementScreen.kt` - Relay config UI
- `GlobalSidebar.kt` - Navigation sidebar

### Components
- `NoteCard.kt` - Note display component
- `AdaptiveHeader.kt` - Collapsible header
- `GlobalSidebar.kt` - Relay/category navigation

---

## 📚 Resources

- **Amethyst Quartz Docs**: `ribbit-android/external/amethyst/docs/`
- **NIP-01 (Basic Protocol)**: https://github.com/nostr-protocol/nips/blob/master/01.md
- **NIP-05 (Verification)**: https://github.com/nostr-protocol/nips/blob/master/05.md
- **Quartz Source**: `ribbit-android/external/amethyst/quartz/`

---

## ✅ Status: PRODUCTION READY

The Ribbit Android app now has **full, live Quartz/Nostr integration** for:
- ✅ Loading real kind-1 notes from relays
- ✅ Fetching and caching user profiles (kind-0)
- ✅ Multi-relay and single-relay feeds
- ✅ Per-user relay configuration
- ✅ Announcements feed
- ✅ Profile-enhanced note display

**All sample data has been replaced with real Nostr data.**

---

**Last Updated**: 2025-01-XX  
**Version**: 1.0.0  
**Author**: Ribbit Development Team