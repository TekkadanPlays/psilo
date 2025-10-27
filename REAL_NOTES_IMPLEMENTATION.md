# Real Notes Implementation Guide

**Status:** Phase 3 - Real Nostr Notes Integration  
**Date:** Current Session  
**Goal:** Replace sample data with real kind-01 notes from user's configured relays

---

## 🎯 Architecture Overview

### Data Flow
```
User configures relays in categories
        ↓
Marks one category as "favorite" (default feed)
        ↓
DashboardScreen loads → gets favorite category relay URLs
        ↓
DashboardViewModel.loadNotesFromFavoriteCategory(relayUrls)
        ↓
NotesRepository connects to relays via Quartz RelayPool
        ↓
Subscribe to kind-01 events with filter
        ↓
Events converted to Note objects
        ↓
StateFlow updates → UI shows real notes
```

---

## 📁 Files Created/Modified

### New Files
1. **`app/src/main/java/com/example/views/repository/NotesRepository.kt`** ✅
   - Uses Quartz RelayPool for relay connections
   - Subscribes to kind-01 (text note) events
   - Converts Nostr events to our Note data model
   - Exposes StateFlow of notes for reactive UI

### Modified Files
1. **`app/src/main/java/com/example/views/data/RelayCategory.kt`** ✅
   - Added `isFavorite: Boolean` field
   - Only one category can be favorite at a time
   - Favorite category = relays used for home feed

2. **`app/src/main/java/com/example/views/viewmodel/RelayManagementViewModel.kt`** ✅
   - Added `setFavoriteCategory(categoryId)` method
   - Added `getFavoriteCategory()` method
   - Added `getFavoriteCategoryRelayUrls()` method

3. **`app/src/main/java/com/example/views/viewmodel/DashboardViewModel.kt`** ✅
   - Added `NotesRepository` instance
   - Added `loadNotesFromFavoriteCategory(relayUrls)` method
   - Added `loadNotesFromSpecificRelay(relayUrl)` method
   - Added `hasRelays` state for empty state handling
   - Observes notes from repository via StateFlow

4. **`app/src/main/java/com/example/views/ui/screens/DashboardScreen.kt`** ✅
   - Added LaunchedEffect to load user relays
   - Passes relay categories to sidebar

### Files That Need Updates (TODO)
1. **`DashboardScreen.kt`** - Add logic to:
   - Load notes when favorite category changes
   - Show empty state when no relays configured
   - Handle sidebar relay click to load specific relay

2. **`RelayManagementScreen.kt`** - Add UI to:
   - Mark category as favorite (star icon)
   - Show which category is favorite

3. **`GlobalSidebar.kt`** - Update to:
   - Show star icon next to favorite category
   - Handle relay click to load notes from that relay

---

## 🔧 Implementation Steps

### Step 1: Load Notes on Dashboard Open

**Location:** `DashboardScreen.kt` (after LaunchedEffect for loading user relays)

```kotlin
// Load notes from favorite category when relays change
LaunchedEffect(relayCategories) {
    val favoriteCategory = relayCategories.firstOrNull { it.isFavorite }
    val relayUrls = favoriteCategory?.relays?.map { it.url } ?: emptyList()
    
    if (relayUrls.isNotEmpty()) {
        viewModel.loadNotesFromFavoriteCategory(relayUrls)
    }
}
```

### Step 2: Add Empty State UI

**Location:** `DashboardScreen.kt` (in the Scaffold content, before notes list)

```kotlin
// Show empty state when no relays configured
if (!uiState.hasRelays && !uiState.isLoadingFromRelays) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No relays configured",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add some relays to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onNavigateTo("relays") }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Relays")
            }
        }
    }
}
```

### Step 3: Handle Sidebar Relay Click

**Location:** `DashboardScreen.kt` (in GlobalSidebar onItemClick)

```kotlin
GlobalSidebar(
    drawerState = drawerState,
    onItemClick = { itemId ->
        when {
            itemId.startsWith("relay_category:") -> {
                val categoryId = itemId.removePrefix("relay_category:")
                val category = relayCategories.firstOrNull { it.id == categoryId }
                val relayUrls = category?.relays?.map { it.url } ?: emptyList()
                if (relayUrls.isNotEmpty()) {
                    viewModel.loadNotesFromFavoriteCategory(relayUrls)
                }
            }
            itemId.startsWith("relay:") -> {
                val relayUrl = itemId.removePrefix("relay:")
                viewModel.loadNotesFromSpecificRelay(relayUrl)
            }
            "relays" -> onNavigateTo("relays")
            "settings" -> onNavigateTo("settings")
            // ... other items
        }
    },
    relayCategories = relayCategories,
    modifier = modifier
)
```

