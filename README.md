# Easy-Annotation Android Gradle Plugin

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/sh3lan93/analytics-annotation)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE.md)
[![Version](https://img.shields.io/badge/version-1.0.1-brightgreen.svg)](https://github.com/sh3lan93/analytics-annotation/releases)

A powerful, annotation-based tracking Gradle plugin for Android that eliminates boilerplate code by automatically injecting analytics tracking at compile time using bytecode transformation.

## ✨ Features

- **🎯 Zero Boilerplate**: Just add annotations - no manual lifecycle management
- **⚡ Compile-Time Safety**: Bytecode injection using ASM and modern AGP APIs
- **🏗️ Architecture Agnostic**: Works with Activities, Fragments, ViewModels, and more
- **📊 Method-Level Tracking**: Track individual method calls with parameters and custom serialization
- **🚀 High Performance**: Minimal build overhead with incremental build support
- **🔧 Highly Configurable**: Fine-grained control over tracking behavior via Gradle plugin
- **🧪 Testing Friendly**: Built-in debug providers and test utilities
- **📱 Modern Android**: Supports API 26+ with latest Android development practices

## 🚀 Quick Start

### 1. Apply the Plugin

**For single module projects:**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.moshalan.easyanalytics") version "1.0.1"
}

// Configure the analytics plugin
analytics {
    enabled = true
    debugMode = false // true if you want to see debug messages during bytecode transformation
}

dependencies {
    implementation("dev.moshalan:easy-analytics-core:1.0.1")
}
```

**For multi-module projects:**

Apply the plugin only to your app module. Feature/library modules just need the dependencies:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.moshalan.easyanalytics") version "1.0.1"  // Apply plugin here
}

analytics {
    enabled = true
    debugMode = false
}

dependencies {
    implementation(project(":feature:home"))  // Your feature modules
    implementation("dev.moshalan:easy-analytics-core:1.0.1")
}
```

```kotlin
// feature/home/build.gradle.kts (Library Module)
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Don't apply the analytics plugin in library modules
}

dependencies {
    implementation("dev.moshalan:easy-analytics-core:1.0.1")
    implementation("dev.moshalan:easy-analytics-annotation:1.0.1")
}
```

The plugin will automatically instrument all `@TrackScreen` annotations in your feature modules when building the app.

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

    override fun setUserId(userId: String?) {
        // Set user ID in Firebase Analytics
    }

    override fun setUserProperty(key: String, value: String) {
        // Set user properties in Firebase Analytics
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
                providers.add(InMemoryDebugAnalyticsProvider())
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
- **Method Tracking**: Calls injected at the beginning of `@Track` annotated methods

#### Compile-Time Transformation
The Gradle plugin approach is that:

- Scans for `@TrackScreen` and `@Trackable` annotations during build
- Generates bytecode to inject analytics tracking calls
- Provides zero runtime overhead for tracking
- Supports incremental builds for fast compilation

### Configuration

#### Plugin Configuration
```kotlin
analytics {
    enabled = true                    // Enable/disable plugin
    debugMode = true                  // Verbose logging during bytecode transformation

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
                providers.add(InMemoryDebugAnalyticsProvider())
                providers.add(FirebaseAnalyticsProvider()) // Custom provider

                // Global error handler for all analytics operations
                errorHandler = { throwable ->
                    Log.e("Analytics", "Analytics error", throwable)
                }

                // Configure method tracking (optional)
                methodTracking {
                    enabled = true
                    customSerializers.add(MyCustomParameterSerializer())
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
├── plugin/        # Gradle plugin for bytecode injection
└── app/          # Sample application with examples
```

### Build Integration
The plugin integrates seamlessly with Android builds:

1. **Annotation Scanning**: Detects `@TrackScreen` and `@Trackable` annotations
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

## 🧪 Testing Analytics

Testing analytics tracking is essential to ensure your analytics events are properly captured. This library provides several built-in mechanisms and testing utilities to verify your analytics implementation.

### 1. Using Debug Providers in Development

The easiest way to test analytics during development is to use the built-in debug providers in your development build:

#### DebugAnalyticsProvider
Logs all tracking events to Logcat for quick verification:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config = analyticsConfig {
                debugMode = BuildConfig.DEBUG

                // Add debug provider to see events in Logcat and verify in tests
                providers.add(InMemoryDebugAnalyticsProvider())

                // Add your production provider
                if (!BuildConfig.DEBUG) {
                    providers.add(FirebaseAnalyticsProvider(Firebase.analytics))
                }
            }
        )
    }
}
```

When you navigate through your app, you'll see output like:
```
D/AnalyticsProvider: Screen tracked: Home Screen with parameters: {category=all, sort=name}
D/AnalyticsProvider: Screen tracked: Product Details with parameters: {product_id=123, price=99.99}
```

### 2. Unit Testing with Mock Providers

Test your annotated classes in isolation using mock providers:

```kotlin
class AnalyticsTest {

