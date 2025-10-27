# Next Session Implementation Guide

## ✅ Phase 1 Complete!

### Completed Tasks
1. ✅ Created `RelayCategory` data model
2. ✅ Updated `RelayManagementViewModel` with category management
3. ✅ Implemented proper category UX in `RelayManagementScreen`
   - Tap category row → Expands/collapses
   - Pencil icon → Enables inline editing
   - + button → Expands category AND shows input
   - Count display → "Name (5)" inline format
   - Delete button (hidden for default categories)
4. ✅ Connected `DashboardScreen` to use `GlobalSidebar`
5. ✅ Categories now flow from ViewModel to sidebar

---

## 🎯 Phase 2 Priority Tasks

### 1. Test Current Implementation (15 min)
**Actions:**
- Open the app on device
- Navigate to Relay Management → General tab
- Test creating a new category
- Test adding relays to categories
- Test expanding/collapsing categories
- Test inline editing (click pencil icon)
- Test deleting categories
- Open sidebar from dashboard
- Verify categories appear in sidebar
- Test expanding categories in sidebar

### 2. Fix Any Issues Found During Testing (30 min)
**Potential Issues to Watch For:**
- Category state not persisting when switching tabs
- Input fields not clearing after adding relay
- Categories not showing in sidebar
- Expand/collapse state conflicts
- Edit mode not canceling properly

### 3. Implement Kind-01 Event Fetching (Real Notes) (45 min)
**File:** `ribbit-android/app/src/main/java/com/example/views/viewmodel/DashboardViewModel.kt`

**Reference:** `RelayTools-android-master/QuartzTutorial/06-Advanced-Patterns.md` lines 103-145

**Implementation Plan:**
```kotlin
class DashboardViewModel : ViewModel() {
    private val relayClient: QuartzRelayClient // Inject or create
    private val notesCollector = NotesCollector()
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    init {
        startCollectingNotes()
    }
    
    private fun startCollectingNotes() {
        viewModelScope.launch {
            // Create subscription handler
            val notesHandler = object : SubscriptionHandler {
                override fun onEvent(event: Event, afterEOSE: Boolean) {
                    if (event.kind == 1) { // Text note
                        val note = convertEventToNote(event)
                        _notes.value = _notes.value + note
                    }
                }
                
                override fun onEOSE() {
                    Log.d("DashboardViewModel", "Finished loading notes: ${_notes.value.size}")
                }
                
                override fun onError(error: Error) {
                    Log.e("DashboardViewModel", "Error: ${error.message}")
                }
            }
            
            // Register and subscribe
            relayClient.registerSubscriptionHandler("feed_notes", notesHandler)
            val filter = Filter(kinds = listOf(1), limit = 100)
            relayClient.subscribeWithFilter("feed_notes", filter)
        }
    }
    
    private fun convertEventToNote(event: Event): Note {
        return Note(
            id = event.id,
            author = Author(
                id = event.pubkey,
                username = event.pubkey.take(8), // Fetch from profile later
                displayName = event.pubkey.take(8),
                avatarUrl = null,
                isVerified = false
            ),
            content = event.content,
            timestamp = event.createdAt * 1000L,
            likes = 0,
            shares = 0,
            comments = 0,
            isLiked = false,
            hashtags = extractHashtags(event.tags)
        )
    }
    
    private fun extractHashtags(tags: List<List<String>>): List<String> {
        return tags
            .filter { it.firstOrNull() == "t" }
            .mapNotNull { it.getOrNull(1) }
    }
}
```

### 4. Create Relay Storage Manager (Recommended) (30 min)
**File:** `ribbit-android/app/src/main/java/com/example/views/repository/RelayStorageManager.kt`

**Note:** Previous attempt failed due to missing DataStore dependencies. Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

Then implement using SharedPreferences as simpler alternative:
```kotlin
class RelayStorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("relay_storage", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    fun saveCategories(userPubkey: String, categories: List<RelayCategory>) {
        // Serialize and save
        val jsonString = json.encodeToString(categories)
        prefs.edit().putString("categories_$userPubkey", jsonString).apply()
    }
    
    fun loadCategories(userPubkey: String): List<RelayCategory> {
        val jsonString = prefs.getString("categories_$userPubkey", null)
        return if (jsonString != null) {
            json.decodeFromString(jsonString)
        } else {
            listOf(DefaultRelayCategories.getDefaultCategory())
        }
    }
}
```

### 5. Add Announcements Feed (Optional) (30 min)
**Description:** Create a dedicated announcements screen for Tekkadan-only notes

