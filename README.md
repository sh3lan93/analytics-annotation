# Easy-Annotation Android Gradle Plugin

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/sh3lan93/analytics-annotation)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE.md)
[![Version](https://img.shields.io/badge/version-1.0.0-brightgreen.svg)](https://github.com/sh3lan93/analytics-annotation/releases)

A powerful, annotation-based tracking Gradle plugin for Android that eliminates boilerplate code by automatically injecting analytics tracking at compile time using bytecode transformation.

## ✨ Features

- **🎯 Zero Boilerplate**: Just add annotations - no manual lifecycle management
- **⚡ Compile-Time Safety**: Bytecode injection using ASM and modern AGP APIs
- **🏗️ Architecture Agnostic**: Works with Activities, Fragments, Jetpack Compose, and ViewModels
- **📊 Method-Level Tracking**: Track individual method calls with parameters and custom serialization
- **🚀 High Performance**: Minimal build overhead with incremental build support
- **🔧 Highly Configurable**: Fine-grained control over tracking behavior via Gradle plugin
- **🧪 Testing Friendly**: Built-in debug providers and test utilities
- **📱 Modern Android**: Supports API 26+ with latest Android development practices

## 🚀 Quick Start

### 1. Apply the Plugin

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.moshalan.easyanalytics") version "1.0.0"
}

// Configure the analytics plugin
analytics {
    enabled = true
    debugMode = false //true if you are interested in see the debug messages while doing manipulation 
    trackActivities = true
    trackFragments = true 
    trackComposables = true
}

dependencies {
    implementation("dev.moshalan:easy-analytics-core:1.0.0")
    implementation("dev.moshalan:easy-analytics-compose:1.0.0") // For Compose support
}
```

### 2. Define the Analytics Provider
```kotlin
class FirebaseAnalyticsProvider(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsProvider {
    
    override fun logEvent(eventName: String, parameters: Map<String, Any>) {
        val bundle = Bundle().apply {
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }
}
```

### 3. Initialize in Your Application

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config = analyticsConfig {
                debugMode = BuildConfig.DEBUG
                providers.add(DebugAnalyticsProvider())
                // Add your analytics providers here
            }
        )
    }
}
```

### 4. Annotate Your Screens

#### Activities
```kotlin
@TrackScreen(screenName = "Home Screen")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Analytics tracking is automatically injected after super.onCreate()!
    }
}
```

#### Fragments
```kotlin
@TrackScreen(screenName = "Profile Screen")
class ProfileFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Analytics tracking is automatically injected after super.onViewCreated()!
    }
}
```

#### Jetpack Compose
```kotlin
@TrackScreenComposable(screenName = "Settings Screen")
@Composable
fun SettingsScreen() {
    // Analytics tracking is automatically injected at function start!
    Column {
        Text("Settings")
        // Your UI content
    }
}
```

#### Method-Level Tracking
Track individual method calls with parameters in any class (ViewModels, Services, etc.):

```kotlin
@Trackable  // Mark class as containing @Track methods
class UserViewModel : ViewModel() {
    
    @Track(eventName = "user_profile_loaded", includeGlobalParams = true)
    fun loadUserProfile(
        @Param("user_id") userId: String,
        @Param("source") source: String
    ) {
        // Method implementation
        // Analytics call is automatically injected at method start!
    }
    
    @Track(eventName = "settings_changed", includeGlobalParams = false)
    fun updateSettings(@Param("settings") settings: UserSettings) {
        // Complex objects are automatically serialized
    }
}
```

## 📚 Documentation

### Core Concepts

#### Automatic Injection
The plugin automatically injects tracking code at compile time using bytecode transformation with ASM:

- **Activities**: Code injected after `super.onCreate(savedInstanceState)`
- **Fragments**: Code injected after `super.onViewCreated(view, savedInstanceState)`  
- **Composables**: Tracking call injected at function start
- **Method Tracking**: Calls injected at the beginning of `@Track` annotated methods

#### Compile-Time Transformation
The Gradle plugin approach is that:

- Scans for `@TrackScreen`, `@TrackScreenComposable`, and `@Trackable` annotations during build
- Generates bytecode to inject analytics tracking calls  
- Provides zero runtime overhead for tracking
- Supports incremental builds for fast compilation

### Configuration

#### Plugin Configuration
```kotlin
analytics {
    enabled = true                    // Enable/disable plugin
    debugMode = true                  // Verbose logging
    trackActivities = true            // Track Activities
    trackFragments = true             // Track Fragments  
    trackComposables = true           // Track Composables
    
    // Package filtering for performance
    includePackages = setOf(
        "com.myapp.features",
        "com.myapp.screens"
    )
    
    excludePackages = setOf(
        "com.myapp.internal",
        "com.myapp.testing"
    )
}
```

#### Runtime Configuration
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config = analyticsConfig {
                debugMode = BuildConfig.DEBUG

                // Add analytics providers
                providers.add(DebugAnalyticsProvider())
                providers.add(FirebaseAnalyticsProvider()) // Custom provider

                // Configure method tracking (optional)
                methodTracking {
                    enabled = true
                    errorHandler = { throwable ->
                        Log.e("Analytics", "Method tracking error", throwable)
                    }
                }
            }
        )

        // Set global parameters (optional)
        ScreenTracking.setGlobalParameters(mapOf(
            "app_version" to BuildConfig.VERSION_NAME,
            "build_type" to BuildConfig.BUILD_TYPE
        ))
    }
}
```

### Debugging

#### Debug Mode
Enable debug mode to see detailed transformation logs:

```kotlin
analytics {
    debugMode = true
}
```

Sample debug output:
```text
DEBUG: AnalyticsClassVisitor: Found @TrackScreen annotation on class com.myapp.MainActivity
DEBUG: AnalyticsClassVisitor: Injecting Activity tracking for "Home Screen"
DEBUG: AnalyticsClassVisitor: Generated tracking method successfully
```

## 🏗️ Architecture

### Module Structure
```
analytics-annotation/
├── annotation/     # Annotations (@TrackScreen, @Trackable, @Track, @Param)
├── core/          # Core tracking logic, providers, and method tracking
├── compose/       # Jetpack Compose integration (@TrackScreenComposable)
├── plugin/        # Gradle plugin for bytecode injection
└── app/          # Sample application with examples
```

### Build Integration
The plugin integrates seamlessly with Android builds:

1. **Annotation Scanning**: Detects `@TrackScreen`, `@TrackScreenComposable`, and `@Trackable` annotations
2. **Bytecode Transformation**: Injects tracking calls using ASM
3. **Method Instrumentation**: Processes `@Track` methods with parameter serialization
4. **Incremental Support**: Only processes changed classes
5. **Cache Compatible**: Works with Gradle build cache

## ⚡ Performance

Performance benchmarks on the sample app:

| Metric | Result | Status |
|--------|--------|--------|
| Build Time Impact | Minimal | ✅ Excellent |
| Incremental Build | Supported | ✅ Working |
| Cache Compatibility | Yes | ✅ Working |
| Memory Overhead | Minimal | ✅ Minimal |

The plugin only processes annotated classes during build time, ensuring minimal performance impact.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup
```bash
git clone https://github.com/shalan/analytics-annotation.git
cd analytics-annotation
./gradlew build
```

### Plugin Development
```bash
# Build and publish plugin locally
./gradlew :plugin:publishToMavenLocal

