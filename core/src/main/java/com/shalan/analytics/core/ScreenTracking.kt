package com.shalan.analytics.core

import android.app.Application
import androidx.annotation.VisibleForTesting

/**
 * A singleton object responsible for initializing and providing access to the analytics manager.
 * This is the primary entry point for setting up screen tracking in an Android application.
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
     * @param application The application instance to register lifecycle callbacks with.
     * @param config The [AnalyticsConfig] that defines the analytics providers and other settings.
     */
    fun initialize(
        application: Application,
        config: AnalyticsConfig,
    ) {
        analyticsManager = AnalyticsManagerImpl(config.providers)
        application.registerActivityLifecycleCallbacks(ScreenTrackingCallbacks(analyticsManager))
    }

    /**
     * Retrieves the initialized [AnalyticsManager] instance.
     * This method should only be called after [initialize] has been successfully invoked.
     *
     * @return The [AnalyticsManager] instance.
     * @throws IllegalStateException if [initialize] has not been called prior to this method.
     */
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
}