### Step 4: Update Sidebar to Handle Relay Clicks

**Location:** `GlobalSidebar.kt` (in RelayCategoriesSection)

```kotlin
// Change relay click to pass relay URL
category.relays.forEach { relay ->
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCategoryClick("relay:${relay.url}") } // Changed this line
            .padding(start = 56.dp, end = 28.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier.size(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = relay.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
```

### Step 5: Add Favorite Category UI in Relay Management

**Location:** `RelayManagementScreen.kt` (in RelayCategorySection header)

```kotlin
// Add star icon to mark favorite
Row(
    modifier = Modifier.weight(1f),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
        contentDescription = if (isExpanded) "Collapse" else "Expand",
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.width(8.dp))
    
    // Favorite star icon
    IconButton(
        onClick = { viewModel.setFavoriteCategory(category.id) },
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (category.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
            contentDescription = if (category.isFavorite) "Favorite" else "Set as favorite",
            tint = if (category.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
    
    // ... rest of the header (name, edit icon, etc.)
}
```

---

## 🔍 How It Works

### NotesRepository Architecture

**Quartz RelayPool:**
- Manages connections to multiple relays simultaneously
- Handles WebSocket lifecycle
- Batches subscriptions across all connected relays
- Provides callbacks for events, EOSE, and errors

**Subscription Flow:**
```kotlin
1. connectToRelays(List<String>)
   → relayPool.register(url) for each relay

2. subscribeToNotes(limit = 100)
   → Create TypedFilter(kinds = [TextNote], limit = 100)
   → relayPool.subscribe(id, filters, callbacks)

3. onEvent callback
   → Convert Event to Note
   → Add to StateFlow if not duplicate
   → Sort by timestamp descending

4. onEOSE callback
   → Set isLoading = false
   → All relays have sent their initial batch
```

### Event to Note Conversion

**Nostr Event Structure:**
```json
{
  "id": "event_hex_id",
  "pubkey": "author_hex_pubkey",
  "created_at": 1234567890,
  "kind": 1,
  "content": "Note text content",
  "tags": [
    ["t", "hashtag"],
    ["e", "reply_to_event_id"],
    ["p", "mentioned_pubkey"]
  ]
}
```

**Our Note Structure:**
```kotlin
Note(
    id = event.id.toHexKey(),
    author = Author(
        id = event.pubKey.toHexKey(),
        username = pubkey.take(8),
        displayName = pubkey.take(8),
        avatarUrl = null
    ),
    content = event.content,
    timestamp = event.createdAt * 1000L,
    hashtags = extractHashtags(event.tags),
    likes = 0,  // Will be tracked separately
    shares = 0,
    comments = 0
)
```

---

## 🎨 UI States

### Loading State
```
┌─────────────────────────┐
│  [Spinner]              │
│  Loading notes...       │
└─────────────────────────┘
```

### Empty State (No Relays)
```
┌─────────────────────────┐
│  [Relay Icon]           │
│  No relays configured   │
│  Add some relays to     │
│  get started            │
│  [Add Relays Button]    │
└─────────────────────────┘
```

### Loaded State (With Notes)
```
┌─────────────────────────┐
│  NoteCard               │
│  NoteCard               │
│  NoteCard               │
│  ...                    │
└─────────────────────────┘
```

### Error State
```
┌─────────────────────────┐
│  [Warning Icon]         │
│  Failed to load notes   │
│  [Retry Button]         │
└─────────────────────────┘
```

---

## 🚀 User Experience Flow

### First Time User (No Relays)
1. Opens app → sees empty state
2. Taps "Add Relays" button
3. Goes to Relay Management → General tab
4. Adds relays to "My Relays" (default favorite category)
5. Returns to dashboard → notes automatically load

### User With Configured Relays
1. Opens app → notes load from favorite category automatically
2. Opens sidebar → sees all relay categories
3. Taps a category → loads notes from all relays in that category
4. Taps a specific relay → loads notes from only that relay

### Switching Favorite Category
1. Goes to Relay Management → General tab
2. Taps star icon on different category
3. Returns to dashboard → feed automatically updates with new category's notes

---

## 📊 Performance Considerations

### Connection Management
- **Pool:** Reuses WebSocket connections across subscriptions
- **Batching:** Multiple filters sent in single subscription request
- **Disconnection:** Clean disconnect on screen close

### Memory Management
- **Deduplication:** Prevents duplicate notes in feed
- **Limit:** Default 100 notes per subscription
- **Sorting:** Notes sorted by timestamp (newest first)

