# Status Badges Guide

Add status badges to your README to show build status and other information.

## 🎯 Available Badges

### Build Status Badge

Shows the status of your build workflow:

```markdown
![Build Status](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg)
```

**Replace:**
- `USERNAME` with your GitHub username
- `REPO` with your repository name

**Example:**
```markdown
![Build Status](https://github.com/johndoe/maplocator/workflows/Build%20Android%20APK/badge.svg)
```

**Result:**  
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)

---

### Release Badge

Shows the latest release version:

```markdown
![Release](https://img.shields.io/github/v/release/USERNAME/REPO)
```

**Example:**
```markdown
![Release](https://img.shields.io/github/v/release/johndoe/maplocator)
```

**Result:**  
![Release](https://img.shields.io/badge/release-v1.0.0-blue)

---

### License Badge

Shows the project license:

```markdown
![License](https://img.shields.io/badge/license-MIT-blue.svg)
```

**Result:**  
![License](https://img.shields.io/badge/license-MIT-blue)

---

### Platform Badge

Shows Android platform:

```markdown
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
```

**Result:**  
![Platform](https://img.shields.io/badge/platform-Android-green)

---

### API Level Badge

Shows minimum Android API level:

```markdown
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)
```

**Result:**  
![API](https://img.shields.io/badge/API-21%2B-brightgreen)

---

## 📝 Complete Badge Set

Add this to the top of your README.md:

```markdown
# MapLocator

![Build Status](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg)
![Release](https://img.shields.io/github/v/release/USERNAME/REPO)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

A complete Android application for location selection and taxi booking.
```

---

## 🎨 Badge Customization

### Colors

Available colors:
- `brightgreen` - Success
- `green` - Good
- `yellowgreen` - OK
- `yellow` - Warning
- `orange` - Important
- `red` - Error
- `lightgrey` - Neutral
- `blue` - Info

### Custom Badges

Create custom badges:

```markdown
![Custom](https://img.shields.io/badge/LABEL-MESSAGE-COLOR.svg)
```

**Examples:**

```markdown
![Java](https://img.shields.io/badge/Java-8-orange.svg)
![Gradle](https://img.shields.io/badge/Gradle-8.0-green.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)
```

---

## 🔄 Dynamic Badges

### GitHub Actions Badge

For specific branch:

```markdown
![Build](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg?branch=main)
```

### Download Count

Show release download count:

```markdown
![Downloads](https://img.shields.io/github/downloads/USERNAME/REPO/total)
```

### Last Commit

Show last commit date:

```markdown
![Last Commit](https://img.shields.io/github/last-commit/USERNAME/REPO)
```

### Contributors

Show number of contributors:

```markdown
![Contributors](https://img.shields.io/github/contributors/USERNAME/REPO)
```

---

## 📊 Shields.io

For more badge options, visit [shields.io](https://shields.io/)

### Popular Shields for Android

**Code Size:**
```markdown
![Code Size](https://img.shields.io/github/languages/code-size/USERNAME/REPO)
```

**Repo Size:**
```markdown
![Repo Size](https://img.shields.io/github/repo-size/USERNAME/REPO)
```

**Issues:**
```markdown
![Issues](https://img.shields.io/github/issues/USERNAME/REPO)
```

**Stars:**
```markdown
![Stars](https://img.shields.io/github/stars/USERNAME/REPO?style=social)
```

**Forks:**
```markdown
![Forks](https://img.shields.io/github/forks/USERNAME/REPO?style=social)
```

---

## 🎭 Badge Styles

Badges support different styles:

### Flat (default)
```markdown
![Badge](https://img.shields.io/badge/style-flat-blue?style=flat)
```

### Flat-Square
```markdown
![Badge](https://img.shields.io/badge/style-flat--square-blue?style=flat-square)
```

### For-the-Badge
```markdown
![Badge](https://img.shields.io/badge/style-for--the--badge-blue?style=for-the-badge)
```

### Plastic
```markdown
![Badge](https://img.shields.io/badge/style-plastic-blue?style=plastic)
```

### Social
```markdown
![Badge](https://img.shields.io/badge/style-social-blue?style=social)
```

---

## 💡 Examples

### Minimal Set
```markdown
![Build](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
```

### Standard Set
```markdown
![Build](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg)
![Release](https://img.shields.io/github/v/release/USERNAME/REPO)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)
```

### Complete Set
```markdown
![Build](https://github.com/USERNAME/REPO/workflows/Build%20Android%20APK/badge.svg)
![Release](https://img.shields.io/github/v/release/USERNAME/REPO)
![Downloads](https://img.shields.io/github/downloads/USERNAME/REPO/total)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Stars](https://img.shields.io/github/stars/USERNAME/REPO?style=social)
```

---

## 🔧 Badge Troubleshooting

### Badge Not Showing

**Workflow Name Mismatch:**
- Ensure workflow name matches exactly
- URL encode spaces as `%20`
- Example: "Build Android APK" → `Build%20Android%20APK`

**Repository Privacy:**
- Private repos may not show badges
- Enable badge sharing in repo settings

**Workflow Not Run:**
- Badge shows "no status" until first run
- Push code to trigger workflow

### Badge Shows Error

**Common Issues:**
- Wrong username/repo name
- Workflow file has syntax errors
- Workflow hasn't run yet
- Repository doesn't exist or is private

**Solution:**
- Verify workflow runs successfully
- Check Actions tab for errors
- Ensure repository is public or badge sharing is enabled

---

## 📚 Resources

- [Shields.io](https://shields.io/) - Badge generator
- [GitHub Badges](https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows/adding-a-workflow-status-badge) - Official docs
- [Simple Icons](https://simpleicons.org/) - Brand icons for badges

---

**Tip:** Test badges in a separate markdown file before adding to README!