    @Test
    fun `verify home screen tracking with parameters`() {
        // Arrange
        val mockProvider = mockk<AnalyticsProvider>()
        ScreenTracking.initialize(
            config = analyticsConfig {
                providers.add(mockProvider)
            }
        )

        // Act
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Assert
        verify(exactly = 1) {
            mockProvider.trackScreen("Home Screen", emptyMap())
        }
    }

    @Test
    fun `verify product details tracking with dynamic parameters`() {
        // Arrange
        val mockProvider = mockk<AnalyticsProvider>()
        ScreenTracking.initialize(
            config = analyticsConfig {
                providers.add(mockProvider)
            }
        )

        val testProduct = Product(
            id = "123",
            name = "Test Product",
            price = 99.99
        )

        val intent = Intent(applicationContext, ProductDetailsActivity::class.java).apply {
            putExtra("product", testProduct)
        }

        // Act
        val activity = Robolectric.buildActivity(ProductDetailsActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .get()

        // Assert
        verify(exactly = 1) {
            mockProvider.trackScreen(
                "Product Details",
                match { params ->
                    params["product_id"] == "123" &&
                    params["product_name"] == "Test Product" &&
                    params["price"] == 99.99
                }
            )
        }
    }
}
```

### 3. Integration Testing with Debug Provider

Verify analytics tracking in integration tests using the in-memory debug provider:

```kotlin
class AnalyticsIntegrationTest {

    private lateinit var debugProvider: InMemoryDebugAnalyticsProvider

    @Before
    fun setup() {
        debugProvider = InMemoryDebugAnalyticsProvider()
        ScreenTracking.initialize(
            config = analyticsConfig {
                providers.add(debugProvider)
            }
        )
    }

    @Test
    fun `verify navigation flow tracks all screens`() {
        // Act
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Navigate to profile
        activity.findViewById<Button>(R.id.profile_button).performClick()

        // Assert
        val events = debugProvider.getLoggedEvents()
        assertEquals(2, events.size)

        assertEquals("screen_view", events[0].eventName)
        assertEquals("screen_view", events[1].eventName)
        assertEquals("Home Screen", events[0].parameters["screen_name"])
        assertEquals("Profile Screen", events[1].parameters["screen_name"])
    }

    @Test
    fun `verify custom parameters are tracked correctly`() {
        // Arrange
        val customParams = mapOf(
            "user_id" to "user_123",
            "subscription_type" to "premium"
        )

        // Act
        ScreenTracking.trackScreen("User Profile", customParams)

        // Assert
        val events = debugProvider.getLoggedEvents()
        assertThat(events).isNotEmpty()
        val lastEvent = events.last()
        assertEquals("screen_view", lastEvent.eventName)
        assertEquals("User Profile", lastEvent.parameters["screen_name"])
        assertEquals("user_123", lastEvent.parameters["user_id"])
        assertEquals("premium", lastEvent.parameters["subscription_type"])
    }
}
```

### 4. Testing Fragments with Parameters

Verify that Fragment parameters are properly tracked:

```kotlin
@Test
fun `verify fragment with parameters tracking`() {
    // Arrange
    val debugProvider = InMemoryDebugAnalyticsProvider()
    ScreenTracking.initialize(
        config = analyticsConfig {
            providers.add(debugProvider)
        }
    )

    // Act
    val fragment = ProductListFragment().apply {
        arguments = Bundle().apply {
            putString("category", "electronics")
            putString("sort", "price")
        }
    }

    val activity = Robolectric.buildActivity(MainActivity::class.java)
        .create()
        .start()
        .get()

    activity.supportFragmentManager.beginTransaction()
        .add(android.R.id.content, fragment)
        .commitNow()

    // Assert
    val events = debugProvider.getLoggedEvents()
    assertThat(events).isNotEmpty()
    val lastEvent = events.last()
    assertEquals("screen_view", lastEvent.eventName)
    assertEquals("Product List", lastEvent.parameters["screen_name"])
    assertEquals("electronics", lastEvent.parameters["category"])
    assertEquals("price", lastEvent.parameters["sort"])
}
```

### 5. Testing Method-Level Tracking (@Track)

Verify that method-level analytics events are properly tracked:

```kotlin
@Test
fun `verify method tracking with parameters`() {
    // Arrange
    val mockProvider = mockk<AnalyticsProvider>()
    ScreenTracking.initialize(
        config = analyticsConfig {
            providers.add(mockProvider)
        }
    )

    val viewModel = UserViewModel()

    // Act
    viewModel.loadUserProfile(
        userId = "user_456",
        source = "deep_link"
    )

    // Assert
    verify(exactly = 1) {
        mockProvider.trackScreen(
            "user_profile_loaded",
            match { params ->
                params["user_id"] == "user_456" &&
                params["source"] == "deep_link"
            }
        )
    }
}
```

### 6. Testing with Custom Analytics Providers

Create a test provider to capture and verify analytics events in your tests:

```kotlin
class TestAnalyticsProvider : AnalyticsProvider {
    private val capturedEvents = mutableListOf<AnalyticsEvent>()

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        capturedEvents.add(AnalyticsEvent(screenName, parameters, System.currentTimeMillis()))
    }

