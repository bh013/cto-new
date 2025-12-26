# MapLocator Release Guide

Quick reference guide for creating releases and building APKs using GitHub Actions.

## 🚀 Quick Start: Creating a Release

### 1. Prepare Your Code
```bash
# Make sure you're on main branch and up to date
git checkout main
git pull origin main

# Ensure all changes are committed
git status
```

### 2. Create and Push a Version Tag
```bash
# Create a tag (replace X.X.X with your version number)
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag to trigger the release workflow
git push origin v1.0.0
```

### 3. Monitor the Build
1. Go to your repository on GitHub
2. Click on the **Actions** tab
3. Watch the **Release Android APK** workflow
4. Wait for it to complete (usually 5-10 minutes)

### 4. Download Your APK
1. Go to the **Releases** section
2. Find your release (e.g., "MapLocator v1.0.0")
3. Download APK files from the **Assets** section

---

## 📱 Version Numbering

Use [Semantic Versioning](https://semver.org/):

- **Major.Minor.Patch** (e.g., `v1.0.0`)
  - **Major**: Breaking changes
  - **Minor**: New features (backwards compatible)
  - **Patch**: Bug fixes

### Examples:
- `v1.0.0` - First stable release
- `v1.1.0` - Added new features
- `v1.1.1` - Fixed bugs
- `v2.0.0` - Major update with breaking changes

### Pre-releases:
- `v1.0.0-alpha` - Alpha version (early testing)
- `v1.0.0-beta` - Beta version (feature complete, testing)
- `v1.0.0-rc1` - Release candidate 1 (final testing)

Pre-releases are automatically marked as "Pre-release" on GitHub.

---

## 🔄 Workflow Triggers

### Automatic Build (CI)
Triggers automatically on:
- Push to `main`, `develop`, or `feature/*`
- Pull requests to `main` or `develop`

**Result:** APK artifacts available for download (30 days)

### Automatic Release
Triggers on:
- Push of tags matching `v*` pattern

**Result:** GitHub Release created with APK files

### Manual Build
Triggered manually from Actions tab:
- Choose build type (debug/release/both)
- Optionally create a release
- Custom tags and titles

---

## 📋 Common Tasks

### Build Debug APK for Testing
```bash
# Option 1: Push to develop branch (automatic build)
git checkout develop
git push origin develop
# Download from Actions > Artifacts

# Option 2: Use manual workflow
# Go to Actions > Manual Build and Release > Run workflow
# Build type: debug
# Create release: No
```

### Create a Pre-release
```bash
# Tag with alpha, beta, or rc
git tag -a v1.0.0-beta -m "Beta release"
git push origin v1.0.0-beta
# Automatically marked as pre-release
```

### Create a Hotfix Release
```bash
# Create hotfix branch
git checkout -b hotfix/1.0.1 v1.0.0
# Make fixes
git commit -m "Fix critical bug"
git push origin hotfix/1.0.1

# After review, create release tag
git tag -a v1.0.1 -m "Hotfix release 1.0.1"
git push origin v1.0.1

# Merge back to main
git checkout main
git merge hotfix/1.0.1
git push origin main
```

### Delete a Tag/Release (if needed)
```bash
# Delete tag locally
git tag -d v1.0.0

# Delete tag remotely
git push origin :refs/tags/v1.0.0

# Then manually delete the release from GitHub Releases page
```

---

## 🔐 APK Signing (Optional but Recommended)

For production releases, you should sign your APKs.

### Quick Setup:

1. **Generate keystore** (one-time):
   ```bash
   keytool -genkey -v -keystore maplocator-release.keystore \
     -alias maplocator -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Add to GitHub Secrets** (Settings > Secrets and variables > Actions):
   - `KEYSTORE_FILE` - Base64 encoded keystore
   - `KEYSTORE_PASSWORD` - Your keystore password
   - `KEY_ALIAS` - Your key alias
   - `KEY_PASSWORD` - Your key password

3. **Update workflows** to use signing (see `.github/workflows/README.md`)

**Important:** Never commit keystore files or passwords to the repository!

---

## 📥 Installing APK on Android Device

### Method 1: Direct Download on Device
1. Open release page on device browser
2. Download APK file
3. Tap downloaded file
4. Enable "Install from Unknown Sources" if prompted
5. Install

### Method 2: ADB Install
```bash
# Download APK to computer
# Connect device via USB
adb install MapLocator-v1.0.0-release.apk
```

### Method 3: Transfer and Install
1. Download APK to computer
2. Transfer to device (USB, email, cloud storage)
3. Open file manager on device
4. Tap APK file to install

---

## ✅ Verification Checklist

Before creating a release:

- [ ] All tests pass
- [ ] Code is reviewed and merged to main
- [ ] Version number is updated in code (workflow does this automatically)
- [ ] CHANGELOG or release notes are prepared
- [ ] App has been tested on real devices
- [ ] No debug code or logs in release build
- [ ] Permissions are correct
- [ ] Signing configuration is set up (for production)

---

## 🐛 Troubleshooting

### Build Failed
1. Check Actions tab for error logs
2. Review the failed step
3. Fix the issue and push again
4. Or re-run the workflow

### APK Not Working
1. Check Android version compatibility (min SDK 21)
2. Verify required permissions
3. Check Logcat for errors
4. Try debug APK for more error details

### Can't Install APK
1. Enable "Install from Unknown Sources"
2. Check available storage space
3. Uninstall previous version first
4. Verify APK is not corrupted (check checksums)

---

## 📚 More Information

For detailed documentation:
- **Workflows:** `.github/workflows/README.md`
- **App Features:** `TAXI_APP_FEATURES.md`
- **Usage:** `USAGE_INSTRUCTIONS.md`
- **Development:** `DEVELOPER_GUIDE.md`

---

## 🎯 Quick Reference Commands

```bash
# Create release
git tag -a v1.0.0 -m "Release 1.0.0" && git push origin v1.0.0

# Create beta release
git tag -a v1.0.0-beta -m "Beta 1.0.0" && git push origin v1.0.0-beta

# List all tags
git tag -l

# View tag details
git show v1.0.0

# Delete tag (local and remote)
git tag -d v1.0.0 && git push origin :refs/tags/v1.0.0

# Build locally
./gradlew assembleDebug
./gradlew assembleRelease
```

---

**Happy Releasing! 🎉**
