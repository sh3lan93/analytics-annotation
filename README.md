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

### For Local Development

If you're working with this repository locally, add the following to your module-level `build.gradle.kts` file:

```kotlin
dependencies {
    implementation(project(":annotation"))
    implementation(project(":core"))
    
    // Optional: For Jetpack Compose support
    implementation(project(":compose"))
}
```

### For Published Library (Future)

*Note: This library is not yet published to Maven Central. The installation instructions below are for future reference.*

```kotlin
dependencies {
    implementation("com.shalan.analytics:annotation:1.0.0")
    implementation("com.shalan.analytics:core:1.0.0")
    
    // Optional: For Jetpack Compose support
    implementation("com.shalan.analytics:compose:1.0.0")
}
```

## 🚀 Usage

### 1. Initialize the Library

Initialize `ScreenTracking` in your `Application` class's `onCreate()` method. This is where you configure your analytics providers.

```kotlin
// SampleApp.kt
import android.app.Application
import com.shalan.analytics.core.ScreenTracking
import com.shalan.analytics.core.InMemoryDebugAnalyticsProvider
import com.shalan.analytics.core.analyticsConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            application = this,
            config = analyticsConfig {
                debugMode = true // Enable debug mode
                providers.add(InMemoryDebugAnalyticsProvider()) // Add debug provider for easy inspection
                // providers.add(FirebaseAnalyticsProvider()) // Add your actual analytics providers here
                // providers.add(MixpanelAnalyticsProvider("YOUR_MIXPANEL_TOKEN"))
            }
        )
    }
}
```

Remember to declare your `SampleApp` in `AndroidManifest.xml`:

```xml
<application
    android:name=".SampleApp"
    ...
</application>
```

### 2. Annotate Your Screens

Simply add the `@TrackScreen` annotation to your Activities or Fragments:

```kotlin
// MainActivity.kt
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen
import com.shalan.analytics.core.TrackedScreenParamsProvider

@TrackScreen(screenName = "Main Screen", additionalParams = ["user_id", "user_name"])
class MainActivity : AppCompatActivity(), TrackedScreenParamsProvider {
    // ... your activity code
    
    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf(
            "user_id" to "12345",
            "user_name" to "John Doe"
        )
    }
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
// ExampleComposableScreen.kt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shalan.analytics.compose.TrackScreenComposable
import com.shalan.analytics.compose.TrackScreenView

@TrackScreenComposable(screenName = "Example Composable Screen")
@Composable
fun ExampleComposableScreen() {
    TrackScreenView(screenName = "Example Composable Screen") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "This is a composable screen")
        }
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

## 🏗️ Architecture Overview

This library follows a clean architecture pattern with clear separation of concerns:

### Module Structure
- **`:annotation`** - Contains the `@TrackScreen` and `@TrackScreenComposable` annotations
- **`:core`** - Core analytics functionality, lifecycle callbacks, and provider management
- **`:compose`** - Jetpack Compose integration with `TrackScreenView` composable
- **`:app`** - Sample application demonstrating library usage

### Key Components
- **ScreenTracking** - Singleton entry point for library initialization
- **AnalyticsManager** - Manages multiple analytics providers and event dispatching
- **ScreenTrackingCallbacks** - Activity lifecycle callbacks for automatic tracking
- **ScreenTrackingFragmentLifecycleCallbacks** - Fragment lifecycle callbacks
- **AnalyticsProvider** - Interface for integrating with different analytics services

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