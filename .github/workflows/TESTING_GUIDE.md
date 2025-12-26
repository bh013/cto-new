# Workflow Testing Guide

Complete guide for testing your GitHub Actions workflows before and after deployment.

## 📋 Table of Contents

1. [Pre-Deployment Testing](#pre-deployment-testing)
2. [Initial Deployment](#initial-deployment)
3. [Post-Deployment Testing](#post-deployment-testing)
4. [Testing Scenarios](#testing-scenarios)
5. [Troubleshooting](#troubleshooting)
6. [Validation Checklist](#validation-checklist)

---

## 🧪 Pre-Deployment Testing

### 1. YAML Syntax Validation

Before pushing, validate YAML syntax:

```bash
# Using yamllint (if available)
yamllint .github/workflows/*.yml

# Using Python (if yamllint not available)
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build-apk.yml'))"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release-apk.yml'))"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/manual-build.yml'))"
```

### 2. Local Build Testing

Test that your app builds locally:

```bash
# Clean build
./gradlew clean

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Check outputs
ls -lh app/build/outputs/apk/debug/
ls -lh app/build/outputs/apk/release/
```

**Expected Outputs:**
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

### 3. Verify gradlew Permissions

```bash
# Check permissions
ls -l gradlew

# Should show executable permission (rwxr-xr-x)
# If not, make executable:
chmod +x gradlew
git add gradlew
git commit -m "Make gradlew executable"
```

### 4. Check .gitignore

Verify sensitive files won't be committed:

```bash
# Should be in .gitignore
grep -E "(keystore|signing.properties|local.properties)" .gitignore

# Test that signing files are ignored
touch signing.properties
git status  # Should not show signing.properties
rm signing.properties
```

---

## 🚀 Initial Deployment

### 1. Push Workflow Files

```bash
# Add all workflow files
git add .github/

# Add documentation
git add RELEASE_GUIDE.md CI_CD_SETUP.md WORKFLOW_IMPLEMENTATION_SUMMARY.md
git add signing.properties.template

# Add modified files
git add .gitignore README.md

# Commit
git commit -m "Add GitHub Actions workflows for CI/CD

- Add build workflow for automatic builds
- Add release workflow for GitHub Releases
- Add manual build workflow for on-demand builds
- Add comprehensive documentation
- Add APK signing template
- Update README with CI/CD information"

# Push to remote
git push origin feat/android-ci-release-apk-workflows
```

### 2. Enable GitHub Actions

1. Go to repository on GitHub
2. Click **Settings** tab
3. Click **Actions** > **General**
4. Under **Actions permissions**:
   - Select **"Allow all actions and reusable workflows"**
5. Under **Workflow permissions**:
   - Select **"Read and write permissions"**
   - Check **"Allow GitHub Actions to create and approve pull requests"**
6. Click **Save**

### 3. Merge to Main (if using PR)

```bash
# Create pull request on GitHub
# After review and approval, merge to main

# Or merge directly if you have permission
git checkout main
git merge feat/android-ci-release-apk-workflows
git push origin main
```

---

## ✅ Post-Deployment Testing

### Test 1: Build Workflow (Push Trigger)

**Objective:** Verify build workflow runs on push

**Steps:**

1. Make a small change:
   ```bash
   git checkout main
   echo "# CI/CD Testing" >> TESTING.md
   git add TESTING.md
   git commit -m "Test build workflow trigger"
   git push origin main
   ```

2. Monitor workflow:
   - Go to **Actions** tab
   - Find "Build Android APK" workflow
   - Watch the run progress

3. Verify success:
   - ✅ Workflow completes successfully
   - ✅ All steps show green checkmarks
   - ✅ Build summary displays correctly

4. Check artifacts:
   - Scroll to bottom of workflow run
   - Find **Artifacts** section
   - Verify `app-debug` and `app-release` are present
   - Download and test APKs

**Expected Duration:** 5-10 minutes

**Troubleshooting:** See [Troubleshooting](#troubleshooting) section

---

### Test 2: Build Workflow (PR Trigger)

**Objective:** Verify build workflow runs on pull requests

**Steps:**

1. Create feature branch:
   ```bash
   git checkout -b test/pr-workflow
   echo "Testing PR workflow" >> TEST_PR.md
   git add TEST_PR.md
   git commit -m "Test PR workflow"
   git push origin test/pr-workflow
   ```

2. Create pull request:
   - Go to repository on GitHub
   - Click **Pull requests** tab
   - Click **New pull request**
   - Select `test/pr-workflow` → `main`
   - Click **Create pull request**

3. Verify workflow:
   - Workflow should start automatically
   - Check "Checks" tab in PR
   - Wait for completion

4. Verify results:
   - ✅ Workflow shows in PR checks
   - ✅ Build passes successfully
   - ✅ Can download artifacts from PR

**Cleanup:**
```bash
# Close PR and delete branch
git checkout main
git branch -D test/pr-workflow
git push origin --delete test/pr-workflow
```

---

### Test 3: Release Workflow (Tag Trigger)

**Objective:** Verify release workflow creates GitHub Release

**Steps:**

1. Create test release tag:
   ```bash
   git checkout main
   git pull origin main
   git tag -a v0.0.1-test -m "Test release workflow"
   git push origin v0.0.1-test
   ```

2. Monitor workflow:
   - Go to **Actions** tab
   - Find "Release Android APK" workflow
   - Watch the run progress

3. Verify release:
   - Go to **Releases** section
   - Find "MapLocator v0.0.1-test" release
   - Check release has:
     - ✅ Debug APK
     - ✅ Release APK
     - ✅ Checksums file
     - ✅ Auto-generated release notes

4. Download and verify:
   ```bash
   # Download APKs from release
   # Download checksums.txt
   
   # Verify checksums
   md5sum MapLocator-v0.0.1-test-release.apk
   sha256sum MapLocator-v0.0.1-test-release.apk
   # Compare with checksums.txt
   ```

**Expected Duration:** 5-10 minutes

**Cleanup:**
```bash
# Delete test tag
git tag -d v0.0.1-test
git push origin :refs/tags/v0.0.1-test
# Manually delete release from GitHub Releases page
```

---

### Test 4: Manual Build Workflow

**Objective:** Verify manual workflow works with custom options

**Steps:**

1. Trigger manual workflow:
   - Go to **Actions** tab
   - Click **"Manual Build and Release"**
   - Click **"Run workflow"**
   - Select options:
     - Build type: `both`
     - Create release: `false`
   - Click **"Run workflow"**

2. Monitor execution:
   - Workflow should appear in list
   - Watch progress

3. Verify artifacts:
   - ✅ Workflow completes
   - ✅ Debug APK artifact created
   - ✅ Release APK artifact created
   - ✅ Build summary displays

4. Test manual release:
   - Trigger again with:
     - Build type: `release`
     - Create release: `true`
     - Release tag: `v0.0.2-manual`
     - Release title: `Test Manual Release`
   - Verify release is created

**Expected Duration:** 5-10 minutes

**Cleanup:**
```bash
# Delete manual release tag
git tag -d v0.0.2-manual
git push origin :refs/tags/v0.0.2-manual
# Delete release from GitHub
```

---

## 🎯 Testing Scenarios

### Scenario 1: Feature Development

**Workflow:** Build Workflow

```bash
# Developer creates feature branch
git checkout -b feature/new-feature
# Make changes...
git push origin feature/new-feature

# Create PR
# Workflow runs automatically
# Review build results in PR

# Merge after approval
```

**Expected:** Build runs on PR, provides feedback

---

### Scenario 2: Production Release

**Workflow:** Release Workflow

```bash
# Prepare release
git checkout main
git pull origin main

# Create release tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Wait for workflow
# GitHub Release created automatically
# Users can download APKs
```

**Expected:** Release with APKs and checksums

---

### Scenario 3: Beta Release

**Workflow:** Release Workflow (Pre-release)

```bash
# Create beta tag
git tag -a v1.1.0-beta -m "Beta release 1.1.0"
git push origin v1.1.0-beta

# Workflow detects "beta" in tag
# Marks as pre-release automatically
```

**Expected:** Pre-release created, marked as beta

---

### Scenario 4: Hotfix Release

**Workflow:** Manual Build → Release Workflow

```bash
# Create hotfix branch from main
git checkout -b hotfix/1.0.1 v1.0.0
# Fix bug...
git push origin hotfix/1.0.1

# Use manual workflow to test build
# After verification, create release tag
git tag -a v1.0.1 -m "Hotfix 1.0.1"
git push origin v1.0.1

# Merge back to main
git checkout main
git merge hotfix/1.0.1
```

**Expected:** Hotfix builds and releases

---

### Scenario 5: Quick Debug Build

**Workflow:** Manual Build

```bash
# Developer needs quick debug APK
# Go to Actions → Manual Build
# Select: Build type = debug, Create release = false
# Download APK from artifacts
```

**Expected:** Debug APK without release

---

## 🔧 Troubleshooting

### Issue: Workflow Doesn't Trigger

**Symptoms:**
- Push code but workflow doesn't start
- Tag pushed but no release created

**Checks:**
1. Verify Actions are enabled:
   - Settings → Actions → General
   - "Allow all actions" should be selected

2. Check workflow file location:
   ```bash
   ls .github/workflows/*.yml
   # Should show workflow files
   ```

3. Verify trigger patterns:
   ```bash
   # For build workflow: push to main, develop, feature/*
   # For release: tag matching v*
   git tag -l  # List all tags
   git branch  # List all branches
   ```

4. Check workflow file syntax:
   - View file in GitHub
   - Look for syntax errors highlighted

**Solution:**
- Fix trigger patterns
- Re-push or re-tag
- Check Actions tab for errors

---

### Issue: Build Fails with "Permission Denied"

**Symptoms:**
```
./gradlew: Permission denied
```

**Solution:**
```bash
# Make gradlew executable
chmod +x gradlew
git add gradlew
git commit -m "Make gradlew executable"
git push
```

---

### Issue: APK Not Found

**Symptoms:**
- Workflow succeeds but no APK in artifacts
- "File not found" error in upload step

**Checks:**
1. Verify build step succeeded
2. Check APK output path
3. Review build logs

**Solution:**
```bash
# Test local build
./gradlew assembleDebug
ls app/build/outputs/apk/debug/

# Check path matches workflow
# Default: app/build/outputs/apk/debug/app-debug.apk
```

---

### Issue: Release Not Created

**Symptoms:**
- Workflow succeeds but no release appears

**Checks:**
1. Verify permissions:
   - Settings → Actions → General
   - "Read and write permissions" should be selected

2. Check workflow logs:
   - Actions → Release workflow → "Create GitHub Release" step
   - Look for error messages

**Solution:**
- Enable write permissions
- Check GITHUB_TOKEN is available
- Verify tag format (must start with 'v')

---

### Issue: Checksums Don't Match

**Symptoms:**
- Downloaded APK checksum differs from checksums.txt

**Possible Causes:**
- Download was corrupted
- APK was modified after creation
- Checksum was calculated incorrectly

**Solution:**
```bash
# Re-download APK and checksums
# Calculate checksum
md5sum downloaded.apk
sha256sum downloaded.apk

# Compare with checksums.txt
# If different, re-download or regenerate
```

---

### Issue: Workflow Timeout

**Symptoms:**
```
The job running on runner ... has exceeded the maximum execution time of 30 minutes.
```

**Causes:**
- Network issues downloading dependencies
- Gradle daemon issues
- Build is genuinely too slow

**Solution:**
1. Increase timeout:
   ```yaml
   jobs:
     build:
       timeout-minutes: 45  # Increase from 30
   ```

2. Optimize Gradle:
   ```properties
   # Add to gradle.properties
   org.gradle.caching=true
   org.gradle.parallel=true
   ```

3. Check build logs for slow steps

---

## ✅ Validation Checklist

### Initial Setup
- [ ] All workflow files in `.github/workflows/`
- [ ] YAML syntax is valid (no errors)
- [ ] gradlew is executable
- [ ] .gitignore includes signing files
- [ ] Actions enabled in repository settings
- [ ] Workflow permissions set to read/write

### Build Workflow
- [ ] Triggers on push to main
- [ ] Triggers on push to develop
- [ ] Triggers on push to feature/*
- [ ] Triggers on PR to main
- [ ] Triggers on PR to develop
- [ ] Builds debug APK successfully
- [ ] Builds release APK successfully
- [ ] Uploads debug artifact
- [ ] Uploads release artifact
- [ ] Build summary displays
- [ ] Build logs available

### Release Workflow
- [ ] Triggers on v* tag push
- [ ] Can be manually triggered
- [ ] Extracts version correctly
- [ ] Builds both APKs
- [ ] Generates checksums (MD5)
- [ ] Generates checksums (SHA256)
- [ ] Creates GitHub Release
- [ ] Uploads debug APK to release
- [ ] Uploads release APK to release
- [ ] Uploads checksums file
- [ ] Auto-generates changelog
- [ ] Detects pre-releases correctly
- [ ] Release notes formatted correctly

### Manual Workflow
- [ ] Can be triggered manually
- [ ] Build type options work (debug)
- [ ] Build type options work (release)
- [ ] Build type options work (both)
- [ ] Creates release when selected
- [ ] Custom tags work
- [ ] Custom titles work
- [ ] Artifacts uploaded correctly
- [ ] Build summary displays

### Documentation
- [ ] README includes CI/CD section
- [ ] QUICKSTART.md is clear
- [ ] Workflow README is comprehensive
- [ ] RELEASE_GUIDE.md is helpful
- [ ] CI_CD_SETUP.md covers advanced topics
- [ ] signing.properties.template is complete
- [ ] BADGES.md explains status badges
- [ ] All examples are correct

### Security
- [ ] Keystore files in .gitignore
- [ ] signing.properties in .gitignore
- [ ] Template file created (not actual)
- [ ] Documentation warns about secrets
- [ ] No secrets in workflow files
- [ ] Minimal permissions used

---

## 📊 Performance Benchmarks

Expected workflow durations (approximate):

| Workflow | Duration | Notes |
|----------|----------|-------|
| Build (cold cache) | 8-12 min | First run, downloads all dependencies |
| Build (warm cache) | 3-5 min | Cached dependencies |
| Release (cold cache) | 10-15 min | Includes changelog generation |
| Release (warm cache) | 4-7 min | Cached dependencies |
| Manual (debug only) | 2-4 min | Single APK, warm cache |
| Manual (both) | 4-7 min | Both APKs, warm cache |

**Cache Hit Rate:**
- First run: 0% (cold)
- Subsequent runs: 90%+ (warm)

---

## 🎓 Best Practices

1. **Test Locally First**
   - Always build locally before pushing
   - Catches issues early

2. **Use Feature Branches**
   - Develop in feature branches
   - Create PR for review
   - Workflow validates changes

3. **Tag from Main**
   - Only tag stable code
   - Always from main branch
   - Use semantic versioning

4. **Review Workflow Logs**
   - Check logs even if successful
   - Look for warnings
   - Monitor build times

5. **Keep Documentation Updated**
   - Update docs when changing workflows
   - Add examples for new features
   - Keep troubleshooting current

6. **Test Pre-releases**
   - Use beta/alpha tags
   - Test before stable release
   - Get user feedback

---

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow Syntax Reference](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)
- [Android Build Guide](https://developer.android.com/studio/build)
- [Semantic Versioning](https://semver.org/)

---

**Happy Testing! 🧪**

If you encounter issues not covered here, please:
1. Check workflow logs
2. Review main documentation
3. Open an issue with details
