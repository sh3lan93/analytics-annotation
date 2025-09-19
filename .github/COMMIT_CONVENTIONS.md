# Commit Message Conventions for Automatic Versioning

This repository uses **Conventional Commits** to automatically determine semantic version increments for snapshot releases.

## 🚀 How It Works

When you push to the `main` branch, our CI workflow analyzes commit messages and automatically creates snapshot releases with the appropriate version increment:

| Change Type | Version Increment | Example |
|-------------|-------------------|---------|
| 🚨 **Breaking Changes** | `MAJOR` (1.0.0 → 2.0.0) | `feat!: redesign API`, `BREAKING CHANGE: remove method` |
| ✨ **New Features** | `MINOR` (1.0.0 → 1.1.0) | `feat: add new provider`, `feature: implement caching` |
| 🐛 **Bug Fixes & Others** | `PATCH` (1.0.0 → 1.0.1) | `fix: resolve memory leak`, `docs: update README` |

## 📝 Commit Message Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

## 🏷️ Types for Version Increment

### 🚨 MAJOR Version Bump (Breaking Changes)
- `feat!:` - New feature with breaking changes
- `fix!:` - Bug fix with breaking changes
- `refactor!:` - Code refactoring with breaking changes
- `perf!:` - Performance improvement with breaking changes
- `BREAKING CHANGE:` - In commit body or footer
- `breaking:` - Explicit breaking change marker

**Examples:**
```bash
git commit -m "feat!: redesign analytics API interface"
git commit -m "fix!: remove deprecated trackEvent method"
git commit -m "refactor: restructure core classes

BREAKING CHANGE: AnalyticsManager constructor now requires context parameter"
```

### ✨ MINOR Version Bump (New Features)
- `feat:` - New feature (without breaking changes)
- `feature:` - Alternative feature syntax

**Examples:**
```bash
git commit -m "feat: add Firebase analytics provider"
git commit -m "feature: implement automatic screen tracking"
git commit -m "feat: support custom event parameters"
```

### 🐛 PATCH Version Bump (Bug Fixes & Improvements)
- `fix:` - Bug fix
- `bugfix:` - Alternative bug fix syntax
- `bug:` - Alternative bug syntax
- `docs:` - Documentation changes
- `style:` - Code style changes
- `refactor:` - Code refactoring (without breaking changes)
- `perf:` - Performance improvements (without breaking changes)
- `test:` - Test additions or updates
- `chore:` - Build process or auxiliary tool changes

**Examples:**
```bash
git commit -m "fix: resolve memory leak in provider cleanup"
git commit -m "docs: update installation instructions"
git commit -m "refactor: simplify event tracking logic"
git commit -m "perf: optimize reflection caching"
```

## 🎯 Best Practices

1. **Be Descriptive**: Write clear, concise commit messages
2. **Use Present Tense**: "add feature" not "added feature"
3. **Include Context**: Add scope when helpful (`feat(compose): add TrackScreenView`)
4. **Breaking Changes**: Always use `!` or `BREAKING CHANGE:` for API changes
5. **Group Related Changes**: Multiple fixes can go in one commit with appropriate type

## 🔍 Examples from Real Scenarios

### Scenario 1: API Redesign (Major)
```bash
# Current version: 1.2.3
git commit -m "feat!: redesign AnalyticsProvider interface

BREAKING CHANGE: trackEvent() now requires context parameter
- Remove deprecated trackScreen(String) method
- Add new trackScreen(Context, String, Map) method
- Update all existing providers to new interface"

# Result: Next snapshot will be 2.0.0-alpha.TIMESTAMP+COMMIT
```

### Scenario 2: New Feature (Minor)
```bash
# Current version: 1.2.3
git commit -m "feat: add Firebase Analytics provider

- Implement FirebaseAnalyticsProvider class
- Add Firebase dependency configuration
- Update documentation with setup instructions"

# Result: Next snapshot will be 1.3.0-alpha.TIMESTAMP+COMMIT
```

### Scenario 3: Bug Fix (Patch)
```bash
# Current version: 1.2.3
git commit -m "fix: resolve null pointer exception in fragment tracking

- Add null check for fragment lifecycle callbacks
- Improve error logging for debugging
- Add unit test for edge case"

# Result: Next snapshot will be 1.2.4-alpha.TIMESTAMP+COMMIT
```

## 🤖 Automatic Release Notes

The CI workflow automatically generates release notes based on:
- **Version Information**: Shows increment type and reasoning
- **Change Analysis**: Indicates what types of changes were detected
- **Commit Links**: Direct links to all commits since last release
- **Artifacts**: Lists all generated .jar and .aar files

## 📦 Snapshot Release Format

Snapshot releases follow this format:
```
MAJOR.MINOR.PATCH-alpha.YYYYMMDD.HHMMSS+COMMIT_HASH

Examples:
- 1.0.1-alpha.20250914.235959+abc1234 (patch)
- 1.1.0-alpha.20250914.235959+def5678 (minor)
- 2.0.0-alpha.20250914.235959+ghi9012 (major)
```

## 💡 Pro Tips

- Use `git commit --amend` to fix commit messages before pushing
- Squash commits when appropriate to create cleaner version increments
- Use conventional scopes: `feat(core):`, `fix(compose):`, `docs(readme):`
- Check existing commit history for consistency: `git log --oneline -10`

This system ensures that every change is properly versioned and documented automatically! 🚀