# Test plugin with sample app
./gradlew :app:clean :app:build
```

### Running Tests
```bash
./gradlew test                    # Unit tests
./gradlew connectedCheck         # Integration tests  
./gradlew :plugin:test           # Plugin tests
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/sh3lan93/analytics-annotation/blob/main/LICENSE.md) file for details.

## 🔗 Links

- [Sample App](app/) - Complete example implementation
- [Examples](https://github.com/sh3lan93/analytics-annotation/blob/main/EXAMPLES.md) - Comprehensive usage examples
- [Plugin Documentation](https://github.com/sh3lan93/analytics-annotation/blob/main/plugin/README.md) - Detailed plugin documentation
- [Issues](https://github.com/sh3lan93/analytics-annotation/issues) - Report bugs and request features

## 📞 Support

- **Examples**: See [EXAMPLES.md](https://github.com/sh3lan93/analytics-annotation/blob/main/EXAMPLES.md) for comprehensive usage examples
- **Issues**: Report bugs on [GitHub Issues](https://github.com/sh3lan93/analytics-annotation/issues)
- **Plugin Details**: Check [plugin/README.md](https://github.com/sh3lan93/analytics-annotation/blob/main/plugin/README.md) for detailed plugin documentation

---

**Made with ❤️ for the Android community**