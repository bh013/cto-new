# GitHub Actions Workflows Documentation

This directory contains GitHub Actions workflows for automated building and releasing of the MapLocator Android application.

## 📋 Table of Contents

- [Workflows Overview](#workflows-overview)
- [Build Workflow](#build-workflow)
- [Release Workflow](#release-workflow)
- [Manual Build Workflow](#manual-build-workflow)
- [How to Use](#how-to-use)
- [APK Signing Configuration](#apk-signing-configuration)
- [Troubleshooting](#troubleshooting)

---

## 🔄 Workflows Overview

### 1. **build-apk.yml** - Continuous Integration Build
Automatically builds APK files when code is pushed or pull requests are created.

**Triggers:**
- Push to `main`, `develop`, or `feature/*` branches
- Pull requests to `main` or `develop` branches

**What it does:**
- ✅ Builds debug APK
- ✅ Builds release APK (unsigned)
- ✅ Uploads artifacts for download (30-day retention)
- ✅ Generates build summary
- ✅ Uploads build logs on failure

### 2. **release-apk.yml** - Automated Release
Creates a GitHub Release with APK files when a version tag is pushed.

**Triggers:**
- Push of tags matching `v*` (e.g., `v1.0.0`, `v2.1.3`)
- Manual workflow dispatch

**What it does:**
- ✅ Builds debug and release APKs
- ✅ Updates version in build.gradle
- ✅ Generates checksums (MD5 & SHA256)
- ✅ Creates GitHub Release with auto-generated changelog
- ✅ Uploads APK files to release assets
- ✅ Marks pre-releases automatically (alpha, beta, rc)

### 3. **manual-build.yml** - On-Demand Build
Allows manual triggering of builds with custom options.

**Triggers:**
- Manual workflow dispatch only

**What it does:**
- ✅ Build debug, release, or both
- ✅ Optionally create a GitHub Release
- ✅ Custom release tags and titles
- ✅ Upload artifacts or release assets

---

## 🏗️ Build Workflow

### When it Runs
The build workflow runs automatically on:
- Every push to `main`, `develop`, or `feature/*` branches
- Every pull request targeting `main` or `develop`

### Outputs
After a successful build, you can download APK files from the workflow run:

1. Go to **Actions** tab in your repository
2. Click on the workflow run
3. Scroll down to **Artifacts** section
4. Download `app-debug` or `app-release` artifacts

### Build Configuration
- **JDK Version:** 17 (Temurin distribution)
- **Gradle Version:** 8.0+ (from gradle wrapper)
- **Timeout:** 30 minutes
- **Artifact Retention:** 30 days

### Example: Triggering a Build
```bash
# Push to main branch
git checkout main
git push origin main

# Create a feature branch
git checkout -b feature/my-new-feature
git push origin feature/my-new-feature
```

---

## 🚀 Release Workflow

### Creating a Release

The release workflow is triggered by creating and pushing a version tag:

#### Step 1: Create a Tag
```bash
# Create an annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# For pre-releases (will be marked as pre-release automatically)
git tag -a v1.0.0-beta -m "Beta release 1.0.0"
git tag -a v1.0.0-rc1 -m "Release candidate 1.0.0"
```

#### Step 2: Push the Tag
```bash
# Push the tag to trigger the workflow
git push origin v1.0.0
```

#### Step 3: Wait for Build
- The workflow will start automatically
- Watch progress in the **Actions** tab
- Build typically takes 5-10 minutes

#### Step 4: Download APK
Once complete, the release will appear in the **Releases** section:
1. Go to repository **Releases** page
2. Find your release (e.g., "MapLocator v1.0.0")
3. Download APK files from **Assets** section

### Manual Release Trigger

You can also trigger a release manually:

1. Go to **Actions** tab
2. Select **Release Android APK** workflow
3. Click **Run workflow**
4. Enter tag name (e.g., `v1.0.0`)
5. Choose if it's a pre-release
6. Click **Run workflow**

### Release Assets

Each release includes:
- **MapLocator-vX.X.X-debug.apk** - Debug version with debugging enabled
- **MapLocator-vX.X.X-release.apk** - Release version (unsigned/signed)
- **checksums.txt** - MD5 and SHA256 checksums for verification

### Pre-release Detection

Releases are automatically marked as pre-release if the tag contains:
- `alpha` (e.g., `v1.0.0-alpha`)
- `beta` (e.g., `v1.0.0-beta`)
- `rc` (e.g., `v1.0.0-rc1`)

---

## 🎯 Manual Build Workflow

### When to Use
Use the manual build workflow when you need:
- A quick build without creating a release
- A release with a custom tag/title
- To build only debug or only release APK

### How to Trigger

1. Go to **Actions** tab
2. Select **Manual Build and Release** workflow
3. Click **Run workflow**
4. Configure options:
   - **Build type:** `debug`, `release`, or `both`
   - **Create release:** Check to create a GitHub Release
   - **Release tag:** Tag name if creating a release
   - **Release title:** Custom title (optional)
5. Click **Run workflow**

### Example Use Cases

**Quick Debug Build:**
- Build type: `debug`
- Create release: Unchecked
- Downloads available as artifact

**Custom Release:**
- Build type: `both`
- Create release: Checked
- Release tag: `v1.0.0-custom`
- Release title: `MapLocator 1.0.0 - Special Build`

---

## 🔐 APK Signing Configuration

### Current Setup
By default, the workflows build **unsigned release APKs**. For production releases, you should sign your APKs.

### Setting Up Signing

#### Step 1: Generate Keystore
```bash
# Generate a new keystore
keytool -genkey -v -keystore maplocator-release.keystore \
  -alias maplocator -keyalg RSA -keysize 2048 -validity 10000
```

#### Step 2: Create signing.properties Template
Create `app/signing.properties.template`:
```properties
# Signing configuration template
# Copy this file to signing.properties and fill in your values
# DO NOT commit signing.properties to git!

RELEASE_STORE_FILE=/path/to/keystore
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

#### Step 3: Update app/build.gradle

Add signing configuration to `app/build.gradle`:

```groovy
android {
    // ... existing config ...

    signingConfigs {
        release {
            if (project.hasProperty('RELEASE_STORE_FILE')) {
                storeFile file(RELEASE_STORE_FILE)
                storePassword RELEASE_STORE_PASSWORD
                keyAlias RELEASE_KEY_ALIAS
                keyPassword RELEASE_KEY_PASSWORD
            }
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### Step 4: Configure GitHub Secrets

Add the following secrets to your repository (**Settings > Secrets and variables > Actions**):

1. **KEYSTORE_FILE**: Base64-encoded keystore file
   ```bash
   base64 maplocator-release.keystore > keystore.base64
   # Copy content and add as secret
   ```

2. **KEYSTORE_PASSWORD**: Keystore password
3. **KEY_ALIAS**: Key alias name
4. **KEY_PASSWORD**: Key password

#### Step 5: Update Workflows

Add these steps to `release-apk.yml` and `manual-build.yml` before building:

```yaml
- name: Decode keystore
  if: ${{ secrets.KEYSTORE_FILE != '' }}
  run: |
    echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > app/release.keystore

- name: Create signing properties
  if: ${{ secrets.KEYSTORE_FILE != '' }}
  run: |
    echo "RELEASE_STORE_FILE=release.keystore" >> gradle.properties
    echo "RELEASE_STORE_PASSWORD=${{ secrets.KEYSTORE_PASSWORD }}" >> gradle.properties
    echo "RELEASE_KEY_ALIAS=${{ secrets.KEY_ALIAS }}" >> gradle.properties
    echo "RELEASE_KEY_PASSWORD=${{ secrets.KEY_PASSWORD }}" >> gradle.properties
```

### Security Best Practices

✅ **DO:**
- Store keystore in GitHub Secrets
- Use strong passwords
- Keep keystore backup in secure location
- Add `signing.properties` and `*.keystore` to `.gitignore`

❌ **DON'T:**
- Commit keystore files to repository
- Share keystore passwords in code
- Use weak or default passwords
- Lose your keystore (you can't update the app without it!)

---

## 📦 Downloading APK Files

### From GitHub Actions (Build Artifacts)

1. Go to **Actions** tab
2. Click on a workflow run
3. Scroll to **Artifacts** section at the bottom
4. Click to download `app-debug` or `app-release`
5. Extract the ZIP file to get the APK

**Note:** Artifacts expire after 30 days.

### From GitHub Releases

1. Go to **Releases** section (right sidebar)
2. Click on a release version
3. Scroll to **Assets** section
4. Click to download APK files directly

**Note:** Release assets are permanent.

### Verifying Checksums

To verify APK integrity:

```bash
# Download checksums.txt from release
# Download APK file

# Verify MD5
md5sum MapLocator-v1.0.0-release.apk
# Compare with value in checksums.txt

# Verify SHA256
sha256sum MapLocator-v1.0.0-release.apk
# Compare with value in checksums.txt
```

---

## 🔧 Troubleshooting

### Build Fails with "Permission Denied"

**Problem:** `./gradlew: Permission denied`

**Solution:** The workflow includes `chmod +x ./gradlew`, but if this persists:
```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
git push
```

### Build Fails with Memory Error

**Problem:** `java.lang.OutOfMemoryError`

**Solution:** Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

### Release Workflow Not Triggering

**Problem:** Pushed tag but workflow didn't run

**Checklist:**
- ✅ Tag matches pattern `v*` (e.g., `v1.0.0`)
- ✅ Tag was pushed to remote: `git push origin v1.0.0`
- ✅ Workflow file exists in the same commit as the tag
- ✅ Check Actions tab for any error messages

### APK Not Found in Artifacts

**Problem:** Workflow succeeds but no APK artifact

**Solution:** Check the build log:
- Look for "Upload debug APK artifact" step
- Check path: `app/build/outputs/apk/debug/app-debug.apk`
- Verify the APK was actually built in "Build debug APK" step

### Release APK is Unsigned

**Problem:** `app-release-unsigned.apk` instead of signed

**Solution:** See [APK Signing Configuration](#apk-signing-configuration) section above.

### Workflow Timeout

**Problem:** Workflow exceeds 30-minute timeout

**Solutions:**
- Check for network issues downloading dependencies
- Increase timeout in workflow file: `timeout-minutes: 45`
- Enable Gradle daemon: Remove `GRADLE_OPTS: -Dorg.gradle.daemon=false`

### Cache Issues

**Problem:** Build fails with corrupted cache

**Solution:** Clear Gradle cache:
1. Go to **Actions** tab
2. Go to **Caches** (in left sidebar)
3. Delete Gradle caches
4. Re-run workflow

---

## 📚 Additional Resources

### Workflow Files
- `build-apk.yml` - CI build workflow
- `release-apk.yml` - Release workflow
- `manual-build.yml` - Manual build workflow

### Documentation
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Build Documentation](https://developer.android.com/studio/build)
- [Gradle Wrapper Documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html)

### Useful Commands

```bash
# List all tags
git tag -l

# Delete a tag locally
git tag -d v1.0.0

# Delete a tag remotely
git push origin :refs/tags/v1.0.0

# View tag details
git show v1.0.0

# Build locally
./gradlew assembleDebug
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

---

## 🤝 Contributing

When modifying workflows:

1. Test changes in a feature branch first
2. Use `workflow_dispatch` for testing
3. Check the Actions tab for results
4. Update this documentation if needed

---

## 📝 Version History

- **v1.0** - Initial workflow setup with build, release, and manual workflows
  - Automated CI/CD builds
  - GitHub Releases integration
  - Checksum generation
  - Build summaries and logging

---

**Questions or Issues?**

If you encounter problems with the workflows:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review workflow logs in the Actions tab
3. Check build logs in artifacts
4. Open an issue in the repository