    fun getEvents(): List<AnalyticsEvent> = capturedEvents.toList()

    fun clear() {
        capturedEvents.clear()
    }
}

// Usage in tests
@Test
fun `verify complete user journey tracking`() {
    // Arrange
    val testProvider = TestAnalyticsProvider()
    ScreenTracking.initialize(
        config = analyticsConfig {
            providers.add(testProvider)
        }
    )

    // Act
    val activity = Robolectric.buildActivity(MainActivity::class.java)
        .create()
        .start()
        .resume()
        .get()

    // Navigate through screens
    activity.navigateToProfile()
    activity.navigateToSettings()

    // Assert
    val events = testProvider.getEvents()
    assertEquals(3, events.size)
    assertEquals("Home Screen", events[0].screenName)
    assertEquals("Profile Screen", events[1].screenName)
    assertEquals("Settings Screen", events[2].screenName)
}
```

### 7. Espresso UI Tests with Analytics Verification

Verify analytics tracking during UI tests:

```kotlin
@RunWith(AndroidJUnit4::class)
class AnalyticsUITest {

    @get:Rule
    val activityRule = ActivityScenario.launch(MainActivity::class.java)

    private val debugProvider = InMemoryDebugAnalyticsProvider()

    @Before
    fun setup() {
        ScreenTracking.initialize(
            config = analyticsConfig {
                providers.add(debugProvider)
            }
        )
    }

    @Test
    fun testNavigationTrackingInUI() {
        // Act
        onView(withId(R.id.profile_button)).perform(click())

        // Assert
        val events = debugProvider.getLoggedEvents()
        val profileScreenEvent = events.find { it.parameters["screen_name"] == "Profile Screen" }

        assertNotNull(profileScreenEvent)
    }
}
```

### 8. Testing Best Practices

- **Isolate Analytics from UI Tests**: Use mock providers instead of real analytics services in tests
- **Verify Parameters**: Always assert that dynamic parameters are captured correctly
- **Test Fragment Arguments**: Ensure Fragment parameters passed via Bundle are properly tracked
- **Use Debug Provider in CI**: Enable InMemoryDebugAnalyticsProvider in CI builds to catch tracking issues
- **Test Error Scenarios**: Verify that analytics failures don't crash your app using error handlers
- **Performance Testing**: Monitor that analytics doesn't impact app startup time

### 9. Debugging Analytics in Release Builds

To debug analytics in release builds without exposing debug code, create a hidden debug menu:

```kotlin
class BuildConfigProvider {
    companion object {
        fun getDebugProvider(): AnalyticsProvider? {
            return if (shouldShowDebugInfo()) {
                InMemoryDebugAnalyticsProvider()
            } else {
                null
            }
        }

        private fun shouldShowDebugInfo(): Boolean {
            // Check BuildConfig or custom logic
            return BuildConfig.DEBUG || isDebugDeviceEnabled()
        }
    }
}
```

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