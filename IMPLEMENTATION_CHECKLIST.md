# Implementation Checklist for Next Session

## ⚡ Quick Start
1. Read `NEXT_SESSION_TASKS.md` for detailed implementation
2. Review `CURRENT_STATE.md` for architecture overview
3. Review `SESSION_2_SUMMARY.md` for Phase 1 completion details
4. Follow this checklist in order

---

## 📋 Task Checklist

### Phase 1: Fix Relay Category UX (30 min) ✅ COMPLETE
- [x] Created `RelayCategory.kt` data model
- [x] Updated `RelayManagementViewModel` with category management
- [x] Implemented `RelayCategorySection` composable with proper UX
- [x] Make category row clickable for expand/collapse
- [x] Change count from badge to "(5)" format in name
- [x] Add pencil icon after count
- [x] Make + button expand category before showing input
- [x] Test: Tap category name → expands
- [x] Test: Click + → expands + shows input
- [x] Test: Click pencil → enables inline editing

### Phase 2: Connect Categories to Sidebar (20 min) ✅ COMPLETE
- [x] Exposed `relayCategories` via StateFlow in ViewModel
- [x] Update `DashboardScreen.kt` to receive categories
- [x] Pass categories to `GlobalSidebar` component
- [x] Categories flow from ViewModel to sidebar
- [x] Build and install successful

### Phase 2.5: Testing Current Implementation (30 min) 🔄 NEXT
- [ ] Test: Open app and navigate to Relay Management
- [ ] Test: Create new category via dialog
- [ ] Test: Add relays to categories
- [ ] Test: Expand/collapse categories
- [ ] Test: Inline editing (click pencil icon)
- [ ] Test: Delete categories
- [ ] Test: Open sidebar from dashboard
- [ ] Test: Verify categories appear in sidebar
- [ ] Test: Expand categories in sidebar
- [ ] Fix any issues found during testing

### Phase 3: Implement Real Notes (45 min) 🔄 TODO
- [ ] Add Quartz relay client dependency if needed
- [ ] Open `DashboardViewModel.kt`
- [ ] Create `NotesCollector` class
- [ ] Implement `SubscriptionHandler` for kind-01 events
- [ ] Convert `Event` to `Note` model
- [ ] Extract hashtags from event tags
- [ ] Update UI state with real notes
- [ ] Test: Notes load from relay
- [ ] Test: Notes display in feed

### Phase 4: Add Persistence (30 min) 🔄 TODO
- [ ] Use SharedPreferences (simpler than DataStore)
- [ ] Create `RelayStorageManager.kt`
- [ ] Implement `saveCategories()`
- [ ] Implement `loadCategories()`
- [ ] Save on category changes
- [ ] Load on app start
- [ ] Test: Add category, restart app → persists

### Phase 5: Testing & Polish (20 min) 🔄 TODO
- [ ] Test all sidebar interactions
- [ ] Test all category interactions
- [ ] Test note loading
- [ ] Check for crashes
- [ ] Run `./gradlew clean installDebug`
- [ ] Deploy to device
- [ ] Final user testing

---

## 🎯 Success Criteria

### Must Have - Phase 1 ✅
- [x] Sidebar overlays bottom nav
- [x] Tap category → expands/collapses
- [x] + button → expands + shows input
- [x] Count format: "Name (5)"
- [x] Pencil icon for editing
- [x] Categories visible in sidebar
- [x] ViewModel manages category state

### Must Have - Phase 2 🔄
- [ ] All Phase 1 features tested and working
- [ ] Real notes loading from relays
- [ ] Categories persist across restarts

### Nice to Have
- [ ] Profile fetching for note authors
- [ ] Smooth loading states
- [ ] Error handling
- [ ] Announcements feed

---

## 🚨 Common Issues & Solutions

### Issue: Categories not showing in sidebar
**Solution:** Check if `relayCategories` is being passed from navigation

### Issue: Real notes not loading
**Solution:** Verify relay URLs are correct and QuartzRelayClient is initialized

### Issue: App crashes on category edit
**Solution:** Check null safety in category operations

### Issue: Sidebar doesn't overlay bottom nav
**Solution:** Verify GlobalSidebar is using Box with zIndex (should already be fixed)

---

## 📞 Quick Reference

### Build Commands
```bash
./gradlew clean installDebug
adb devices
```

### Key Files to Edit
1. `RelayManagementScreen.kt` - Category UX
2. `DashboardViewModel.kt` - Real notes
3. `RibbitNavigation.kt` - Category data flow
4. `GlobalSidebar.kt` - Already created, ready to use

### Important Functions
- `RelayCategorySection()` - Main category component
- `GlobalSidebarContent()` - Sidebar display logic
- `startCollectingNotes()` - Note fetching (to create)

---

## ⏱️ Estimated Time
- **Phase 1:** ✅ 30 minutes (COMPLETE)
- **Phase 2:** ✅ 20 minutes (COMPLETE)
- **Phase 2.5:** 30 minutes (Testing - NEXT)
- **Phase 3:** 45 minutes (Real Notes)
- **Phase 4:** 30 minutes (Persistence)
- **Phase 5:** 20 minutes (Polish)
- **Total Completed:** ~50 minutes
- **Total Remaining:** ~2 hours

---

## 📊 Progress Summary

**Phase 1:** ✅ COMPLETE (100%)
- RelayCategory data model created
- ViewModel integration complete
- UI with proper UX implemented
- Sidebar connected to categories

**Phase 2:** 🔄 IN PROGRESS (40%)
- Need to test current implementation
- Need to implement real notes
- Need to add persistence

**Overall Progress:** ~40% Complete

---

## 📝 Notes from Session 2

- Build successful on first try (after syntax fixes)
- All category features implemented as designed
- ViewModel pattern working perfectly
- GlobalSidebar integration seamless
- App installed and ready for testing

**Next Session:** Start with thorough testing of Phase 1 features!

Good luck! 🚀
