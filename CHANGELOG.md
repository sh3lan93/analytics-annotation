# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2024-11-06

### 🚜 Refactor

- **BREAKING**: Remove Jetpack Compose tracking functionality - The entire `:compose` module, `@TrackScreenComposable` annotation, and related Compose-specific instrumentation have been removed to focus the library on bytecode transformation for Activities and Fragments
- Remove `trackActivities` and `trackFragments` from extension - These configuration options are now obsolete as the presence of a `@TrackScreen` annotation is sufficient to enable tracking
- **Centralize instrumentation logic** and add helper classes:
  - Introduced `AnalyticsConstants.kt` for centralized constant management
  - Introduced `AnnotationMetadata.kt` for type-safe annotation metadata handling
  - Introduced `AnnotationExtractor.kt` for clean annotation parameter parsing
  - Introduced `ClassTypeDetector.kt` for Activity/Fragment type detection
  - Introduced `MethodInstrumentationStrategy.kt` for instrumentation strategy selection
  - Introduced `LifecycleInstrumentingMethodVisitor.kt` for lifecycle method injection
  - Introduced `TrackingLogger.kt` for optimized lazy-evaluation logging
- **Centralize error handling** with a global `errorHandler` - Moved `errorHandler` from `MethodTrackingConfig` to `AnalyticsConfig` level for unified analytics error management across screen tracking, event logging, and method tracking

### 📚 Documentation

- Update README with simplified configuration - Removed references to Compose, removed obsolete configuration properties (`trackActivities`, `trackFragments`, `trackComposables`), and updated examples to reflect current API

## [1.0.1] - 2025-10-03

### 🚀 Features

- Introduce `TrackScreenOnce` composable for reliable screen tracking
- Introduce TrackScreenHelper for ASM-injected code

### ⚙️ Miscellaneous Tasks

- Add extensive KDoc documentation to `AnalyticsClassVisitorFactory`
- Bump version to 1.0.1
- Bump plugin version to 1.0.1

### Chore
- Bump version to 1.0.1

### Refactor

- **compose**: Use `TrackScreenOnce` for Composable screen tracking
- Simplify tracking method injection with a helper class
- Remove unused variable in `AnalyticsClassVisitorFactory`
- Remove unused class properties in `AnalyticsClassVisitor`
- Relocate `TrackingAnnotationInfo` and remove `AnnotationScanner`

## [1.0.0] - 2025-09-24

### 🚀 Features

- Add `ExcludeActivity` for annotation testing
- Implement ASM-based class instrumentation for analytics
- Add .env to .gitignore
- Implement method tracking instrumentation
- Add integration tests for Analytics Plugin and core DSL
- Configure Maven publishing for compose module

### 🐛 Bug Fixes

- Add 'plans' directory to .gitignore

### ⚙️ Miscellaneous Tasks

- Remove LogcatAnalyticsProvider
- Add TestAnalyticsManager for verifying screen view events
- Add comprehensive EXAMPLES.md and build benchmark script
- Update README with plugin details and project structure
- Update Android Test Runner and disable comment wrapping lint rule
- Remove lifecycle approach screen tracking for Activities and Fragments
- Introduce annotations for event tracking
- Introduce ParameterSerializer interface
- Introduce JSON and Primitive Parameter Serializers
- Introduce method-level tracking with @Trackable and @Track
- Add method tracking configuration to Gradle plugin
- Add comprehensive method tracking examples and custom serializers
- Update README and dependencies for method-level tracking
- Add convention plugins for library builds
- Introduce Gradle convention plugins for build configuration
- Add CONTRIBUTING.md
- Add COMMIT_CONVENTIONS.md for automated versioning
- Change annotation retention to BINARY
- Change annotation retention to BINARY
- Update minimum API level and fix code block language
- Remove --no-daemon from Gradle tasks in CI
- Harden GitHub Actions workflow permissions
- Enhance PR checks workflow
- Update GitHub Actions and improve security for PRs
- Update relative links to absolute URLs in README
- Remove separate dependency check job

### Build

- Add Maven Publish plugin to root build.gradle

### Chore

- Configure Maven publishing for core library

### Docs

- Add KDoc for @TrackScreenComposable and bump minSdk
- Add Apache License 2.0
- Update README for 1.0.0 release
- Update README with new group ID and artifact names
- Update README with correct Compose dependency and license
- Update README with `analyticsConfig` DSL and global parameters
- Update links in README.md and EXAMPLES.md
- Update README

### Feat

- Enhance code documentation for screen type detection
- Introduce `MethodTrackingManager` for ASM-instrumented event tracking
- Configure Maven publishing for annotation module
- Add comprehensive PR checks workflow

### Refactor

- Simplify screen tracking by removing flow context
- Remove TestAnalyticsManager
- Simplify screen tracking and plugin integration
- Introduce PluginLogger for centralized logging
- Rename Gradle plugin ID
- Introduce Method Tracking Configuration and Initialization
- Update plugin metadata and group ID
- Remove TrackScreenView composable

### Test

- Add unit tests for method tracking configuration and manager

<!-- generated by git-cliff -->