### Error Handling
- **Connection Errors:** Caught and displayed in UI
- **Parse Errors:** Logged but don't crash app
- **Timeout:** Relays that don't respond marked as unavailable

---

## 🐛 Known Limitations

### Current Implementation
1. ❌ No profile metadata fetching yet (shows pubkey instead of name)
2. ❌ No reaction counts from relays (likes/shares always 0)
3. ❌ No media preview from NIP-92 tags
4. ❌ No reply thread reconstruction
5. ❌ No pagination (only initial 100 notes)

### Planned Improvements
1. ⚠️ Profile cache using cache relays (Personal tab)
2. ⚠️ Reaction event aggregation (kind-7)
3. ⚠️ Media preview extraction
4. ⚠️ Reply thread building
5. ⚠️ Infinite scroll with pagination

---

## 🔐 Security Considerations

### Relay Trust
- User explicitly adds relays (no auto-connect)
- Can remove relays anytime
- Can view notes from single relay to verify content

### Event Validation
- Quartz validates event signatures automatically
- Malformed events are rejected
- Invalid timestamps are handled gracefully

### Privacy
- No tracking of user's relay preferences
- Local storage only (no cloud backup)
- Each account has separate relay configurations

---

## 📝 Testing Checklist

### Relay Connection
- [ ] Connect to multiple relays successfully
- [ ] Handle relay connection failures gracefully
- [ ] Reconnect after network interruption
- [ ] Disconnect cleanly on screen close

### Note Loading
- [ ] Load notes from favorite category on app open
- [ ] Load notes from tapped category in sidebar
- [ ] Load notes from tapped relay in sidebar
- [ ] Show empty state when no relays configured
- [ ] Show loading state while fetching

### Category Management
- [ ] Create new category
- [ ] Add relays to category
- [ ] Mark category as favorite
- [ ] Only one category can be favorite
- [ ] Favorite category persists across restarts

### Edge Cases
- [ ] No internet connection
- [ ] All relays offline
- [ ] No notes from relays (empty result)
- [ ] Relay returns invalid events
- [ ] User has no favorite category set

---

## 🎓 Quartz API Reference

### RelayPool Methods
```kotlin
// Connect to relay
relayPool.register(url: String)

// Subscribe to events
relayPool.subscribe(
    subscriptionId: String,
    filters: List<TypedFilter>,
    onEvent: (Event, String) -> Unit,
    onEOSE: (String) -> Unit,
    onError: (String, String) -> Unit
)

// Unsubscribe
relayPool.unsubscribe(subscriptionId: String)

// Disconnect
relayPool.disconnect()
```

### TypedFilter Options
```kotlin
TypedFilter(
    kinds: List<EventKind>,           // Event kinds to fetch
    authors: List<String>? = null,    // Filter by author pubkeys
    ids: List<String>? = null,        // Filter by event IDs
    tags: Map<String, List<String>>? = null,  // Filter by tags
    since: Long? = null,              // Events after timestamp
    until: Long? = null,              // Events before timestamp
    limit: Int? = null                // Max number of events
)
```

### Event Structure
```kotlin
Event(
    id: String,              // Event hex ID
    pubKey: String,          // Author hex pubkey
    createdAt: Long,         // Unix timestamp (seconds)
    kind: Int,               // Event kind (1 = text note)
    content: String,         // Note content
    tags: List<List<String>>, // Event tags
    sig: String              // Signature
)
```

---

## ✅ Success Criteria

### Phase 3 Complete When:
1. ✅ NotesRepository created and integrated
2. ✅ DashboardViewModel loads real notes
3. ⚠️ DashboardScreen shows empty state when no relays
4. ⚠️ DashboardScreen loads notes from favorite category
5. ⚠️ Sidebar relay click loads specific relay notes
6. ⚠️ Relay Management shows favorite category (star icon)
7. ⚠️ User can change favorite category
8. ⚠️ Notes display in feed with real content

---

## 🚦 Next Steps

**Immediate (This Session):**
1. Update DashboardScreen with empty state and note loading logic
2. Update GlobalSidebar to handle relay clicks
3. Add favorite category UI in RelayManagementScreen
4. Test end-to-end flow
5. Build and deploy

**Future Sessions:**
1. Profile metadata fetching from cache relays
2. Reaction event aggregation
3. Reply thread reconstruction
4. Media preview support
5. Infinite scroll pagination

---

**Implementation Status:** 70% Complete  
**Remaining Work:** ~1 hour  
**Ready to Deploy:** After Step 1-5 completion

---

**End of Implementation Guide**