# Ribbit Android - User Guide

**Version**: 1.0.0  
**Last Updated**: January 2025  
**Status**: Production Ready with Live Nostr Integration

---

## 📖 Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Account Setup with Amber](#account-setup-with-amber)
4. [Relay Configuration](#relay-configuration)
5. [Using the Home Feed](#using-the-home-feed)
6. [Announcements Feed](#announcements-feed)
7. [Navigation and Sidebar](#navigation-and-sidebar)
8. [Features Guide](#features-guide)
9. [Troubleshooting](#troubleshooting)
10. [FAQ](#faq)

---

## 🎯 Overview

Ribbit is a modern Nostr client for Android that connects you to the decentralized social network. Unlike traditional social media, Nostr is:

- **Decentralized**: No central server controls your data
- **Censorship-resistant**: No one can ban you or delete your content
- **Portable**: Your identity (private key) works across all Nostr apps
- **Open**: Built on open protocols anyone can implement

### What Makes Ribbit Special?

- ✅ **Real Nostr Integration**: Live data from real Nostr relays
- ✅ **Amber Integration**: Secure key management with Amber
- ✅ **Custom Relay Management**: Organize relays into categories
- ✅ **Profile Caching**: Fast loading with intelligent profile caching
- ✅ **Material Design 3**: Beautiful, modern UI
- ✅ **Per-Account Settings**: Each account has separate relay configs

---

## 🚀 Getting Started

### Prerequisites

Before using Ribbit, you need:

1. **Android Device**: Android 8.0 (API 26) or higher
2. **Amber App**: Install Amber from [GitHub](https://github.com/greenart7c3/Amber) or F-Droid
3. **Nostr Identity**: Create or import a Nostr key pair in Amber

### Initial Setup

1. **Install Ribbit**
   - Download the APK from releases
   - Install on your Android device
   - Grant necessary permissions

2. **First Launch**
   - Open Ribbit
   - You'll see a welcome screen
   - Tap "Sign in with Amber"

3. **Connect Amber**
   - Ribbit will request connection to Amber
   - Approve the connection in Amber
   - Select which Nostr account to use

4. **Configure Relays** (Required for first use)
   - After signing in, tap "Add Relays" or navigate to Settings → Relays
   - Add at least one relay to see content

---

## 🔐 Account Setup with Amber

### What is Amber?

Amber is a Nostr signing app that securely stores your private keys. Instead of exposing your private key to every app, Amber:

- Stores your keys securely
- Signs events on behalf of apps
- Lets you approve/deny signing requests
- Supports multiple accounts

### Connecting to Amber

1. **Tap "Sign in with Amber"**
   - On first launch or from Settings → Account

2. **Approve in Amber**
   - Amber will show a connection request
   - Review the permissions Ribbit is requesting
   - Tap "Approve"

3. **Select Account**
   - If you have multiple Nostr accounts in Amber
   - Choose which one to use with Ribbit

4. **Success!**
   - You'll see your profile in the sidebar
   - Your public key (npub) will be displayed

### Switching Accounts

You can use multiple Nostr accounts with Ribbit:

1. **Open Sidebar**
   - Tap the hamburger menu (☰) or swipe from left edge

2. **Tap Your Profile**
   - At the top of the sidebar

3. **Tap "Switch Account"**
   - Ribbit will ask Amber to switch accounts
   - Select a different account in Amber

4. **Different Relay Configs**
   - Each account has its own relay configuration
   - When you switch accounts, relay settings switch too

---

## 🌐 Relay Configuration

Relays are servers that store and relay Nostr events. You need to configure relays to see content and post.

### Understanding Relay Types

Ribbit supports three types of relays:

#### 1. Category Relays (General Tab)
- Organized into custom categories
- Example: "Friends", "Tech News", "General"
- Your **Favorite** category feeds the home screen
- Can have multiple relays per category

#### 2. Personal Relays (Personal Tab)
- **Inbox Relays**: Where others send you messages/mentions
- **Outbox Relays**: Where you publish your content
- **Cache Relays**: Fast relays for fetching profiles

#### 3. Announcement Relays (Future)
- Special relay for official Ribbit/Tekkadan announcements

### Accessing Relay Management

1. **From Home Screen**
   - Open sidebar (☰)
   - Tap "Relays" at the bottom

2. **From Settings**
   - Tap Settings in bottom nav
   - Tap "Relay Management"

### General Tab: Creating Categories

Categories help you organize relays by topic or purpose.

#### Creating a Category

1. **Tap General Tab**
2. **Tap "+ Add Category"**
3. **Enter Category Name**
   - Example: "Tech", "Friends", "News"
4. **Tap "Add"**

#### Adding Relays to a Category

1. **Find Your Category**
   - Scroll to find it in the list

2. **Tap the "+" Button**
   - Next to the category name

3. **Enter Relay URL**
   - Format: `wss://relay.example.com`
   - Example: `wss://relay.damus.io`

4. **Tap "Add"**
   - The relay will appear in the category

#### Popular Public Relays

Here are some reliable public relays to get started:

- `wss://relay.damus.io` - Popular general relay
- `wss://nos.lol` - Fast and reliable
- `wss://relay.primal.net` - Primal's relay
- `wss://relay.nostr.band` - Good for search
- `wss://nostr.wine` - Well-maintained
- `wss://relay.snort.social` - Snort's relay

#### Setting Your Favorite Category

Your **favorite category** determines what appears in your home feed.

1. **Tap the Star Icon** next to a category name
2. **The star turns yellow** (⭐)
3. **Only ONE category can be favorite**
4. **Home feed loads notes from all relays in that category**

Example:
- Category: "General" (⭐ Favorite)
  - `wss://relay.damus.io`
  - `wss://nos.lol`
  - `wss://relay.primal.net`
- Home feed will load notes from all three relays

#### Removing Relays

1. **Tap the "-" Button** next to a relay
2. **Relay is removed immediately**

#### Deleting Categories

1. **Tap and Hold on Category Name**
2. **Confirm deletion**
3. **All relays in that category are removed**

### Personal Tab: Inbox/Outbox/Cache

Personal relays are specific to your Nostr workflow.

#### Inbox Relays

Where others send you direct messages and mentions.

1. **Tap Personal Tab**
2. **Tap "+" next to Inbox**
3. **Add relay URL**
4. **Recommend: 2-3 relays**

Common inbox relays:
- `wss://relay.damus.io`
- `wss://nos.lol`

#### Outbox Relays

Where you publish your notes and profile.

1. **Tap "+" next to Outbox**
2. **Add relay URL**
3. **Recommend: 3-5 relays for redundancy**

Tip: Use well-known relays so others can find your content.

#### Cache Relays

Fast relays used for fetching user profiles (kind-0 metadata).

1. **Tap "+" next to Cache**
2. **Add relay URL**
3. **Recommend: 2-3 fast relays**

Best cache relays:
- `wss://purplepag.es` - Specialized for profiles
- `wss://relay.nostr.band`
- `wss://nos.lol`

### Tips for Relay Configuration

✅ **Start Simple**
- Add 3-5 relays to your General category
- Set General as favorite
- Add 1-2 cache relays

✅ **More Relays = More Content**
- More relays means you see more notes
- But also means more bandwidth usage

✅ **Paid vs Free Relays**
- Free relays work great for most users
- Paid relays may offer faster speeds or special features

✅ **Test Your Relays**
- After adding relays, go to Home and pull to refresh
- If you see notes, relays are working!

---

## 📱 Using the Home Feed

The home feed shows notes from all relays in your **favorite category**.

### First Time Setup

After configuring relays:

1. **Go to Home** (bottom nav)
2. **Pull Down to Refresh**
3. **Wait 2-3 seconds**
4. **Notes start appearing!**

### Understanding the Feed

#### Note Card Layout

Each note card shows:

- **Avatar**: User's profile picture (if available)
- **Display Name**: User's display name or username
- **Username**: Shorter username
- **Timestamp**: When the note was posted
- **Content**: The note text
- **Hashtags**: Clickable hashtags (if any)
- **Action Buttons**: Like, Share, Comment, Zap, More

#### Interacting with Notes

**Like (Heart Icon)**
- Tap to like the note
- Tap again to unlike
- Like count updates in real-time

**Share (Repost Icon)**
- Tap to share/repost the note
- Share count updates

**Comment (Chat Icon)**
- Tap to open thread view
- See replies and add comments

**Zap (Lightning Icon)**
- Tap to send a Lightning tip
- Opens Zap amount selector
- Requires Lightning wallet setup

**More (⋮ Menu)**
- Copy note ID
- Share externally
- Report/mute (future)

### Loading States

#### Initial Load
- Shows "Loading notes..." with spinner
- Usually takes 2-5 seconds

#### No Relays Configured
- Shows "Add some relays to get started"
- Tap "Add Relays" button to configure

#### Empty Feed
- All relays connected but no notes yet
- Pull to refresh to try again
- May need to wait for events

### Pull to Refresh

Swipe down from the top to refresh:
- Reconnects to relays
- Fetches latest notes
- Updates profile information

### Scrolling

- Smooth infinite scroll
- Older notes load as you scroll down
- Tap status bar to scroll to top quickly

### Profile Pictures

Profile pictures load automatically:
- First, notes show with default avatar
- After a moment, real profile pictures appear
- This is the profile cache working!

---

## 📢 Announcements Feed

The Announcements feed shows official updates from Tekkadan/Ribbit team.

### Accessing Announcements

Tap **Announcements** in the bottom navigation bar.

### What You'll See

- Official Ribbit updates
- Feature announcements
- Maintenance notices
- Community news

### Configuration (Future)

Currently uses home feed. In the future:
- Settings → Announcements
- Configure announcement relay
- Set announcement account pubkey

---

## 🧭 Navigation and Sidebar

### Bottom Navigation

Four main destinations:

1. **Home** 🏠
   - Your main feed
   - Shows notes from favorite category
   - Pull to refresh

2. **Announcements** 📢
   - Official announcements
   - Tekkadan updates

3. **Notifications** 🔔
   - Mentions and replies (future)
   - Reactions to your notes (future)

4. **Settings** ⚙️
   - Account settings
   - Relay management
   - Appearance settings
   - About

### Global Sidebar

Access by:
- Tapping hamburger menu (☰)
- Swiping from left edge

#### Sidebar Sections

**Profile Section (Top)**
- Your avatar and name
- Tap to view your profile
- Tap to switch accounts

**Relay Categories (Middle)**
- Shows all your relay categories
- Tap category name to load notes from that category
- Tap expand/collapse arrow to see relays
- Tap individual relay to load notes from only that relay

**Navigation (Bottom)**
- Relays - Quick access to relay management
- Settings - App settings
- Logout - Sign out of current account

### Category/Relay Selection

#### Loading Notes from a Category

1. **Open Sidebar**
2. **Tap Category Name** (e.g., "Tech")
3. **Sidebar closes**
4. **Home feed loads notes from all relays in that category**

#### Loading Notes from a Single Relay

1. **Open Sidebar**
2. **Expand Category** (tap arrow)
3. **Tap Relay URL**
4. **Sidebar closes**
5. **Home feed loads notes ONLY from that relay**

This is useful for:
- Testing if a relay is working
- Seeing what content a specific relay has
- Debugging connection issues

---

## ⚙️ Features Guide

### Search (Future)

Will support:
- Search by content
- Search by hashtag
- Search by author

### Thread View

View full conversation threads:

1. **Tap on a Note**
2. **Thread View Opens**
3. **See All Replies**
4. **Add Your Reply**

### Profile View

View user profiles:

1. **Tap on Avatar or Username**
2. **Profile Screen Opens**
3. **See User's Notes**
4. **Follow/Unfollow** (future)

### Appearance Settings

Customize the app appearance:

1. **Settings → Appearance**
2. **Theme**: Light, Dark, System
3. **Font Size**: Small, Medium, Large
4. **Accent Color**: Choose your color (future)

---

## 🔧 Troubleshooting

### No Notes Appearing

**Problem**: Home feed is empty after adding relays.

**Solutions**:
1. Check you set a favorite category (⭐)
2. Pull down to refresh
3. Wait 5-10 seconds for connection
4. Try different relays (some may be offline)
5. Check internet connection

### Profile Pictures Not Loading

**Problem**: Seeing default avatars instead of real pictures.

**Solutions**:
1. Add cache relays in Personal tab
2. Wait 10-20 seconds for profiles to load
3. Pull to refresh
4. Some users may not have profile pictures set

### Amber Connection Failed

**Problem**: Can't connect to Amber.

**Solutions**:
1. Make sure Amber is installed
2. Check Amber has at least one account
3. Try restarting Amber app
4. Revoke Ribbit in Amber settings and reconnect
5. Check Android permissions for both apps

### Relay Not Connecting

**Problem**: Added a relay but no notes from it.

**Solutions**:
1. Check relay URL format: `wss://relay.example.com`
2. Test relay in another Nostr client
3. Some relays may be offline temporarily
4. Try a different relay from our recommended list
5. Check relay on https://nostr.watch

### App Crashes

**Solutions**:
1. Clear app cache (Settings → Apps → Ribbit → Clear Cache)
2. Restart the app
3. Check for updates
4. Report bug with crash logs

### Slow Performance

**Solutions**:
1. Reduce number of relays (try 3-5)
2. Close and reopen app
3. Clear profile cache (future feature)
4. Check device storage space

---

## ❓ FAQ

### General Questions

**Q: What is Nostr?**

A: Nostr is a decentralized social protocol. Instead of one company controlling your data (like Twitter), Nostr uses cryptographic keys and relays to create censorship-resistant social media.

**Q: Do I need to pay?**

A: Ribbit is free. Some Nostr relays charge for access, but many free relays are available.

**Q: Can I use my existing Nostr key?**

A: Yes! Import your key into Amber, then connect Ribbit to Amber.

**Q: Is my private key safe?**

A: Your private key never leaves Amber. Ribbit only requests signatures from Amber when needed.

### Technical Questions

**Q: How many relays should I use?**

A: Start with 3-5 relays for your main category. More relays = more content but more bandwidth.

**Q: What's the difference between inbox and outbox relays?**

A: 
- **Inbox**: Where you CHECK for messages sent to you
- **Outbox**: Where you PUBLISH your content
- Think: inbox = mailbox, outbox = post office

**Q: Why do I need cache relays?**

A: Cache relays are fast relays used specifically for fetching user profiles (names, avatars). This speeds up the UI.

**Q: Can I delete my posts?**

A: Technically, you can request deletion (NIP-09), but relays may choose to keep or delete. Nostr is designed to be permanent by default.

**Q: How do I backup my account?**

A: Your account is your private key, stored in Amber. Backup your key from Amber's settings.

### Privacy Questions

**Q: Is Nostr anonymous?**

A: Pseudonymous. Your public key is visible, but not linked to your real identity unless you choose to share it.

**Q: Can relays see my IP address?**

A: Yes, relays you connect to can see your IP. Use Tor/VPN if you need IP privacy.

**Q: Are my messages encrypted?**

A: Direct messages (DMs) are encrypted. Public notes are public by design.

### Troubleshooting Questions

**Q: Why aren't my notes posting?**

A: Check:
1. Amber connection is active
2. Outbox relays are configured
3. Internet connection is working

**Q: Why can't I see someone's posts?**

A: Their posts might be on relays you're not connected to. Try adding more relays.

**Q: App says "No relays configured"**

A: You need to add relays in Settings → Relays → General tab. Add at least one relay and set that category as favorite.

---

## 📞 Support

### Getting Help

1. **Check this guide first**
2. **Search GitHub Issues**: [Ribbit Issues](https://github.com/your-org/ribbit-android/issues)
3. **Report a Bug**: Create a new issue with:
   - Device model and Android version
   - Steps to reproduce
   - Screenshots if applicable
   - Crash logs if available

### Community

- **Nostr**: Follow us on Nostr (pubkey TBD)
- **GitHub**: Star the repo and contribute
- **Matrix/Discord**: Join our community (links TBD)

---

## 🎓 Learning More

### Nostr Resources

- **Nostr.com**: Official Nostr website
- **NostrResources.com**: Comprehensive guide
- **NIPs**: Nostr Implementation Possibilities (specs)
- **Nostr.watch**: Relay monitoring

### Contributing

Ribbit is open source! Contribute on GitHub:
- Report bugs
- Suggest features
- Submit pull requests
- Improve documentation

---

## 📝 Changelog

### Version 1.0.0 (Current)

✅ **Live Nostr Integration**
- Real kind-1 (text notes) from relays
- Profile caching (kind-0 metadata)
- Multi-relay support

✅ **Relay Management**
- Category organization
- Favorite category selection
- Personal inbox/outbox/cache relays
- Per-account relay configs

✅ **UI/UX**
- Material Design 3
- Global navigation sidebar
- Pull-to-refresh
- Thread view
- Collapsible headers

✅ **Amber Integration**
- Secure key management
- Multi-account support
- Account switching

### Coming Soon

🔄 **Phase 2 Features**
- Reactions (kind-7)
- Reply threads
- Reposts (kind-6)
- Lightning Zaps (NIP-57)
- User following
- Search

---

**Thank you for using Ribbit!** 🐸

If you have questions, feedback, or issues, please reach out. We're building this together.

---

**Last Updated**: January 2025  
**Version**: 1.0.0  
**License**: MIT (TBD)