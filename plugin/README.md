# Analytics Annotation Gradle Plugin

This Gradle plugin provides automatic analytics tracking injection for Android applications using bytecode transformation. It processes classes annotated with `@TrackScreen` or `@TrackScreenComposable` and generates the necessary analytics tracking code at build time.

## Overview

The plugin uses ASM (Java bytecode manipulation framework) to automatically inject analytics tracking code into Android applications during the build process. This eliminates the need for manual boilerplate code while maintaining runtime flexibility through dynamic parameter resolution.

## Plugin Architecture

### Main Components

1. **AnalyticsPlugin** - Main plugin entry point and configuration
2. **AnalyticsPluginExtension** - DSL for plugin configuration
3. **AnalyticsClassVisitorFactory** - Core bytecode transformation engine
4. **Utils Package** - Supporting utilities for error reporting and validation

### Supported Targets
- **Activities**: Injects tracking after `super.onCreate()`
- **Fragments**: Injects tracking after `super.onViewCreated()`
- **Composables**: Injects tracking at function start

## Configuration

Add the plugin to your `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.shalan.analytics") version "1.0.0-SNAPSHOT"
}

analytics {
    enabled = true                    // Enable/disable plugin
    debugMode = true                 // Enable debug logging
    trackActivities = true           // Track Activities
    trackFragments = true            // Track Fragments
    trackComposables = true          // Track Composables
    
    // Optional: Include only specific packages
    includePackages = setOf("com.yourapp.package")
    
    // Optional: Exclude specific packages
    excludePackages = setOf("com.yourapp.internal")
}
```

## Usage Examples

### Simple Activity Tracking
```kotlin
@TrackScreen(screenName = "Main Screen")
class MainActivity : AppCompatActivity() {
    // Plugin automatically injects tracking after super.onCreate()
}
```

### Dynamic Parameters
```kotlin
@TrackScreen(
    screenName = "Profile Screen", 
    additionalParams = ["user_id", "user_type"]
)
class ProfileActivity : AppCompatActivity(), TrackedScreenParamsProvider {
    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf(
            "user_id" to getCurrentUserId(),
            "user_type" to getUserType()
        )
    }
}
```

### Composable Tracking
```kotlin
@TrackScreenComposable("Settings Screen")
@Composable
fun SettingsScreen() {
    // Plugin automatically injects tracking at function start
}
```

## Generated Code

The plugin generates a private method `__injectAnalyticsTracking()` in each annotated class:

```kotlin
// Generated for Activities/Fragments
private fun __injectAnalyticsTracking() {
    ScreenTracking.getManager().logScreenView(
        screenName = "Main Screen",
        screenClass = "MainActivity",
        parameters = ScreenTracking.createParametersMap(this, arrayOf("user_id", "user_name"))
    )
}

// Generated for Composables (static method)
private static fun __injectAnalyticsTracking() {
    ScreenTracking.getManager().logScreenView(
        screenName = "Composable Screen",
        screenClass = "MyComposableKt",
        parameters = Collections.emptyMap()
    )
}
```

## Dynamic Parameter Resolution

For classes implementing `TrackedScreenParamsProvider`, the plugin generates bytecode that:

1. Checks if the instance implements `TrackedScreenParamsProvider`
2. Calls `getTrackedScreenParams()` to get runtime values
3. Filters parameters based on `additionalParams` array
4. Passes the filtered map to the analytics manager

This approach ensures that:
- Static parameter keys are defined at compile time
- Actual values are resolved at runtime
- Only requested parameters are included
- Analytics never crashes the app (try-catch protection)

## Processing Flow

### 1. Class Filtering
- Skips system classes (android.*, androidx.*, java.*, kotlin.*)
- Applies include/exclude package filters
- Only processes enabled class types

### 2. Annotation Discovery
- Scans for `@TrackScreen` and `@TrackScreenComposable` annotations
- Extracts annotation parameters (screenName, screenClass, additionalParams)
- Determines class type (Activity/Fragment/Composable)

### 3. Method Injection
- **Activities**: Injects after `super.onCreate(savedInstanceState)`
- **Fragments**: Injects after `super.onViewCreated(view, savedInstanceState)`
- **Composables**: Injects at the beginning of the function

### 4. Bytecode Generation
- Creates `__injectAnalyticsTracking()` method
- Generates appropriate method access modifiers (private/static)
- Handles parameter map creation and method calls

## Debug Features

Enable debug mode to see detailed processing information:

```kotlin
analytics {
    debugMode = true
}
```

Debug output includes:
- Class processing decisions
- Annotation discovery
- Method instrumentation details
- Bytecode generation steps

## Error Handling

The plugin includes robust error handling:
- **Transformation Errors**: Logged via `ErrorReporter` without stopping the build
- **Runtime Safety**: Generated code includes try-catch blocks
- **Graceful Degradation**: Missing annotations or providers don't crash the app

## Build Integration

The plugin integrates with the Android Gradle Plugin's transformation pipeline:
- Runs after Kotlin/Java compilation
- Processes bytecode before DEX generation
- Compatible with R8/ProGuard optimization
- Supports incremental builds

## Performance Considerations

- **Minimal Overhead**: Only processes annotated classes
- **Deferred Transformation**: Annotations processed only when needed
- **Optimized Bytecode**: Generates minimal additional instructions
- **Build Time**: Negligible impact on build performance

## Testing

The plugin includes comprehensive tests:
- Unit tests for plugin configuration
- Integration tests for bytecode transformation
- Validation of generated analytics calls

Run tests with:
```bash
./gradlew :plugin:test
```

## Publishing

Build and publish the plugin:
```bash
# Clean and build
./gradlew :plugin:clean :plugin:build

# Publish to Maven Local for testing
./gradlew :plugin:publishToMavenLocal

# Clean app module after plugin changes
./gradlew :app:clean :app:build
```

## Integration with Core Library

The plugin works in conjunction with the core analytics library:
- Calls `ScreenTracking.getManager()` for analytics manager access
- Uses `ScreenTracking.createParametersMap()` for parameter resolution
- Integrates with `TrackedScreenParamsProvider` interface
- Respects `AnalyticsManager` configuration and providers

## Troubleshooting

### Common Issues

1. **Plugin Not Applied**: Ensure plugin is properly configured in build.gradle.kts
2. **Missing Tracking**: Check that classes are in included packages
3. **Build Errors**: Enable debug mode to see transformation details
4. **Runtime Crashes**: Verify core library is properly initialized

### Debug Steps

1. Enable debug mode in plugin configuration
2. Check build logs for transformation messages
3. Verify generated bytecode with tools like `javap` or ASM Bytecode Viewer
4. Test with sample app to isolate issues

This plugin provides a powerful and flexible solution for automatic analytics tracking in Android applications, reducing boilerplate code while maintaining full control over tracking behavior.