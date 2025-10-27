# Testing Checklist - Ribbit Android

**Version**: 1.0.0  
**Last Updated**: January 2025  
**Purpose**: Comprehensive testing checklist for real Quartz/Nostr integration

---

## 🎯 Pre-Testing Setup

### Prerequisites
- [ ] Android device or emulator (Android 8.0+)
- [ ] Amber app installed and configured
- [ ] At least one Nostr account in Amber
- [ ] Internet connection
- [ ] Logcat access for debugging

### Test Environment Setup
1. [ ] Install latest Ribbit APK
2. [ ] Clear app data if testing fresh install
3. [ ] Launch Amber and verify accounts
4. [ ] Enable developer options on device
5. [ ] Connect to logcat for monitoring

---

## 📱 Phase 1: Basic Functionality Testing

### 1.1 App Launch
- [ ] App launches without crash
- [ ] Welcome/login screen appears (if not signed in)
- [ ] No ANR (Application Not Responding) errors
- [ ] Splash screen displays correctly (if implemented)

### 1.2 Amber Integration
- [ ] "Sign in with Amber" button appears
- [ ] Tapping button launches Amber
- [ ] Connection request appears in Amber
- [ ] Approving connection returns to Ribbit
- [ ] Account selection works (if multiple accounts)
- [ ] Profile appears in sidebar after connection
- [ ] Public key (npub) displays correctly

### 1.3 Account Switching
- [ ] Open sidebar → Tap profile → "Switch Account"
- [ ] Amber launches for account selection
- [ ] Selecting different account switches successfully
- [ ] Relay configs change per account
- [ ] Previous account data not leaked to new account

---

## 🌐 Phase 2: Relay Configuration Testing

### 2.1 Relay Management Screen Access
- [ ] Navigate to Settings → Relay Management
- [ ] Or sidebar → "Relays"
- [ ] Screen loads without errors
- [ ] General and Personal tabs visible

### 2.2 General Tab - Category Management

#### Creating Categories
- [ ] Tap "+ Add Category"
- [ ] Input field appears
- [ ] Enter category name (e.g., "Test Category")
- [ ] Tap "Add"
- [ ] Category appears in list
- [ ] Can create multiple categories
- [ ] Duplicate names allowed (or prevented as designed)

#### Adding Relays to Category
- [ ] Tap "+" next to category name
- [ ] Input field appears for relay URL
- [ ] Enter valid relay: `wss://relay.damus.io`
- [ ] Tap "Add"
- [ ] Relay appears under category
- [ ] Can add multiple relays to same category
- [ ] Invalid URL format rejected (test: `http://invalid`)

#### Removing Relays
- [ ] Tap "-" next to relay URL
- [ ] Relay removed immediately
- [ ] No crash on removal

#### Setting Favorite Category
- [ ] Tap star icon next to category name
- [ ] Star turns yellow (⭐)
- [ ] Previous favorite loses star (only one favorite)
- [ ] Favorite persists on app restart
- [ ] Favorite persists per account

#### Expanding/Collapsing Categories
- [ ] Tap category name or caret
- [ ] Relays show/hide
- [ ] Smooth animation
- [ ] State persists during session

#### Removing Categories
- [ ] Long press on category (if implemented)
- [ ] Or use delete button
- [ ] Confirmation dialog appears
- [ ] Category and all relays removed
- [ ] No crash

### 2.3 Personal Tab - Inbox/Outbox/Cache

#### Adding Inbox Relays
- [ ] Tap Personal tab
- [ ] Tap "+" next to Inbox
- [ ] Enter relay URL
- [ ] Relay added successfully
- [ ] Add 2-3 relays

#### Adding Outbox Relays
- [ ] Tap "+" next to Outbox
- [ ] Enter relay URL
- [ ] Relay added successfully
- [ ] Can add multiple relays

#### Adding Cache Relays
- [ ] Tap "+" next to Cache
- [ ] Enter relay URL
- [ ] Relay added successfully
- [ ] Cache relays used for profile fetching

