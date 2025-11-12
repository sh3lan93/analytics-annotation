# Examples and Use Cases

This document provides comprehensive examples for using the Analytics Annotation Library in various scenarios.

## Basic Examples

### Simple Activity Tracking
```kotlin
@TrackScreen(screenName = "Home")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Analytics automatically tracked when onCreate is called
    }
}
```

### Fragment with Parameters
```kotlin
@TrackScreen(screenName = "Product List")
class ProductListFragment : Fragment(), TrackedScreenParamsProvider {

    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf(
            "category" to (arguments?.getString("category") ?: "all"),
            "sort_order" to (arguments?.getString("sort") ?: "name")
        )
    }
}
```

## Advanced Use Cases

### E-commerce Product Details
```kotlin
@TrackScreen(screenName = "Product Details")
class ProductDetailsActivity : AppCompatActivity(), TrackedScreenParamsProvider {

    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        product = intent.getParcelableExtra("product") ?: return
    }

    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf(
            "product_id" to product.id,
            "product_name" to product.name,
            "category" to product.category,
            "price" to product.price,
            "in_stock" to product.inStock
        )
    }
}
```

### User Profile with Dynamic Data
```kotlin
@TrackScreen(screenName = "User Profile")
class ProfileActivity : AppCompatActivity(), TrackedScreenParamsProvider {

    private lateinit var userManager: UserManager

    override fun getTrackedScreenParams(): Map<String, Any> {
        val user = userManager.getCurrentUser()
        return mapOf(
            "user_id" to user.id,
            "subscription_type" to user.subscriptionType,
            "account_age_days" to user.getAccountAgeDays()
        )
    }
}
```

### Navigation Drawer Fragment
```kotlin
@TrackScreen(screenName = "Navigation Menu")
class NavigationDrawerFragment : Fragment(), TrackedScreenParamsProvider {

    override fun getTrackedScreenParams(): Map<String, Any> {
        val activity = activity as? MainActivity
        return mapOf(
            "current_tab" to (activity?.getCurrentTab() ?: "unknown"),
            "user_type" to getUserType()
        )
    }

    private fun getUserType(): String {
        return if (UserSession.isPremium()) "premium" else "free"
    }
}
```

## Custom Analytics Providers

### Firebase Analytics Provider
```kotlin
class FirebaseAnalyticsProvider(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsProvider {

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName.replace(" ", ""))

            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
```

### Custom API Provider
```kotlin
class CustomApiAnalyticsProvider(
    private val apiClient: AnalyticsApiClient
) : AnalyticsProvider {

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        try {
            val event = ScreenViewEvent(
                screenName = screenName,
                parameters = parameters,
                timestamp = System.currentTimeMillis(),
                sessionId = SessionManager.getCurrentSessionId()
            )

            // Queue for batch sending
            apiClient.queueEvent(event)
        } catch (e: Exception) {
            Log.e("Analytics", "Failed to track screen: $screenName", e)
        }
    }
}
```

## Testing Examples

### Unit Test with Mock Provider
```kotlin
@Test
fun `verify home screen tracking`() {
    // Arrange
    val mockProvider = mockk<AnalyticsProvider>()
    ScreenTracking.initialize(context) {
        providers.add(mockProvider)
    }

    // Act
    val activity = Robolectric.buildActivity(MainActivity::class.java)
        .create()
        .start()
        .resume()
        .get()

    // Assert
    verify { mockProvider.trackScreen("Home", emptyMap()) }
}
```

### Integration Test with Debug Provider
```kotlin
@Test
fun `verify product details tracking with parameters`() {
    // Arrange
    val debugProvider = InMemoryDebugAnalyticsProvider()
    ScreenTracking.initialize(context) {
        providers.add(debugProvider)
    }

    val intent = Intent(context, ProductDetailsActivity::class.java).apply {
        putExtra("product", testProduct)
    }

    // Act
    val activity = Robolectric.buildActivity(ProductDetailsActivity::class.java, intent)
        .create()
        .start()
        .resume()
        .get()

    // Assert
    val lastEvent = debugProvider.getLastEvent()
    assertEquals("Product Details", lastEvent?.screenName)
    assertEquals(testProduct.id, lastEvent?.parameters?.get("product_id"))
}
```

### Espresso UI Test
```kotlin
@Test
fun testNavigationTracking() {
    // Launch app
    ActivityScenario.launch(MainActivity::class.java)

    // Navigate to profile
    onView(withId(R.id.profile_button)).perform(click())

    // Verify tracking (would need test-specific analytics provider)
    // This is typically verified through your analytics provider's test methods
}
```

## Configuration Examples

### Development Configuration

**Plugin Configuration (in build.gradle.kts):**
```kotlin
plugins {
    id("dev.moshalan.easyanalytics") version "2.1.0"
}

analytics {
    enabled = true
    debugMode = true                        // Enable verbose logging during bytecode transformation

    // Optional: Include only specific packages
    includePackages = setOf("com.yourapp")
}
```

