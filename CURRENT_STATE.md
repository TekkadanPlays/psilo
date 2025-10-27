# Ribbit Android - Current State & Architecture

## 📱 App Status: FUNCTIONAL & DEPLOYED
**Last Build:** Successful
**Device:** Motorola Razr 2023 - Android 15
**Status:** Installed and running

---

## 🏗️ Architecture Overview

### Navigation Structure
```
┌─────────────────────────────────────────┐
│  GlobalSidebar (Overlay - zIndex 1000)  │ ← Only relay categories
├─────────────────────────────────────────┤
│  TopAppBar (Collapsible, Shared State)  │
├─────────────────────────────────────────┤
│                                         │
│  Main Content (Feed/Thread/Profile)     │
│                                         │
├─────────────────────────────────────────┤
│  Bottom Nav (5 icons, Collapsible)     │ ← Shared across all main screens
└─────────────────────────────────────────┘
```

### Bottom Navigation (Left to Right)
1. 🏠 **Home** - Dashboard feed (currently sample data)
2. ✉️ **Messages** - Dead button (placeholder)
3. 🌐 **Relays** - Relay management with categories
4. 📣 **Announcements** - Tekkadan-only feed
5. 🔔 **Notifications** - Notification view with filtering

### Main Screens
- **Dashboard** - Home feed with collapsible header
- **Announcements** - Secondary feed for Tekkadan announcements
- **Thread View** - Note detail with comments
- **Profile View** - User profile with notes
- **Relay Management** - Two tabs (General + Personal)
- **Notifications** - Categorized notification view

---

## 🎨 Key Components

### GlobalSidebar (`GlobalSidebar.kt`)
- **Location:** `app/src/main/java/com/example/views/ui/components/`
- **Purpose:** Global sidebar showing only General relay categories
- **Features:**
  - Overlays ALL UI including bottom nav (Box + zIndex)
  - Categories collapsed by default
  - Tap to expand/collapse
  - Shows relay count in format "Name (5)"
  - Shows individual relays when expanded
- **Status:** ✅ Created, needs category data integration

### Collapsible Headers
- **Shared State:** `TopAppBarState` passed globally
- **Behavior:** Scroll up = collapse header + bottom nav together
- **State Persistence:** Collapse state maintained across navigation
- **Used In:** Dashboard, Announcements, Thread, Profile, Relays, Notifications

### Relay Management
- **General Tab:**
  - User-defined categories
  - Add/edit/delete categories
  - Add/remove relays within categories
  - Default "My Relays" category
  - ⚠️ Needs UX fixes (see NEXT_SESSION_TASKS.md)
  
- **Personal Tab:**
  - Outbox Relays (for publishing)
  - Inbox Relays (for receiving)
  - Cache Relays (for caching/backup)
  - Only one input visible at a time ✅

---

## 📊 Data Flow

### Current (Sample Data)
```
SampleData.kt → DashboardViewModel → DashboardScreen → NoteCard
```

### Target (Real Notes)
```
QuartzRelayClient → SubscriptionHandler → Event (kind-01)
                 ↓
            DashboardViewModel → Note
                 ↓
            DashboardScreen → NoteCard
```

### Relay Categories
```
RelayManagementScreen → relayCategories (State)
                     ↓
        [Need to share globally]
                     ↓
                GlobalSidebar
```

---

## 🔧 Technical Details

### Dependencies
- **Compose:** Material3 with Material Motion
- **Navigation:** Jetpack Navigation Compose
- **Coroutines:** Flow + StateFlow for reactive UI
- **Relay Client:** Quartz (from RelayTools-android-master)
- **Storage:** SharedPreferences (DataStore attempted but failed)

### Transition Animations
- **Bottom Nav Navigation:** MaterialFadeThrough (fade in/out)
- **Detail Navigation:** Shared X-axis (slide from right)
  - Forward: Slide in from right, previous scales down
  - Back: Slide out to right, previous scales up
- **Duration:** 300ms with MaterialMotion easing

### State Management
- **ViewModels:** DashboardViewModel, RelayManagementViewModel, AccountStateViewModel
- **Shared State:** TopAppBarState for collapsible behavior
- **Local State:** Remember + MutableState for UI interactions

---

## 🐛 Known Issues (Priority Order)

### HIGH PRIORITY (COMPLETED ✅)
1. ✅ **Relay Category UX** - FIXED
   - Tap category row → Expands/collapses
   - Pencil icon → Enables inline editing
   - + button → Expands category AND shows input
   - Count display → "My Relays (5)" inline format

