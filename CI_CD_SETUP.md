# CI/CD Setup for MapLocator

Complete guide for Continuous Integration and Continuous Deployment (CI/CD) setup using GitHub Actions.

## 📖 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Workflows](#workflows)
4. [Setup Instructions](#setup-instructions)
5. [Configuration](#configuration)
6. [Security](#security)
7. [Monitoring](#monitoring)
8. [Best Practices](#best-practices)

---

## 🎯 Overview

This project uses GitHub Actions for automated building, testing, and releasing of the MapLocator Android application.

### Benefits
- ✅ Automated builds on every push/PR
- ✅ Consistent build environment
- ✅ Automatic releases on version tags
- ✅ APK artifact storage
- ✅ Build verification and quality checks
- ✅ Reduced manual work

### Workflows Included
1. **build-apk.yml** - Continuous Integration
2. **release-apk.yml** - Release Automation
3. **manual-build.yml** - On-Demand Builds

---

## 🏗️ Architecture

### Build Pipeline Flow

```
┌─────────────────┐
│   Code Change   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Push/PR/Tag    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ GitHub Actions  │
│   Triggered     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Checkout Code  │
│   Setup JDK     │
│  Cache Gradle   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Build APK     │
│ (Debug/Release) │
└────────┬────────┘
         │
         ├─────────────┬──────────────┐
         ▼             ▼              ▼
┌──────────────┐ ┌──────────┐ ┌─────────────┐
│Upload Artifact│ │ Create   │ │Generate     │
│   (30 days)   │ │ Release  │ │Checksums    │
└──────────────┘ └──────────┘ └─────────────┘
```

### Trigger Matrix

| Event | Build Workflow | Release Workflow | Manual Workflow |
|-------|---------------|------------------|-----------------|
| Push to main | ✅ | ❌ | ❌ |
| Push to develop | ✅ | ❌ | ❌ |
| Push to feature/* | ✅ | ❌ | ❌ |
| Pull Request | ✅ | ❌ | ❌ |
| Tag v* | ❌ | ✅ | ❌ |
| Manual Trigger | ❌ | ✅* | ✅ |

*Manual trigger for release workflow only

---

## 🔄 Workflows

### 1. Build Workflow (build-apk.yml)

**Purpose:** Validate code changes and build APKs for testing

**Triggers:**
- Push to: `main`, `develop`, `feature/*`
- Pull requests to: `main`, `develop`

**Steps:**
1. Checkout repository
2. Set up JDK 17
3. Cache Gradle dependencies
4. Make gradlew executable
5. Build debug APK
6. Build release APK
7. Upload APK artifacts (30-day retention)
8. Generate build summary
9. Upload logs on failure

**Outputs:**
- `app-debug` artifact
- `app-release` artifact
- Build summary in workflow UI

**Configuration:**
```yaml
JDK Version: 17 (Temurin)
Gradle: 8.0+ (wrapper)
Timeout: 30 minutes
Runner: ubuntu-latest
```

### 2. Release Workflow (release-apk.yml)

**Purpose:** Automated release creation with APK distribution

**Triggers:**
- Tag push matching `v*` pattern
- Manual workflow dispatch

**Steps:**
1. Checkout repository (with full history)
2. Set up JDK 17
3. Cache Gradle dependencies
4. Extract version from tag
5. Update version in build.gradle
6. Build debug and release APKs
7. Rename APKs with version
8. Generate MD5 and SHA256 checksums
9. Generate changelog from commits
10. Create GitHub Release
11. Upload assets to release

**Outputs:**
- GitHub Release with:
  - `MapLocator-vX.X.X-debug.apk`
  - `MapLocator-vX.X.X-release.apk`
  - `checksums.txt`
  - Auto-generated release notes

**Pre-release Detection:**
Automatically marks as pre-release if tag contains:
- `alpha`
- `beta`
- `rc`

### 3. Manual Build Workflow (manual-build.yml)

**Purpose:** On-demand builds with custom options

**Triggers:**
- Manual dispatch only

**Options:**
- Build type: `debug`, `release`, or `both`
- Create release: Yes/No
- Release tag: Custom tag name
- Release title: Custom title

**Steps:**
1. Checkout repository
2. Set up JDK 17
3. Cache Gradle dependencies
4. Build selected APK type(s)
5. Optional: Create release with custom parameters
6. Upload artifacts or release assets
7. Generate build summary

**Use Cases:**
- Quick debug builds
- Custom release versions
- Testing build process
- Emergency releases

---

## 🚀 Setup Instructions

### Prerequisites
- GitHub repository for the project
- Android project with Gradle build system
- Git installed locally

### Step 1: Verify Project Structure
Ensure your project has:
```
project/
├── .github/
│   └── workflows/
│       ├── build-apk.yml
│       ├── release-apk.yml
│       └── manual-build.yml
├── app/
│   ├── build.gradle
│   └── src/
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle
└── settings.gradle
```

### Step 2: Enable Actions
1. Go to repository **Settings**
2. Click **Actions** > **General**
3. Enable **"Allow all actions and reusable workflows"**
4. Save changes

### Step 3: Set Permissions
1. In **Settings** > **Actions** > **General**
2. Scroll to **"Workflow permissions"**
3. Select **"Read and write permissions"**
4. Check **"Allow GitHub Actions to create and approve pull requests"**
5. Save changes

### Step 4: Test Build Workflow
```bash
# Push a change to trigger build
git checkout develop
git commit --allow-empty -m "Test CI workflow"
git push origin develop
```

Check Actions tab to verify workflow runs.

### Step 5: Test Release Workflow
```bash
# Create and push a test tag
git tag -a v0.0.1 -m "Test release"
git push origin v0.0.1
```

Check Releases section to verify release creation.

### Step 6: Configure Signing (Optional)
See [Security](#security) section for APK signing setup.

---

## ⚙️ Configuration

### Environment Variables

Workflows use these environment variables:

```yaml
GRADLE_OPTS: -Dorg.gradle.daemon=false
JAVA_VERSION: 17
GRADLE_VERSION: 8.0+
```

### Gradle Caching

Workflows cache:
- `~/.gradle/caches` - Dependency cache
- `~/.gradle/wrapper` - Gradle wrapper cache

Cache key: `${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}`

### Timeout Configuration

Default timeouts:
- All jobs: 30 minutes

To change timeout, edit workflow:
```yaml
jobs:
  build:
    timeout-minutes: 45  # Change to desired value
```

### Artifact Retention

- Build artifacts: 30 days
- Release assets: Permanent
- Build logs (on failure): 7 days

To change retention, edit workflow:
```yaml
- uses: actions/upload-artifact@v4
  with:
    retention-days: 60  # Change to desired value
```

### Build Variants

Current configuration builds:
- Debug APK: `app-debug.apk`
- Release APK: `app-release-unsigned.apk` (or signed)

To add custom variants, update `app/build.gradle`:
```groovy
android {
    buildTypes {
        staging {
            // Custom staging configuration
        }
    }
}
```

Then update workflows to build staging:
```yaml
- name: Build staging APK
  run: ./gradlew assembleStaging
```

---

## 🔐 Security

### Secrets Management

#### Required Secrets (for Signed APKs)
Add in **Settings** > **Secrets and variables** > **Actions**:

| Secret Name | Description | Example |
|------------|-------------|---------|
| `KEYSTORE_FILE` | Base64-encoded keystore | `MIIEowIBAAKCAQEA...` |
| `KEYSTORE_PASSWORD` | Keystore password | `myStrongPassword123` |
| `KEY_ALIAS` | Key alias name | `maplocator` |
| `KEY_PASSWORD` | Key password | `myKeyPassword456` |

#### Creating KEYSTORE_FILE Secret
```bash
# Encode keystore to base64
base64 -w 0 maplocator-release.keystore > keystore.base64

# Copy content of keystore.base64
cat keystore.base64

# Add as KEYSTORE_FILE secret in GitHub
```

### .gitignore Configuration

Ensure these are in `.gitignore`:
```gitignore
# Keystore files
*.jks
*.keystore
signing.properties

# Local configuration
local.properties

# Build outputs
*.apk
*.aab
build/
```

### Workflow Security

Workflows follow these security practices:
- ✅ Secrets are never logged
- ✅ Keystore is decoded at build time only
- ✅ Temporary files are cleaned up
- ✅ GITHUB_TOKEN has minimal permissions
- ✅ No third-party actions with write access

### Permissions

Workflows request minimal permissions:
```yaml
permissions:
  contents: write  # For creating releases only
```

### Best Practices
1. Never commit keystore files
2. Use strong passwords (20+ characters)
3. Rotate secrets periodically
4. Limit repository access
5. Enable branch protection
6. Require reviews for main branch
7. Use signed commits
8. Keep secrets backup in secure location

---

## 📊 Monitoring

### Viewing Workflow Runs

1. Go to **Actions** tab
2. Select a workflow from the left sidebar
3. View recent runs and their status

### Build Status Badge

Add to README.md:
```markdown
![Build Status](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg)
```

### Notifications

Configure notifications in GitHub settings:
1. Go to **Settings** (personal)
2. Click **Notifications**
3. Under **Actions**, enable:
   - Failed workflow runs
   - Successful workflow runs (optional)

### Build Logs

Access logs:
1. Click on workflow run
2. Click on job name
3. Expand step to view logs
4. Download logs via "Download log archive"

### Metrics to Monitor

- Build success rate
- Build duration
- APK size trends
- Failure patterns
- Cache hit rate

---

## 🎯 Best Practices

### Versioning
- Use semantic versioning: `vMAJOR.MINOR.PATCH`
- Tag stable releases: `v1.0.0`
- Tag pre-releases: `v1.0.0-beta`, `v1.0.0-rc1`
- Always include release notes

### Branch Strategy
```
main (production)
├── develop (integration)
│   ├── feature/new-feature-1
│   ├── feature/new-feature-2
│   └── bugfix/fix-issue-123
└── hotfix/urgent-fix (from main)
```

### Workflow Best Practices
1. Test locally before pushing
2. Use feature branches for development
3. Create PRs for code review
4. Merge only after CI passes
5. Tag releases from main branch
6. Write descriptive commit messages
7. Keep workflows DRY (reuse steps)

### APK Management
1. Keep debug APKs for testing
2. Sign release APKs for production
3. Test APKs on real devices
4. Verify checksums after download
5. Archive old releases if needed

### Build Optimization
1. Use Gradle caching (implemented)
2. Enable parallel builds
3. Use build cache
4. Optimize dependency resolution
5. Clean build outputs regularly

Add to `gradle.properties`:
```properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

### Troubleshooting Workflow

1. **Check logs** - Review workflow logs for errors
2. **Verify secrets** - Ensure secrets are set correctly
3. **Test locally** - Run `./gradlew assembleRelease`
4. **Clear cache** - Delete Gradle caches if corrupted
5. **Update dependencies** - Keep Gradle and plugins updated
6. **Ask for help** - Open an issue with logs

---

## 📚 Additional Resources

### Documentation
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Android Build Docs](https://developer.android.com/studio/build)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [APK Signing](https://developer.android.com/studio/publish/app-signing)

### Project Documentation
- `.github/workflows/README.md` - Detailed workflow documentation
- `RELEASE_GUIDE.md` - Quick release guide
- `DEVELOPER_GUIDE.md` - Development guide

### Useful Links
- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)

---

## 🔄 Maintenance

### Regular Tasks

**Weekly:**
- Review workflow runs
- Check for failed builds
- Monitor APK sizes

**Monthly:**
- Review and clean old artifacts
- Update dependencies
- Test on latest Android versions
- Review and update documentation

**Quarterly:**
- Rotate signing keys (if policy requires)
- Review security practices
- Update workflow actions to latest versions
- Performance optimization review

### Updating Workflows

To update a workflow:
1. Create feature branch
2. Edit workflow file
3. Test using `workflow_dispatch`
4. Create PR for review
5. Merge after approval

### Updating Dependencies

Keep these updated:
- Actions (e.g., `actions/checkout@v4`)
- JDK version
- Gradle version
- Android Gradle Plugin
- Android SDK tools

---

## ❓ FAQ

### Q: How do I create my first release?
```bash
git tag -a v1.0.0 -m "First release"
git push origin v1.0.0
```

### Q: Can I build locally with the same setup?
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### Q: How do I test a workflow without creating a release?
Use the manual build workflow with "Create release" unchecked.

### Q: What if a build fails?
1. Check the logs in Actions tab
2. Fix the issue
3. Push the fix or re-run the workflow

### Q: How do I delete a release?
1. Delete from Releases page (web UI)
2. Delete tag: `git push origin :refs/tags/v1.0.0`

### Q: Can I use different JDK versions?
Yes, edit workflow:
```yaml
- uses: actions/setup-java@v4
  with:
    java-version: '11'  # or 17, 19, etc.
```

---

## 🤝 Contributing

When contributing to CI/CD:
1. Test changes in a feature branch
2. Use manual workflow for testing
3. Document changes in this file
4. Update README if needed
5. Request review from maintainers

---

## 📝 Changelog

### Version 1.0 (Current)
- ✅ Build workflow for CI
- ✅ Release workflow with GitHub Releases
- ✅ Manual build workflow
- ✅ Gradle caching
- ✅ APK artifact uploads
- ✅ Checksum generation
- ✅ Build summaries
- ✅ Pre-release detection
- ✅ Comprehensive documentation

### Future Enhancements
- [ ] Unit test execution
- [ ] Code coverage reports
- [ ] Lint checks
- [ ] Security scanning
- [ ] Play Store upload
- [ ] Screenshot automation
- [ ] Performance testing

---

**Questions or Issues?**

If you encounter problems with CI/CD:
1. Check workflow logs
2. Review this documentation
3. Check `.github/workflows/README.md`
4. Open an issue with details

---

*Last updated: 2024*