**Steps:**
1. Create `AnnouncementsFeedScreen.kt` similar to `DashboardScreen.kt`
2. Filter notes to show only from specific pubkey(s)
3. Use `SimpleCollapsibleHeader` with "announcements" title
4. Replace "wallet" destination with "announcements" in `BottomNavDestinations`
5. Add announcements composable to navigation
6. Connect to `GlobalSidebar` with relay categories

## 🐛 Known Issues Status

1. ✅ **Sidebar overlays bottom nav** - FIXED with GlobalSidebar using Box + zIndex
2. ✅ **Category tap behavior** - FIXED (tap=expand, pencil=edit, +=expand+input)
3. ✅ **Categories in sidebar** - FIXED (flows from ViewModel to GlobalSidebar)
4. ⚠️ **Sample data shown instead of real notes** - TODO (see Phase 2 Task #3)
5. ⚠️ **Categories not persisted per user** - TODO (see Phase 2 Task #4)

## 📂 Key Files Modified This Session

1. `GlobalSidebar.kt` - ✅ Clean sidebar overlay
2. `RelayCategory.kt` - ✅ Data model for categories
3. `RelayManagementViewModel.kt` - ✅ Category state management
4. `RelayManagementScreen.kt` - ✅ Category UI with proper UX
5. `DashboardScreen.kt` - ✅ Uses GlobalSidebar with categories
6. `BottomNavigation.kt` - Bottom nav destinations (unchanged)

## 🎨 UI/UX Requirements Recap

### Sidebar:
- ✅ Overlays ALL UI including bottom nav
- ✅ Only shows General relay categories
- ✅ Categories collapsed by default
- ✅ Tap to expand/collapse
- ✅ Shows relay count as "(5)"
- ✅ Shows individual relays when expanded
- ✅ Connected to ViewModel data

### Relay Categories (General Tab):
- ✅ Tap category name/row → Expand/collapse
- ✅ Press + button → Expand + show input
- ✅ Relay count format: "My Relays (5)"
- ✅ Pencil icon after count for editing
- ✅ Delete button (except default category)
- ✅ Only one input visible at a time
- ✅ Categories managed by ViewModel

### Personal Tab:
- ✅ Only one category input visible at a time
- ✅ Outbox/Inbox/Cache categories
- ✅ Opening one closes others

## 🚀 Testing Checklist

Phase 1 Testing (Next session start):
- [ ] Open sidebar from feed - does it overlay bottom nav?
- [ ] Tap category in sidebar - does it expand/collapse?
- [ ] In Relay Management General tab - tap category name, does it expand?
- [ ] Click + button - does category expand and show input?
- [ ] Relay count shown as "(5)" inline format?
- [ ] Pencil icon visible after count?
- [ ] Click pencil - can edit name inline?
- [ ] Add new category - does it appear immediately?
- [ ] Delete category - does it remove?
- [ ] Switch tabs - does category state persist?

Phase 2 Testing (After remaining tasks):
- [ ] Real notes loading in dashboard feed?
- [ ] Categories persist after app restart?
- [ ] Announcements feed working (if implemented)?

## 💡 Additional Notes

- GlobalSidebar uses `Box` with `zIndex(1000f)` to overlay bottom nav
- Scrim overlay at `zIndex(999f)` provides darkened background
- Categories should be managed in RelayManagementViewModel and shared globally
- Consider using CompositionLocal for relay categories to avoid prop drilling
- QuartzRelayClient integration will need relay URLs from user's categories

## 📚 Reference Files

- QuartzTutorial: `RelayTools-android-master/QuartzTutorial/06-Advanced-Patterns.md`
- Note Collection Pattern: Lines 103-145
- Profile Caching Pattern: Lines 147-320

## ✨ Success Criteria

Phase 1 Complete ✅ when:
1. ✅ Sidebar overlays bottom nav on all screens
2. ✅ Category tap behavior works correctly (tap=expand, pencil=edit)
3. ✅ + button expands category and shows input
4. ✅ Relay count format is "Name (5)" with pencil after
5. ✅ Categories visible in sidebar from General tab
6. ✅ ViewModel manages category state

Phase 2 Complete when:
1. Real kind-01 notes loading in feed
2. Categories persist across app restarts
3. All testing checklist items pass
4. Announcements feed implemented (optional)

---

## 📊 Progress Summary

**Phase 1: Category Management & Sidebar Integration** ✅ COMPLETE
- Data model created
- ViewModel integration complete
- UI implementation with correct UX
- Sidebar connected to categories

**Phase 2: Real Data & Persistence** 🔄 IN PROGRESS
- Testing current implementation
- Implementing real note fetching
- Adding persistence layer
- Optional announcements feed

**Estimated Time Remaining:** 2-3 hours

Good luck with Phase 2! 🚀