#### Removing Personal Relays
- [ ] Tap "-" next to any personal relay
- [ ] Relay removed successfully
- [ ] No crash

### 2.4 Relay Persistence
- [ ] Configure relays for Account A
- [ ] Close app completely
- [ ] Reopen app
- [ ] Verify relays still configured
- [ ] Switch to Account B
- [ ] Verify Account B has different/empty relays
- [ ] Switch back to Account A
- [ ] Verify Account A relays restored

---

## 📰 Phase 3: Home Feed Testing

### 3.1 Initial Load

#### With No Relays
- [ ] Launch app with no relays configured
- [ ] See empty state: "Add some relays to get started"
- [ ] "Add Relays" button visible
- [ ] Tapping button navigates to Relay Management
- [ ] No crash or frozen UI

#### With Relays Configured
- [ ] Add 3 relays to "General" category
- [ ] Set "General" as favorite (⭐)
- [ ] Navigate to Home screen
- [ ] Pull down to refresh
- [ ] Loading indicator appears
- [ ] Notes start appearing within 5 seconds
- [ ] At least 1 note appears (if relays have content)

### 3.2 Note Display

#### Note Card Content
- [ ] Avatar displays (default first, then real avatar)
- [ ] Display name shows
- [ ] Username shows (if different from display name)
- [ ] Timestamp shows (relative or absolute)
- [ ] Note content displays correctly
- [ ] Hashtags display (if present)
- [ ] No text overflow or cut-off

#### Note Interactions
- [ ] Like button tappable
- [ ] Like count updates on tap
- [ ] Unlike works (tap again)
- [ ] Share button tappable
- [ ] Comment button tappable (opens thread)
- [ ] Zap button tappable (opens zap menu)
- [ ] More (⋮) menu tappable

### 3.3 Profile Loading

#### Profile Pictures
- [ ] Notes initially show default avatars
- [ ] After 5-10 seconds, real avatars load
- [ ] Avatars don't flicker or reload repeatedly
- [ ] All visible avatars load eventually
- [ ] Network images load correctly

#### Profile Names
- [ ] Initially shows pubkey preview (abc123...)
- [ ] After profile loads, shows real name
- [ ] Display name and username both update
- [ ] No UI jump when names change

### 3.4 Scroll Performance
- [ ] Smooth scrolling with 10 notes
- [ ] Smooth scrolling with 50 notes
- [ ] Smooth scrolling with 100+ notes
- [ ] No frame drops or stuttering
- [ ] Scroll to top button works (if implemented)
- [ ] Tap status bar to scroll to top

### 3.5 Pull to Refresh
- [ ] Swipe down from top
- [ ] Refresh indicator appears
- [ ] Existing notes remain
- [ ] New notes appear at top
- [ ] Loading indicator disappears when done
- [ ] No crash during refresh

### 3.6 Empty States
- [ ] All relays offline: appropriate message
- [ ] No notes available: "No notes yet"
- [ ] Loading state: "Loading notes..."
- [ ] Error state: error message displayed

---

## 🎯 Phase 4: Sidebar Navigation Testing

### 4.1 Sidebar Access
- [ ] Tap hamburger menu (☰) to open
- [ ] Swipe from left edge to open
- [ ] Sidebar slides in smoothly
- [ ] Tap outside to close
- [ ] Swipe left to close
- [ ] Back button closes sidebar

### 4.2 Profile Section
- [ ] Avatar displays at top
- [ ] Name displays correctly
- [ ] Public key preview displays
- [ ] Tap profile to view profile screen (if implemented)

### 4.3 Category Navigation

#### Viewing Categories
- [ ] All created categories listed
- [ ] Favorite category marked (⭐)
- [ ] Categories alphabetically sorted (or creation order)
- [ ] Expand/collapse works

#### Loading from Category
- [ ] Tap category name (e.g., "Tech")
- [ ] Sidebar closes
- [ ] Home feed loading indicator appears
- [ ] Notes load from all relays in that category
- [ ] Old notes clear, new notes appear

