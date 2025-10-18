# Obtanium Setup Guide

This guide will help you set up Ribbit for distribution through Obtanium.

## Prerequisites

1. **GitHub Repository**: ✅ Already set up at `https://github.com/TekkadanPlays/ribbit-android`
2. **Obtanium App**: Install from [F-Droid](https://f-droid.org/packages/com.github.imranr98.obtainium/) or [GitHub](https://github.com/ImranR98/Obtainium/releases)

## Setup Steps

### 1. Create a Release

1. **Build the release APK**:
   ```powershell
   .\scripts\release.ps1 1.0 1
   ```

2. **Test the APK** on your device to ensure it works correctly

3. **Commit and push changes**:
   ```bash
   git add .
   git commit -m "Release v1.0"
   git push
   ```

4. **Create a GitHub Release**:
   - Go to your GitHub repository
   - Click "Releases" → "Create a new release"
   - Tag: `v1.0`
   - Title: `Ribbit v1.0`
   - Upload the APK file: `releases/ribbit-v1.0.apk`

### 2. Add Screenshots (Optional but Recommended)

1. Create a `screenshots/` directory in your repository
2. Add screenshots of your app:
   - `screenshot1.png` - Main dashboard
   - `screenshot2.png` - Thread view
   - `screenshot3.png` - Profile screen
3. Update the screenshot URLs in `obtanium.json`

### 3. Add App Icon and Banner

1. **App Icon**: Already configured to use the launcher icon
2. **Banner**: Create a `banner.png` (1200x600px) and add it to the repository root

### 4. Configure Obtanium

1. **Open Obtanium** on your device
2. **Add Repository**:
   - Tap the "+" button
   - Enter: `https://github.com/TekkadanPlays/ribbit-android`
   - Tap "Add"
3. **Install Ribbit**:
   - Find "Ribbit" in the repository
   - Tap "Install"
   - Follow the installation prompts

### 5. Update Obtanium Manifest

The `obtanium.json` file is already configured with:
- ✅ Correct repository URL
- ✅ Author information (Tekkadan)
- ✅ Nostr contact
- ✅ App description and features
- ✅ Release information

## Testing Obtanium Integration

1. **Install Obtanium** on a test device
2. **Add your repository** using the GitHub URL
3. **Verify** that Ribbit appears in the app list
4. **Test installation** to ensure the APK downloads and installs correctly
5. **Check updates** - Obtanium should detect new releases automatically

## Maintenance

### For Future Releases

1. **Update version** in `app/build.gradle.kts`
2. **Run release script**: `.\scripts\release.ps1 <version> <code>`
3. **Create GitHub release** with the new APK
4. **Update obtanium.json** with new version info
5. **Push changes** to GitHub

### Updating Obtanium Manifest

When you make changes to the app description, features, or metadata:
1. Edit `obtanium.json`
2. Commit and push changes
3. Obtanium will automatically pick up the updates

## Troubleshooting

### Common Issues

1. **APK not found**: Ensure the GitHub release has the APK file uploaded
2. **Installation fails**: Check that the APK is properly signed
3. **Obtanium can't find the app**: Verify the repository URL is correct
4. **Version not updating**: Clear Obtanium cache and refresh

### Getting Help

- **Obtanium Issues**: [GitHub Issues](https://github.com/ImranR98/Obtainium/issues)
- **Ribbit Issues**: [Your Repository Issues](https://github.com/TekkadanPlays/ribbit-android/issues)

## Security Notes

- **Keystore**: The `keystore.properties` file is in `.gitignore` for security
- **Signing**: Release builds use the debug keystore by default (suitable for testing)
- **Production**: For production releases, use a proper release keystore

## Success! 🎉

Once set up, users can:
- Install Ribbit directly from Obtanium
- Receive automatic updates
- View app information and screenshots
- Access your source code and contact information

Your app is now ready for distribution through Obtanium!

