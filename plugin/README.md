# Easy Analytics Gradle Plugin

This Gradle plugin provides automatic analytics tracking injection for Android applications using bytecode transformation. It processes classes annotated with `@TrackScreen`, `@TrackScreenComposable`, or `@Trackable` and generates the necessary analytics tracking code at build time.

## Overview

The plugin uses ASM (Java bytecode manipulation framework) to automatically inject analytics tracking code into Android applications during the build process. This eliminates the need for manual boilerplate code while maintaining runtime flexibility through dynamic parameter resolution.

## Plugin Architecture

### Main Components

1. **AnalyticsPlugin** - Main plugin entry point and configuration
2. **AnalyticsPluginExtension** - DSL for plugin configuration
3. **AnalyticsClassVisitorFactory** - Core bytecode transformation engine
4. **Utils Package** - Supporting utilities for error reporting and validation

### Supported Targets
- **Activities**: Injects screen tracking after `super.onCreate()`
- **Fragments**: Injects screen tracking after `super.onViewCreated()`
- **Composables**: Injects screen tracking at function start
- **Methods**: Injects event tracking at method entry for `@Track` annotated methods in `@Trackable` classes

## Configuration

Add the plugin to your `app/build.gradle.kts`:

```kotlin
plugins {
    id("dev.moshalan.easyanalytics") version "1.0.0"
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
    
    // Method tracking configuration
    methodTrackingEnabled = true     // Enable method-level tracking
    maxParametersPerMethod = 10      // Max parameters to track per method
    excludeMethods = setOf("toString", "hashCode", "equals")
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

### Method-Level Event Tracking
Track individual method calls with parameters using `@Trackable` and `@Track`:

```kotlin
@Trackable  // Marks class as containing @Track methods
class UserViewModel : ViewModel() {
    
    @Track(eventName = "user_profile_loaded", includeGlobalParams = true)
    fun loadUserProfile(
        @Param("user_id") userId: String,
        @Param("source") source: String
    ) {
        // Method implementation
        // Analytics call is automatically injected at method start
    }
    
    @Track(eventName = "settings_updated", includeGlobalParams = false)
    fun updateSettings(@Param("settings") settings: UserSettings) {
        // Complex objects are automatically serialized to JSON
    }
}
```

### Method Tracking in Composables
```kotlin
@Trackable
object ComposableTracker {
    @Track(eventName = "button_clicked", includeGlobalParams = true)
    fun handleButtonClick(
        @Param("button_id") buttonId: String,
        @Param("screen_context") context: String
    ) {
        // Button click logic
    }
}

@Composable
fun MyScreen() {
    Button(onClick = { 
        ComposableTracker.handleButtonClick("save_button", "profile_screen")
    }) {
        Text("Save")
    }
}
```

### Parameter Limit Configuration
The plugin respects the `maxParametersPerMethod` configuration to avoid performance issues:

```kotlin
@Trackable
class DataProcessor {
    // This method has many parameters, but plugin will only track first 10 (default limit)
    @Track(eventName = "complex_processing", includeGlobalParams = true)
    fun processComplexData(
        @Param("param1") p1: String,
        @Param("param2") p2: String,
        @Param("param3") p3: String,
        @Param("param4") p4: String,
        @Param("param5") p5: String,
        @Param("param6") p6: String,
        @Param("param7") p7: String,
        @Param("param8") p8: String,
        @Param("param9") p9: String,
        @Param("param10") p10: String,
        @Param("param11") p11: String,  // This will be ignored (beyond limit)
        @Param("param12") p12: String,  // This will be ignored (beyond limit)
        nonTrackedParam: String         // Not annotated, so ignored
    ) {
        // Only first 10 @Param annotated parameters will be tracked
        // Configure via: maxParametersPerMethod = 15 to track more
    }
}
```

## Generated Code

### Screen Tracking
The plugin generates a private method `__injectAnalyticsTracking()` in each screen-annotated class:

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

### Method Tracking
For `@Track` annotated methods, the plugin injects analytics calls at method entry:

```kotlin
// Original method
@Track(eventName = "user_profile_loaded", includeGlobalParams = true)
fun loadUserProfile(@Param("user_id") userId: String, @Param("source") source: String) {
    // Method implementation
}

// Becomes (conceptually):
fun loadUserProfile(userId: String, source: String) {
    // Injected tracking call
    MethodTrackingManager.track(
        "user_profile_loaded",
        mapOf("user_id" to userId, "source" to source),
        true
    )
    
    // Original method implementation
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
- Scans for `@TrackScreen`, `@TrackScreenComposable`, and `@Trackable` annotations
- Extracts annotation parameters (screenName, screenClass, additionalParams for screen tracking)
- Identifies `@Track` annotated methods within `@Trackable` classes
- Processes `@Param` annotations on method parameters
- Determines class type (Activity/Fragment/Composable/Trackable)

### 3. Method Injection
- **Activities**: Injects screen tracking after `super.onCreate(savedInstanceState)`
- **Fragments**: Injects screen tracking after `super.onViewCreated(view, savedInstanceState)`
- **Composables**: Injects screen tracking at the beginning of the function
- **@Track Methods**: Injects event tracking calls at method entry with parameter collection

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