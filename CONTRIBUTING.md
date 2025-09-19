# Contributing to Analytics Annotation Library

Thank you for your interest in contributing to the Analytics Annotation Library! This document provides guidelines and information for contributors.

## 🤝 How to Contribute

We welcome contributions of all kinds:
- 🐛 **Bug Reports** - Help us identify and fix issues
- 💡 **Feature Requests** - Suggest new functionality
- 📝 **Documentation** - Improve or add documentation
- 🔧 **Code Contributions** - Fix bugs or implement features
- 🧪 **Testing** - Add or improve test coverage
- 📦 **Examples** - Provide usage examples

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK) 17+**
- **Android SDK** with API level 24+ support
- **Git** for version control

### Development Setup

1. **Fork and Clone**
   ```bash
   git clone https://github.com/YOUR_USERNAME/analytics-annotation.git
   cd analytics-annotation
   ```

2. **Initial Build**
   ```bash
   ./gradlew build
   ```

3. **Verify Setup**
   ```bash
   ./gradlew test
   ./gradlew ktlintCheck
   ```

### Project Structure

The project is organized as a multi-module Gradle project:

```
analytics-annotation/
├── annotation/     # @TrackScreen, @Trackable, @Track, @Param annotations
├── core/          # Core tracking logic, providers, lifecycle callbacks
├── compose/       # Jetpack Compose integration (@TrackScreenComposable)
├── plugin/        # Gradle plugin for bytecode injection
├── app/          # Sample application with usage examples
├── build-logic/  # Convention plugins for build configuration
└── .github/      # CI/CD workflows and commit conventions
```

### Module Responsibilities

- **`:annotation`** - Annotation definitions only, minimal dependencies
- **`:core`** - Main analytics functionality, lifecycle management, provider pattern
- **`:compose`** - Compose-specific tracking utilities and composables
- **`:plugin`** - Gradle plugin for compile-time bytecode transformation
- **`:app`** - Sample app demonstrating all library features

## 🔧 Development Workflow

### Building

```bash
# Build all modules
./gradlew build

# Build specific modules
./gradlew :core:build
./gradlew :plugin:build
./gradlew :app:build
```

### Testing

```bash
# Run all unit tests
./gradlew test

# Run tests for specific modules
./gradlew :core:test
./gradlew :compose:test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedCheck

# Run specific instrumented tests
./gradlew :compose:connectedAndroidTest
```

### Code Quality

```bash
# Check code style
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Check for dependency updates
./gradlew dependencyUpdates
```

### Plugin Development

```bash
# Build and publish plugin locally for testing
./gradlew :plugin:publishToMavenLocal

# Test plugin with sample app
./gradlew :app:clean :app:build

# Run plugin-specific tests
./gradlew :plugin:test
```

### Sample App Testing

```bash
# Install and run sample app
./gradlew :app:installDebug
./gradlew :app:run
```

## 📝 Coding Standards

### Code Style

- **Kotlin**: Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Formatting**: Use ktlint for automatic formatting (`./gradlew ktlintFormat`)
- **Naming**: Use descriptive names for classes, methods, and variables
- **Comments**: Add KDoc for all public APIs

### Architecture Principles

1. **Simplicity First** - APIs should be intuitive and easy to understand
2. **Zero Runtime Crashes** - Analytics should never cause app crashes
3. **Minimal Overhead** - Use reflection caching and lazy initialization
4. **Testability** - Every component should be easily testable in isolation
5. **Documentation** - Every public API must have KDoc comments

### Key Design Patterns

- **Provider Pattern** - `AnalyticsProvider` interface for pluggable analytics services
- **Manager Pattern** - `AnalyticsManager` for coordinating multiple providers
- **Lifecycle Integration** - Automatic tracking via Android lifecycle callbacks
- **DSL Configuration** - Kotlin DSL for library initialization

### Error Handling

- Always use try-catch blocks around analytics code
- Log errors appropriately but never crash the host application
- Provide fallback behavior for critical operations

## 🧪 Testing Guidelines

### Unit Tests

- Use **JUnit 4** and **Mockk** for mocking
- Use **Robolectric** for Android-specific unit tests
- Aim for high test coverage, especially for core functionality
- Test both success and failure scenarios

