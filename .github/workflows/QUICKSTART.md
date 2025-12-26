# GitHub Actions Quick Start

Get started with automated builds and releases in 5 minutes!

## 🎯 What You Get

✅ **Automatic builds** on every push/PR  
✅ **GitHub Releases** on version tags  
✅ **APK downloads** from releases  
✅ **Build artifacts** for testing  
✅ **Checksums** for verification  

---

## 🚀 Quick Start

### 1️⃣ Create Your First Build

Push code to trigger a build:

```bash
git push origin main
```

📍 **View:** Actions tab → Build Android APK

### 2️⃣ Create Your First Release

Tag and push:

```bash
git tag -a v1.0.0 -m "First release"
git push origin v1.0.0
```

📍 **View:** Releases section → Download APK

### 3️⃣ Manual Build (Optional)

1. Go to **Actions** tab
2. Click **"Manual Build and Release"**
3. Click **"Run workflow"**
4. Select options and run

---

## 📁 What Was Created

```
.github/workflows/
├── build-apk.yml       # Auto-build on push/PR
├── release-apk.yml     # Auto-release on tags
├── manual-build.yml    # Manual on-demand builds
├── README.md           # Full documentation
└── QUICKSTART.md       # This file

Root directory:
├── RELEASE_GUIDE.md           # Release process guide
├── CI_CD_SETUP.md             # Complete CI/CD docs
└── signing.properties.template # APK signing template
```

---

## 📖 Documentation

- **[README.md](README.md)** - Complete workflow documentation
- **[RELEASE_GUIDE.md](../../RELEASE_GUIDE.md)** - How to create releases
- **[CI_CD_SETUP.md](../../CI_CD_SETUP.md)** - CI/CD architecture and setup

---

## 🔄 Common Tasks

### Build APK for Testing
```bash
# Push to any branch
git push origin develop
# Download from Actions → Artifacts
```

### Create Beta Release
```bash
git tag -a v1.0.0-beta -m "Beta release"
git push origin v1.0.0-beta
```

### Download APK
1. Go to **Releases** page
2. Click on a release
3. Download APK from **Assets**

---

## ⚙️ Configuration (Optional)

### APK Signing

For signed releases, add GitHub Secrets:

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Add these secrets:
   - `KEYSTORE_FILE` (base64-encoded keystore)
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

See [signing.properties.template](../../signing.properties.template) for details.

### Workflow Permissions

Ensure workflows have write permissions:

1. **Settings** → **Actions** → **General**
2. **Workflow permissions** → **"Read and write permissions"**
3. Save

---

## ✅ Verification

Check everything is working:

- [ ] Push to main triggers build workflow
- [ ] Workflow completes successfully
- [ ] APK artifacts are created
- [ ] Tag push creates release
- [ ] Release has APK files
- [ ] Manual workflow can be triggered

---

## 🆘 Need Help?

**Build failed?**
- Check Actions tab for logs
- Review error messages
- See [README.md](README.md) troubleshooting section

**Release not created?**
- Verify tag format: `v1.0.0` (must start with 'v')
- Check workflow permissions
- View workflow run in Actions tab

**Can't download APK?**
- Check Releases section
- Look in workflow Artifacts
- Verify build completed successfully

---

## 📚 Learn More

- [Full Workflow Documentation](README.md)
- [Release Process Guide](../../RELEASE_GUIDE.md)
- [CI/CD Architecture](../../CI_CD_SETUP.md)
- [GitHub Actions Docs](https://docs.github.com/en/actions)

---

**Happy Building! 🎉**

Questions? Check the documentation or open an issue.