#### Loading from Single Relay
- [ ] Expand category to show relays
- [ ] Tap specific relay URL
- [ ] Sidebar closes
- [ ] Home feed loads notes ONLY from that relay
- [ ] Verify by checking note variety/source

### 4.4 Bottom Navigation
- [ ] Relays button → opens Relay Management
- [ ] Settings button → opens Settings
- [ ] Logout button → signs out of Amber

---

## 📢 Phase 5: Announcements Feed Testing

### 5.1 Accessing Announcements
- [ ] Tap "Announcements" in bottom nav
- [ ] Announcements screen loads
- [ ] Header shows "Announcements"
- [ ] No crash

### 5.2 Empty State (No Relay)
- [ ] If no announcement relay configured
- [ ] See message: "No Announcement Relay"
- [ ] Appropriate icon displays
- [ ] Helpful message displayed

### 5.3 With Configured Relay
- [ ] Configure announcement relay (in code or settings)
- [ ] Navigate to Announcements
- [ ] Pull to refresh
- [ ] Announcements load (if any)
- [ ] Notes display like home feed
- [ ] Only announcements from configured pubkey (if filtered)

### 5.4 Announcement Interactions
- [ ] Like button works
- [ ] Share button works
- [ ] Comment button works
- [ ] Zap button works (if implemented)
- [ ] More menu works

---

## 🔄 Phase 6: Multi-Relay Testing

### 6.1 Adding Multiple Relays
- [ ] Add 5 relays to favorite category:
  - `wss://relay.damus.io`
  - `wss://nos.lol`
  - `wss://relay.primal.net`
  - `wss://nostr.wine`
  - `wss://relay.snort.social`
- [ ] Navigate to Home
- [ ] Pull to refresh
- [ ] Notes from multiple relays load
- [ ] More variety in notes (different authors)

### 6.2 Relay Performance
- [ ] All 5 relays connect successfully
- [ ] Notes arrive within 5 seconds
- [ ] No timeout errors
- [ ] Logcat shows successful connections

### 6.3 Relay Failures
- [ ] Add invalid relay: `wss://invalid.example.com`
- [ ] App doesn't crash
- [ ] Other relays still work
- [ ] Error logged (check logcat)
- [ ] Notes still load from working relays

---

## ⚡ Phase 7: Performance Testing

### 7.1 Load Time
- [ ] Time from app launch to first note: ____ seconds (target: < 5s)
- [ ] Time from relay config to notes: ____ seconds (target: < 3s)
- [ ] Profile pictures load time: ____ seconds (target: < 10s)

### 7.2 Memory Usage
- [ ] Launch app → check memory usage
- [ ] Load 100 notes → check memory usage
- [ ] Load 500 notes → check memory usage
- [ ] No memory leaks (use Android Profiler)
- [ ] Memory usage reasonable (< 200MB for 500 notes)

### 7.3 Battery Usage
- [ ] Use app for 30 minutes
- [ ] Check battery drain
- [ ] Should be reasonable (< 10% per hour)

### 7.4 Network Usage
- [ ] Monitor network traffic
- [ ] Loading 100 notes uses reasonable data
- [ ] No excessive websocket traffic
- [ ] Connections close when not needed

### 7.5 Scroll Performance
- [ ] Load 500+ notes
- [ ] Scroll rapidly up and down
- [ ] No frame drops (use GPU rendering profile)
- [ ] Smooth 60 FPS
- [ ] No UI freezes

---

## 🐛 Phase 8: Edge Cases & Error Handling

### 8.1 Network Issues

#### No Internet
- [ ] Disable WiFi and mobile data
- [ ] Try to load notes
- [ ] See appropriate error message
- [ ] No crash
- [ ] Re-enable internet
- [ ] Pull to refresh works

#### Intermittent Connection
- [ ] Toggle airplane mode during note loading
- [ ] App handles gracefully
- [ ] No crash
- [ ] Reconnects when internet returns

#### Slow Connection
- [ ] Use network throttling (developer options)
- [ ] Notes load slowly but correctly
- [ ] Loading indicator shows
- [ ] No timeout crashes

### 8.2 Invalid Data