2. ✅ **Categories in Sidebar** - FIXED
   - RelayManagementViewModel now manages categories
   - Categories exposed via StateFlow
   - DashboardScreen uses GlobalSidebar with categories
   - Data flows from ViewModel to sidebar

### MEDIUM PRIORITY
3. **Sample Data in Feed** - Not loading real notes
   - Need to integrate QuartzRelayClient
   - Implement kind-01 event subscription
   - Convert Event → Note

4. **No Persistence** - Categories lost on restart
   - Need storage implementation
   - SharedPreferences or DataStore

### LOW PRIORITY
5. **Deprecated icon warnings** - Use AutoMirrored versions</parameter>

---

## 📁 Key File Locations

### Navigation & Routing
- `app/src/main/java/com/example/views/ui/navigation/RibbitNavigation.kt`

### Screens
- `app/src/main/java/com/example/views/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/views/ui/screens/AnnouncementsFeedScreen.kt`
- `app/src/main/java/com/example/views/ui/screens/ModernThreadViewScreen.kt`
- `app/src/main/java/com/example/views/ui/screens/ProfileScreen.kt`
- `app/src/main/java/com/example/views/ui/screens/RelayManagementScreen.kt`
- `app/src/main/java/com/example/views/ui/screens/NotificationsScreen.kt`

### Components
- `app/src/main/java/com/example/views/ui/components/GlobalSidebar.kt` ← NEW
- `app/src/main/java/com/example/views/ui/components/AdaptiveHeader.kt`
- `app/src/main/java/com/example/views/ui/components/SimpleCollapsibleHeader.kt`
- `app/src/main/java/com/example/views/ui/components/ScrollAwareBottomNavigation.kt`
- `app/src/main/java/com/example/views/ui/components/NoteCard.kt`

### Data Models
- `app/src/main/java/com/example/views/data/RelayCategory.kt` ← NEW
- `app/src/main/java/com/example/views/data/Note.kt`
- `app/src/main/java/com/example/views/data/Author.kt`
- `app/src/main/java/com/example/views/data/SampleData.kt`

### ViewModels
- `app/src/main/java/com/example/views/viewmodel/DashboardViewModel.kt`
- `app/src/main/java/com/example/views/viewmodel/RelayManagementViewModel.kt`
- `app/src/main/java/com/example/views/viewmodel/AccountStateViewModel.kt`

---

## 🎯 Next Session Goals

1. Fix relay category UX (tap to expand, pencil to edit)
2. Connect categories to sidebar
3. Implement kind-01 event fetching for real notes
4. Add category persistence (optional but recommended)

**See NEXT_SESSION_TASKS.md for detailed implementation guide**

---

## 🚀 Build & Deploy Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Check connected devices
adb devices

# Full clean + install
./gradlew clean installDebug
```

---

## 📚 External References

### Quartz Tutorial (Relay Integration)
- **Location:** `RelayTools-android-master/QuartzTutorial/`
- **Key File:** `06-Advanced-Patterns.md`
- **Relevant Sections:**
  - Lines 103-145: Notes Collection Pattern
  - Lines 147-320: Profile Caching Pattern

### Material Motion Guidelines
- Transitions follow Material Design 3 motion system
- Easing curves: EasingStandardDecelerate/Accelerate
- Duration: 300ms for navigation, 200ms for UI elements

---

## ✨ Recent Achievements

### Session 1 (Previous)
- ✅ Global collapsible navigation (header + bottom nav)
- ✅ Clean sidebar overlay (above all UI)
- ✅ Proper transition animations
- ✅ Double-tap back to exit with toast
- ✅ Scroll-to-top on home button tap
- ✅ User avatar menu in all headers
- ✅ Bottom nav visible on thread/profile screens
- ✅ Thread view with collapsible header
- ✅ Profile view with collapsible header

### Session 2 (Current - Phase 1 Complete)
- ✅ Created RelayCategory data model
- ✅ Added category management to RelayManagementViewModel
- ✅ Implemented proper category UX (tap to expand, pencil to edit)
- ✅ Connected DashboardScreen to use GlobalSidebar
- ✅ Categories now flow from ViewModel to sidebar
- ✅ Relay count displays as "(N)" inline format
- ✅ + button expands category and shows input
- ✅ Only one input visible at a time per category
- ✅ Delete button (hidden for default categories)

---

**Last Updated:** Session 2 (Phase 1 Complete)
**App Version:** Development build
**Target SDK:** 34 (Android 14)
**Min SDK:** 26 (Android 8.0)
**Build Status:** ✅ Successful - Installed on device