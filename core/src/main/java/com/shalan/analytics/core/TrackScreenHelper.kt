package com.shalan.analytics.core

import androidx.annotation.RestrictTo

/**
 * Internal helper class for screen tracking, called by ASM-injected code.
 *
 * This class simplifies bytecode generation by providing simple static methods
 * that handle complex logic like parameter collection and error handling.
 *
 * These methods should not be called directly by library users.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object TrackScreenHelper {
    /**
     * Tracks a screen view for an Activity or Fragment.
     *
     * This method is called by ASM-injected code in Activity.onCreate() or Fragment.onViewCreated().
     *
     * @param instance The Activity or Fragment instance
     * @param screenName The name of the screen to track
     * @param screenClass The class name of the screen
     */
    @JvmStatic
    fun trackScreen(
        instance: Any,
        screenName: String,
        screenClass: String,
    ) {
        try {
            val parameters =
                if (instance is TrackedScreenParamsProvider) {
                    instance.getTrackedScreenParams()
                } else {
                    emptyMap()
                }

            ScreenTracking.getManager().logScreenView(
                screenName = screenName,
                screenClass = screenClass,
                parameters = parameters,
            )
        } catch (e: Exception) {
            // Analytics should never crash the app
            // Silently ignore errors in tracking
        }
    }
}
