# Android Screen Tracking Annotation Library

## 🚀 Project Overview

This Android library provides a **declarative, annotation-based approach** to screen tracking for analytics. It aims to significantly reduce boilerplate code by allowing Android engineers to simply annotate their Activities and Fragments with `@TrackScreen`, eliminating the need for manual analytics injection in every screen.

**Why this library?**

In many Android projects, screen tracking can become a tedious and error-prone process, often leading to duplicated code and inconsistencies. This library abstracts away the complexities of lifecycle management and analytics dispatch, allowing you to focus on building features, not on repetitive tracking logic.

## ✨ Features

*   **Annotation-Driven**: Mark any Activity or Fragment with `@TrackScreen` for automatic tracking.
*   **Lifecycle-Aware**: Leverages `Application.ActivityLifecycleCallbacks` and `FragmentManager.FragmentLifecycleCallbacks` for robust and centralized screen event detection.
*   **Performance Optimized**: Employs reflection caching to minimize overhead during runtime.
*   **Flexible Parameter Handling**: Supports static parameters via annotation and dynamic, runtime parameters through an optional interface (`TrackedScreenParamsProvider`).
*   **Provider-Agnostic**: Easily integrate with any analytics service by implementing the `AnalyticsProvider` interface.
*   **Error Resilient**: Analytics calls are wrapped in `try-catch` blocks to prevent crashes in case of provider failures.
*   **Debug Mode**: Includes a built-in `InMemoryDebugAnalyticsProvider` for easy testing and verification of logged events during development.

## 🛠️ Installation

Add the following to your module-level `build.gradle.kts` file:

```kotlin
dependencies {
    implementation(project(":annotation"))
    implementation(project(":core"))
}
```

## 🚀 Usage

### 1. Initialize the Library

Initialize `ScreenTracking` in your `Application` class's `onCreate()` method. This is where you configure your analytics providers.

```kotlin
// MyApplication.kt
import android.app.Application
import com.shalan.analytics.core.ScreenTracking
import com.shalan.analytics.core.LogcatAnalyticsProvider // For basic logging
import com.shalan.analytics.core.InMemoryDebugAnalyticsProvider // For debugging
import com.shalan.analytics.core.analyticsConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            application = this,
            config = analyticsConfig {
                debugMode = BuildConfig.DEBUG // Enable debug mode in debug builds
                providers.add(LogcatAnalyticsProvider()) // Add a basic Logcat provider
                // providers.add(FirebaseAnalyticsProvider()) // Add your actual analytics providers here
                // providers.add(MixpanelAnalyticsProvider("YOUR_MIXPANEL_TOKEN"))

                // You can also add the InMemoryDebugAnalyticsProvider for easy inspection of events
                if (BuildConfig.DEBUG) {
                    providers.add(InMemoryDebugAnalyticsProvider())
                }
            }
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        ScreenTracking.getManager().release() // Important for releasing resources
    }
}
```

Remember to declare your `MyApplication` in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...
</application>
```

### 2. Annotate Your Screens

Simply add the `@TrackScreen` annotation to your Activities or Fragments:

```kotlin
// HomeActivity.kt
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen

@TrackScreen(screenName = "Home Screen")
class HomeActivity : AppCompatActivity() {
    // ... your activity code
}

// ExampleFragment.kt
import androidx.fragment.app.Fragment
import com.shalan.analytics.annotation.TrackScreen

@TrackScreen(screenName = "Example Fragment")
class ExampleFragment : Fragment() {
    // ... your fragment code
}
```

### 3. Tracking Jetpack Compose Screens

The library now supports tracking screen views in Jetpack Compose.

First, add the `:compose` module to your dependencies:

```kotlin
dependencies {
    // ... other dependencies
    implementation(project(":compose"))
}
```

Then, you can track your composable screens using the `TrackScreenView` composable function. For clarity and consistency, it's also recommended to annotate your main screen composable with `@TrackScreenComposable`.

```kotlin
// ProfileScreen.kt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.shalan.analytics.compose.TrackScreenComposable
import com.shalan.analytics.compose.TrackScreenView

@TrackScreenComposable(screenName = "Profile Screen")
@Composable
fun ProfileScreen() {
    // Wrap your screen's content with TrackScreenView
    TrackScreenView(screenName = "Profile Screen") {
        // ... your screen's UI
        Text("This is the Profile Screen!")
    }
}
```

The `TrackScreenView` composable uses a `LaunchedEffect` to log the screen view event once when it enters the composition. The `@TrackScreenComposable` annotation serves as a clear marker for which composables are being tracked, making your code more self-documenting.

### 4. Provide Dynamic Parameters (Optional)

If your screen needs to send dynamic parameters (e.g., user ID, product ID) that are not compile-time constants, implement the `TrackedScreenParamsProvider` interface for Activities/Fragments.

```kotlin
// ProductDetailActivity.kt
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen
import com.shalan.analytics.core.TrackedScreenParamsProvider

@TrackScreen(
    screenName = "Product Detail Screen",
    screenClass = "ProductView",
    additionalParams = ["product_id", "product_name", "user_type"]
)
class ProductDetailActivity : AppCompatActivity(), TrackedScreenParamsProvider {

    private val productId: String = "P12345"
    private val productName: String = "Awesome Gadget"
    private val userType: String = "Premium"

    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf(
            "product_id" to productId,
            "product_name" to productName,
            "user_type" to userType,
            "unwanted_param" to "this_will_be_filtered" // This parameter will be ignored
        )
    }
    // ... rest of your activity code
}
```

The library will automatically filter `getTrackedScreenParams()` to only include keys specified in `additionalParams` of the `@TrackScreen` annotation.

### 5. Setting Global Parameters

You can set global parameters that will be included with all subsequent analytics events. These are useful for user properties, app version, A/B test groups, etc.

```kotlin
// Somewhere in your app, after ScreenTracking.initialize()
ScreenTracking.setGlobalParameters(mapOf(
    "app_version" to "1.0.0",
    "user_segment" to "early_adopter"
))

// You can update them later
ScreenTracking.setGlobalParameters(mapOf(
    "user_segment" to "loyal_user"
))
```

## 🧪 Testing

The library includes comprehensive unit and integration tests to ensure reliability. You can run them using:

```bash
./gradlew test
./gradlew connectedCheck # Requires a connected device or emulator
```

## 🤝 Contributing

Contributions are welcome! Please follow the existing code style, add tests for new features, and update documentation.

## 🛣️ Future Enhancements

*   **Compile-time Validation**: Implement an annotation processor (e.g., using KSP) for compile-time validation of `@TrackScreen` parameters.
*   **Event Batching**: Implement event batching for improved network efficiency.
*   **Security Considerations**: Add checks for PII, parameter sanitization, and user privacy settings.
*   **UI Tests**: Develop UI tests to confirm screen tracking during navigation.

## 📄 License

This library is licensed under the Apache License 2.0. See the `LICENSE` file for more details.