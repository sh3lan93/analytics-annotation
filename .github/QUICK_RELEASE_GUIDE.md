# 🚀 Quick Release Guide

## TL;DR - How to Create a Release

### Step 1: Bump the Version
Edit `plugin/build.gradle.kts`:
```kotlin
version = "2.1.0"  // Change this to your new version
```

### Step 2: Commit and Push
```bash
git add plugin/build.gradle.kts
git commit -m "chore: Bump version to 2.1.0"
git push origin main
```

### Step 3: Done! 🎉
The release workflow automatically:
- ✅ Creates tag `v2.1.0`
- ✅ Generates changelog from commits
- ✅ Creates GitHub Release
- ✅ Builds artifacts

**Check it**: Go to [Releases](https://github.com/sh3lan93/analytics-annotation/releases) tab

---

## 📊 What Gets Released

When you merge a commit to `main`, the workflow:

| Component | Action |
|-----------|--------|
| **Git Tag** | Creates `v{version}` (e.g., `v2.1.0`) |
| **GitHub Release** | Creates release page with changelog |
| **Changelog** | Generated from recent commits using git-cliff |
| **Build Artifacts** | Project build (JAR/AAR files) |

---

## 🔑 Key Points

- **Single Source of Truth**: Version in `plugin/build.gradle.kts`
- **Automatic**: No manual tag creation needed
- **Safe**: Won't create duplicate releases if tag exists
- **Smart Changelog**: Groups commits by type (Features, Fixes, etc.)
- **Uses git-cliff**: Leverages your existing `cliff.toml` config

---

## 📝 Version Bumping Examples

### Patch Release (Bug fixes, small changes)
```bash
version = "2.0.0"  →  version = "2.0.1"
```

### Minor Release (New features)
```bash
version = "2.0.0"  →  version = "2.1.0"
```

### Major Release (Breaking changes)
```bash
version = "2.0.0"  →  version = "3.0.0"
```

---

## ⚡ Manual Trigger (Advanced)

If you want to trigger the release workflow manually without a push:

1. Go to **Actions** tab
2. Select **Release** workflow
3. Click **Run workflow** button
4. Confirm

---

## 🐛 Troubleshooting

**Q: Tag already exists, no release created?**
A: The version in `plugin/build.gradle.kts` matches an existing tag. Bump the version to create a new release.

**Q: Changelog is empty?**
A: Ensure commits follow conventional commit format (see [COMMIT_CONVENTIONS.md](COMMIT_CONVENTIONS.md))

**Q: Release didn't appear?**
A: Check **Actions** tab for workflow logs. Common issues:
- Version wasn't bumped (tag already exists)
- Commits don't follow conventional format
- Network issue pushing tag

---

## 📖 For More Details

See [RELEASE_WORKFLOW.md](RELEASE_WORKFLOW.md) for:
- Complete workflow documentation
- Configuration details
- Troubleshooting guide
- Git-Cliff integration details

---

**Example Release Flow**:
```
Feature Branch → PR → Review → Merge to main → Auto-Release ✨
                                    ↓
                              Bump version
                                    ↓
                              Create tag v2.1.0
                                    ↓
                              GitHub Release
```

**That's it!** The rest happens automatically. 🤖