#### Malformed Relay URL
- [ ] Add relay: `invalid-url`
- [ ] App rejects or handles gracefully
- [ ] No crash

#### Invalid Event Data
- [ ] (Requires test relay with bad data)
- [ ] App parses what it can
- [ ] Skips invalid events
- [ ] Logs error
- [ ] No crash

### 8.3 Resource Exhaustion

#### Many Relays
- [ ] Add 20+ relays to favorite category
- [ ] App still works (may be slow)
- [ ] No crash
- [ ] Consider warning user

#### Many Categories
- [ ] Create 50+ categories
- [ ] UI still responsive
- [ ] Scrolling in sidebar works

#### Large Notes
- [ ] Find note with 10,000+ characters
- [ ] Note displays (may truncate)
- [ ] No crash
- [ ] Scroll performance OK

### 8.4 State Management

#### App Backgrounding
- [ ] Load notes
- [ ] Background app (home button)
- [ ] Wait 5 minutes
- [ ] Return to app
- [ ] Notes still visible
- [ ] Can refresh

#### Configuration Changes
- [ ] Load notes
- [ ] Rotate device
- [ ] Notes still visible
- [ ] UI adapts to landscape/portrait
- [ ] No crash

#### Process Death
- [ ] Enable "Don't keep activities" (Developer options)
- [ ] Load notes
- [ ] Background app
- [ ] Android kills process
- [ ] Return to app
- [ ] App restores correctly
- [ ] Can load notes again

---

## 🔐 Phase 9: Security & Privacy Testing

### 9.1 Amber Integration
- [ ] Ribbit never displays or logs private keys
- [ ] All signing goes through Amber
- [ ] Amber permissions respected
- [ ] Revoking Amber access logs out

### 9.2 Data Isolation
- [ ] Account A data not visible to Account B
- [ ] Relay configs isolated per account
- [ ] Notes isolated per account (based on relays)

### 9.3 Network Security
- [ ] All relay connections use WSS (wss://)
- [ ] No plaintext WS (ws://) connections allowed
- [ ] Certificate validation works

---

## 📊 Phase 10: Regression Testing

After any code changes, retest:
- [ ] App launch
- [ ] Amber connection
- [ ] Relay configuration (add/remove)
- [ ] Note loading (favorite category)
- [ ] Profile caching
- [ ] Sidebar navigation
- [ ] Account switching
- [ ] Pull to refresh

---

## ✅ Acceptance Criteria

### Must Pass
- [ ] App launches without crash
- [ ] Amber integration works
- [ ] Can add relays
- [ ] Notes load from favorite category
- [ ] Profile pictures eventually load
- [ ] No data leaks between accounts
- [ ] App doesn't crash on common actions

### Should Pass
- [ ] Notes load within 5 seconds
- [ ] Profile pictures load within 10 seconds
- [ ] Smooth scrolling with 100+ notes
- [ ] Graceful error handling
- [ ] Pull to refresh works

### Nice to Have
- [ ] Notes load within 3 seconds
- [ ] Profile pictures load within 5 seconds
- [ ] 60 FPS scrolling with 500+ notes
- [ ] < 5% battery drain per hour

---

## 📝 Bug Reporting Template

When reporting bugs, include:

```
**Bug Title**: [Short description]

**Severity**: Critical / High / Medium / Low

**Steps to Reproduce**:
1. 
2. 
3. 

**Expected Result**:


**Actual Result**:


**Environment**:
- Device: 
- Android Version: 
- Ribbit Version: 
- Amber Version: 

**Logs** (attach logcat):


**Screenshots** (if applicable):

```

---

## 🎯 Testing Completion

### Summary
- Total test cases: ___
- Passed: ___
- Failed: ___
- Blocked: ___
- Not Tested: ___

### Critical Issues Found
1. 
2. 
3. 

### Recommendations
1. 
2. 
3. 

### Sign-off
- Tester Name: _________________
- Date: _________________
- Status: ☐ Approved  ☐ Needs Work  ☐ Blocked

---

**Testing Version**: 1.0.0  
**Last Updated**: January 2025  
**Status**: Ready for Testing