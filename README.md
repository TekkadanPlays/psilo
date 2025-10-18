# 🐸 Ribbit

A modern social networking platform built with Jetpack Compose and Material Design 3.

## Features

- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Thread View**: Hierarchical comment system with visual thread lines
- **State Preservation**: Maintains state across navigation and device rotation
- **Profile Management**: User profiles with note feeds
- **Search Functionality**: Find content and users
- **Responsive Design**: Optimized for all screen sizes
- **Performance**: Smooth scrolling and animations

## Screenshots

*Screenshots will be added here*

## Installation

### Via Obtanium (Recommended)

1. Install [Obtanium](https://github.com/ImranR98/Obtainium) from F-Droid
2. Add this repository: `https://github.com/TekkadanPlays/ribbit-android`
3. Install Ribbit from Obtanium

### Manual Installation

1. Download the latest APK from [Releases](https://github.com/TekkadanPlays/ribbit-android/releases)
2. Enable "Install from unknown sources" in your device settings
3. Install the APK

## Building from Source

### Prerequisites

- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK 35+

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/TekkadanPlays/ribbit-android.git
   cd ribbit-android
   ```

2. Open in Android Studio and sync the project

3. Build the debug version:
   ```bash
   ./gradlew assembleDebug
   ```

4. Build the release version:
   ```bash
   ./gradlew assembleRelease
   ```

## Architecture

- **UI**: Jetpack Compose with Material Design 3
- **State Management**: ViewModel with StateFlow
- **Navigation**: Custom navigation with state preservation
- **Performance**: Optimized with Baseline Profiles and Strong Skipping Mode

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

**Tekkadan**
- Nostr: [npub12zqf55l7l9vsg5f6ssx5pq4f9dzu6hcmnepkm8ftj25fecy379jqkq99h8](https://njump.me/npub12zqf55l7l9vsg5f6ssx5pq4f9dzu6hcmnepkm8ftj25fecy379jqkq99h8)
- UI made with 💚 by Tekkadan

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Designed with [Material Design 3](https://m3.material.io/)
- Icons from [Material Icons](https://fonts.google.com/icons)