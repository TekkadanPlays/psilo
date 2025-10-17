# Ribbit v0.1 🐸

A modern Android social media app built with Jetpack Compose and Material3 design principles.

## Features

### Core UI Framework
- **Material3 Design System** - Modern, adaptive UI components
- **Sharp, Edge-to-Edge Design** - No rounded corners, clean flat design
- **Jetpack Compose** - 100% declarative UI framework
- **Material Icons Extended** - Full icon library with R8 optimization

### Navigation & User Experience
- **Bottom Navigation** - Home, Search, Notifications, Messages, Profile
- **Sidebar Navigation** - Home, Bookmarks, Lists, Report a Bug
- **Feed Position Memory** - Scroll position preserved across navigation
- **Scroll-to-Top** - Home button scrolls to top with header expansion
- **Search Functionality** - Real-time search with smooth transitions

### Social Features
- **Note Cards** - Upvote, downvote, bookmark, comment, share, lightning
- **Profile Pages** - User profiles with stats and notes
- **Settings Page** - Flat design with organized options
- **About Page** - App information and branding

### Technical Features
- **Splash Screen** - Custom frog emoji launcher
- **R8 Minification** - Optimized builds with unused code elimination
- **State Management** - Compose state with ViewModels
- **Navigation** - Custom navigation system with back handling
- **Responsive Design** - Adaptive layouts for different screen sizes

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material3
- **Architecture**: MVVM with ViewModels
- **Build System**: Gradle with Kotlin DSL
- **Minification**: R8 (Release builds)
- **Icons**: Material Icons Extended
- **Networking**: Ktor (prepared for future API integration)

## Installation & Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or later
- Android SDK 35+ (API Level 35)
- Git

### Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ribbit-android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the project folder
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.example.views/.SplashActivity
   ```

### Build Variants

- **Debug**: Fast builds (~13s), all icons available, no minification
- **Release**: Optimized builds with R8 minification, smaller APK size

### Project Structure

```
app/
├── src/main/java/com/example/views/
│   ├── MainActivity.kt                 # Main activity with navigation
│   ├── SplashActivity.kt              # Splash screen
│   ├── data/                          # Data models and sample data
│   ├── ui/
│   │   ├── components/                # Reusable UI components
│   │   │   ├── AdaptiveHeader.kt      # Top app bar with search
│   │   │   ├── BottomNavigation.kt    # Bottom navigation bar
│   │   │   ├── ModernSidebar.kt       # Sidebar navigation
│   │   │   ├── NoteCard.kt           # Social media note cards
│   │   │   └── SharedElementTransition.kt
│   │   ├── screens/                   # Screen composables
│   │   │   ├── DashboardScreen.kt     # Main feed screen
│   │   │   ├── ProfileScreen.kt       # User profile screen
│   │   │   ├── SettingsScreen.kt      # Settings page
│   │   │   └── AboutScreen.kt         # About page
│   │   └── theme/                     # Material3 theming
│   └── viewmodel/                     # ViewModels for state management
├── src/main/res/                      # Android resources
│   ├── values/
│   │   ├── colors.xml                 # Color definitions
│   │   ├── strings.xml                # String resources
│   │   └── themes.xml                 # App themes
└── build.gradle.kts                   # Module build configuration
```

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep composables focused and reusable
- Use Material3 components consistently

### UI Components
- All components use Material3 design tokens
- Sharp, edge-to-edge design (no rounded corners)
- Consistent spacing using Material3 spacing system
- Proper accessibility support

### State Management
- Use `remember` for local state
- Use ViewModels for complex state
- Pass state down, events up pattern
- Use `collectAsState()` for ViewModel state

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Material3 Design System by Google
- Jetpack Compose team
- Android development community

---

**Ribbit v0.1** - Built with ❤️ using Jetpack Compose