### Integration Tests

- Use **Instrumented Tests** for lifecycle behavior verification
- Test real Android components when possible
- Use test doubles like `TestAnalyticsManager` for controlled environments

### Test Organization

```kotlin
class ExampleTest {
    // Given - setup test data and conditions
    // When - execute the action being tested
    // Then - verify the expected outcome
}
```

## 📋 Pull Request Process

### Before Submitting

1. **Create an Issue** - Discuss the change before starting work (for significant changes)
2. **Create a Branch** - Use descriptive branch names (`feature/add-firebase-provider`, `fix/memory-leak`)
3. **Follow Commit Conventions** - See [Commit Message Guidelines](#commit-message-guidelines)
4. **Run Tests** - Ensure all tests pass locally
5. **Check Code Style** - Run `./gradlew ktlintCheck`

### Pull Request Checklist

- [ ] **Tests Added/Updated** - Include tests for new functionality
- [ ] **Documentation Updated** - Update README, KDoc, or examples as needed
- [ ] **Code Style** - Follows project coding standards
- [ ] **Backward Compatibility** - Doesn't break existing APIs (unless breaking change)
- [ ] **Performance** - No significant performance regressions
- [ ] **Sample App** - Update sample app if demonstrating new features

### PR Description Template

```markdown
## Summary
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)
```

## 📝 Commit Message Guidelines

This project uses [Conventional Commits](https://conventionalcommits.org/) for automatic versioning. Please follow our [commit conventions](.github/COMMIT_CONVENTIONS.md).

### Quick Reference

**Format:** `<type>[optional scope]: <description>`

**Types:**
- `feat:` - New feature (minor version bump)
- `fix:` - Bug fix (patch version bump)
- `feat!:` or `BREAKING CHANGE:` - Breaking change (major version bump)
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Test additions/updates
- `chore:` - Build process changes

**Examples:**
```bash
feat: add Firebase analytics provider
fix: resolve memory leak in fragment tracking
feat!: redesign AnalyticsProvider interface
docs: update installation instructions
```

## 🐛 Bug Reports

When reporting bugs, please include:

### Bug Report Template

```markdown
**Describe the Bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. See error

**Expected Behavior**
A clear description of what you expected to happen.

**Environment:**
- Library Version: [e.g. 1.0.0]
- Android Version: [e.g. API 28]
- Device: [e.g. Pixel 6]
- Gradle Version: [e.g. 8.1.1]

**Logs/Stack Trace**
```
Include relevant logs or stack traces
```

**Additional Context**
Add any other context about the problem here.
```

## 💡 Feature Requests

For feature requests, please include:

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
A clear description of what the problem is.

**Describe the solution you'd like**
A clear description of what you want to happen.

**Describe alternatives you've considered**
Other solutions or features you've considered.

**Additional context**
Any other context or screenshots about the feature request.

**Implementation Ideas**
If you have ideas about how this could be implemented.
```

## 📄 Code of Conduct

### Our Standards

- **Be Respectful** - Treat everyone with respect and kindness
- **Be Collaborative** - Work together constructively
- **Be Inclusive** - Welcome people of all backgrounds and experience levels
- **Be Patient** - Help others learn and grow

### Unacceptable Behavior

- Harassment or discrimination of any kind
- Trolling, insulting, or derogatory comments
- Publishing others' private information
- Any conduct that could reasonably be considered inappropriate

## 📄 License

By contributing to this project, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE.md).

## 🙋‍♂️ Getting Help

- **Documentation** - Check [README.md](README.md) and [EXAMPLES.md](EXAMPLES.md)
- **Issues** - Search existing [GitHub Issues](https://github.com/sh3lan93/analytics-annotation/issues)
- **Plugin Details** - See [plugin/README.md](plugin/README.md) for plugin-specific documentation

## 🎉 Recognition

Contributors will be recognized in:
- **Release Notes** - Mentioned in changelog for their contributions
- **README** - Contributors section (coming soon)
- **GitHub** - Automatically tracked in repository insights

Thank you for contributing to the Analytics Annotation Library! 🚀

---

**Questions?** Feel free to open an issue for any questions about contributing.