package com.shalan.analytics.core

import android.app.Application
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * A singleton object responsible for initializing and providing access to the analytics manager.
 * This is the primary entry point for setting up screen tracking in an Android application.
 *
 * The library supports two initialization approaches:
 * 1. **Lifecycle-based tracking**: Pass an Application instance to register lifecycle callbacks
 *    for automatic screen tracking of Activities and Fragments.
 * 2. **Plugin-based tracking**: Pass null for application parameter when using the Gradle plugin
 *    for automatic bytecode instrumentation and screen tracking.
 */
object ScreenTracking {
    private lateinit var analyticsManager: AnalyticsManager

    // For testing purposes only
    @VisibleForTesting
    fun setAnalyticsManagerForTesting(manager: AnalyticsManager) {
        this.analyticsManager = manager
    }

    /**
     * Initializes the Screen Tracking library.
     * This method should be called once, typically in the `onCreate()` method of your
     * application's custom [Application] class.
     *
     * @param config The [AnalyticsConfig] that defines the analytics providers and other settings.
     * @param application The application instance to register lifecycle callbacks with.
     *                   Pass a non-null Application instance for lifecycle-based tracking.
     *                   Pass null when using the Gradle plugin for bytecode instrumentation.
     *
     * ## Usage Examples:
     *
     * **Lifecycle-based tracking (recommended for manual integration):**
     * ```kotlin
     * // In your Application.onCreate()
     * ScreenTracking.initialize(
     *     config = analyticsConfig {
     *         providers.add(YourAnalyticsProvider())
     *     },
     *     application = this
     * )
     * ```
     *
     * **Plugin-based tracking (when using Gradle plugin):**
     * ```kotlin
     * // In your Application.onCreate()
     * ScreenTracking.initialize(
     *     config = analyticsConfig {
     *         providers.add(YourAnalyticsProvider())
     *     },
     *     application = null  // Plugin handles instrumentation
     * )
     * ```
     */
    fun initialize(
        config: AnalyticsConfig,
        application: Application? = null,
    ) {
        analyticsManager = AnalyticsManagerImpl(config.providers)
        application?.registerActivityLifecycleCallbacks(ScreenTrackingCallbacks(analyticsManager))
    }

    /**
     * Retrieves the initialized [AnalyticsManager] instance.
     * This method should only be called after [initialize] has been successfully invoked.
     * Used by both lifecycle callbacks and Gradle plugin injected code.
     *
     * @return The [AnalyticsManager] instance.
     * @throws IllegalStateException if [initialize] has not been called prior to this method.
     */
    @JvmStatic
    fun getManager(): AnalyticsManager {
        check(::analyticsManager.isInitialized) {
            "ScreenTracking must be initialized in Application.onCreate()"
        }
        return analyticsManager
    }

    /**
     * Sets global parameters that will be included with all subsequent analytics events.
     * This method can be called at any time after [initialize] to update global parameters.
     *
     * @param parameters A map of key-value pairs representing the global parameters.
     */
    fun setGlobalParameters(parameters: Map<String, Any>) {
        getManager().setGlobalParameters(parameters)
    }

    /**
     * Checks if the ScreenTracking library has been initialized.
     * This method is used by the Gradle plugin for bytecode injection safety checks.
     *
     * @return true if the library has been initialized, false otherwise.
     */
    private fun isInitialized(): Boolean {
        return ::analyticsManager.isInitialized
    }

    /**
     * Tracks a screen view event with the given name and parameters.
     * This method is used by the Gradle plugin for automatic screen tracking injection.
     *
     * @param screenName The name of the screen being tracked.
     * @param parameters Additional parameters to include with the screen tracking event.
     */
    fun trackScreen(
        screenName: String,
        parameters: Map<String, Any> = emptyMap(),
    ) {
        if (isInitialized()) {
            getManager().logScreenView(screenName, "", parameters)
        }
    }

    /**
     * Creates a parameters map by checking if the instance implements TrackedScreenParamsProvider
     * and filtering the provided parameters against the requested parameter keys.
     * This method is used by the Gradle plugin for bytecode injection.
     *
     * @param instance The Activity/Fragment instance to check for TrackedScreenParamsProvider
     * @param parameterKeys Array of parameter keys to extract from the provider
     * @return Map containing the filtered parameters
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmStatic
    fun createParametersMap(
        instance: Any,
        parameterKeys: Array<String>,
    ): Map<String, Any> {
        return try {
            val providedParams =
                if (instance is TrackedScreenParamsProvider) {
                    instance.getTrackedScreenParams()
                } else {
                    emptyMap()
                }

            val filteredParams = mutableMapOf<String, Any>()
            for (key in parameterKeys) {
                providedParams[key]?.let { value ->
                    filteredParams[key] = value
                }
            }
            filteredParams
        } catch (e: Exception) {
            // Analytics should never crash the app
            emptyMap()
        }
    }
}