**Runtime Configuration (in Application):**
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config = analyticsConfig {
                debugMode = true

                // Add debug providers for development
                providers.add(InMemoryDebugAnalyticsProvider())
                providers.add(LogcatAnalyticsProvider())

                // Global error handler for all analytics operations
                errorHandler = { throwable ->
                    Log.e("Analytics", "Analytics error", throwable)
                }

                // Configure method tracking (optional)
                methodTracking {
                    enabled = true
                }
            }
        )

        // Set global parameters
        ScreenTracking.setGlobalParameters(mapOf(
            "app_version" to BuildConfig.VERSION_NAME,
            "build_type" to "debug"
        ))
    }
}
```

### Production Configuration

**Plugin Configuration (in build.gradle.kts):**
```kotlin
plugins {
    id("dev.moshalan.easyanalytics") version "2.1.0"
}

analytics {
    enabled = true
    debugMode = false                       // Disable verbose logging in production

    // Performance optimization - include only tracked packages
    includePackages = setOf(
        "com.yourapp.features",
        "com.yourapp.screens"
    )

    // Exclude internal packages to reduce processing
    excludePackages = setOf(
        "com.yourapp.debug",
        "com.yourapp.testing"
    )
}
```

**Runtime Configuration (in Application):**
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config = analyticsConfig {
                debugMode = false

                // Add production analytics providers
                providers.add(FirebaseAnalyticsProvider(Firebase.analytics))
                providers.add(MixpanelAnalyticsProvider(mixpanel))

                // Global error handler for all analytics operations
                errorHandler = { throwable ->
                    Log.e("Analytics", "Analytics error", throwable)
                }

                // Configure method tracking
                methodTracking {
                    enabled = true
                    customSerializers.add(MyCustomParameterSerializer())
                }
            }
        )

        // Set global parameters
        ScreenTracking.setGlobalParameters(mapOf(
            "app_version" to BuildConfig.VERSION_NAME,
            "build_type" to "release",
            "user_type" to UserSession.getUserType()
        ))
    }
}
```

### A/B Testing Integration
```kotlin
@TrackScreen(screenName = "Home Screen")
class MainActivity : AppCompatActivity(), TrackedScreenParamsProvider {

    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf(
            "ab_test_variant" to ABTestManager.getVariant("home_layout"),
            "user_segment" to UserSegmentation.getCurrentSegment()
        )
    }
}
```

## Error Handling Examples

### Graceful Degradation
```kotlin
class RobustAnalyticsProvider(
    private val primaryProvider: AnalyticsProvider,
    private val fallbackProvider: AnalyticsProvider
) : AnalyticsProvider {

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        try {
            primaryProvider.trackScreen(screenName, parameters)
        } catch (e: Exception) {
            Log.w("Analytics", "Primary provider failed, using fallback", e)
            try {
                fallbackProvider.trackScreen(screenName, parameters)
            } catch (fallbackError: Exception) {
                Log.e("Analytics", "Both providers failed", fallbackError)
            }
        }
    }
}
```

### Parameter Validation
```kotlin
class ValidatingAnalyticsProvider(
    private val delegate: AnalyticsProvider
) : AnalyticsProvider {

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        val sanitizedParams = parameters.filterValues { value ->
            when (value) {
                is String -> value.isNotBlank() && !containsPII(value)
                is Number -> value.toDouble().isFinite()
                is Boolean -> true
                else -> false
            }
        }

        delegate.trackScreen(screenName, sanitizedParams)
    }

    private fun containsPII(value: String): Boolean {
        // Basic PII detection (implement based on your needs)
        return value.contains("@") || value.matches(Regex("\\d{10,}"))
    }
}
```

## Performance Optimization Examples

### Lazy Provider Initialization
```kotlin
class LazyAnalyticsProvider(
    private val providerFactory: () -> AnalyticsProvider
) : AnalyticsProvider {

    private val provider by lazy { providerFactory() }

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        provider.trackScreen(screenName, parameters)
    }
}

// Usage
ScreenTracking.initialize(
    config = analyticsConfig {
        providers.add(LazyAnalyticsProvider {
            FirebaseAnalyticsProvider(Firebase.analytics)
        })
    }
)
```

### Batch Processing Provider
```kotlin
class BatchingAnalyticsProvider(
    private val delegate: AnalyticsProvider,
    private val batchSize: Int = 10,
    private val flushInterval: Long = 30_000 // 30 seconds
) : AnalyticsProvider {

    private val eventQueue = mutableListOf<ScreenEvent>()
    private val handler = Handler(Looper.getMainLooper())

    override fun trackScreen(screenName: String, parameters: Map<String, Any>) {
        synchronized(eventQueue) {
            eventQueue.add(ScreenEvent(screenName, parameters, System.currentTimeMillis()))

            if (eventQueue.size >= batchSize) {
                flushEvents()
            } else {
                scheduleFlush()
            }
        }
    }

    private fun flushEvents() {
        val events = eventQueue.toList()
        eventQueue.clear()

        events.forEach { event ->
            delegate.trackScreen(event.screenName, event.parameters)
        }
    }

    private fun scheduleFlush() {
        handler.removeCallbacks(::flushEvents)
        handler.postDelayed(::flushEvents, flushInterval)
    }
}
```

---