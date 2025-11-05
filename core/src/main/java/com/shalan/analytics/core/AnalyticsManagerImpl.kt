package com.shalan.analytics.core

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Default implementation of [AnalyticsManager] that dispatches analytics events
 * to a list of registered [AnalyticsProvider]s.
 * This implementation ensures that if one provider fails to log an event, it does not
 * prevent other providers from logging, and it prevents application crashes.
 *
 * Events are logged asynchronously on a background thread to avoid blocking the main thread.
 *
 * @param providers A list of [AnalyticsProvider] instances to which events will be dispatched.
 */
class AnalyticsManagerImpl(
    private val providers: List<AnalyticsProvider>,
) : AnalyticsManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var globalParameters: Map<String, Any> = emptyMap()

    /**
     * Logs a screen view event by iterating through all registered [AnalyticsProvider]s
     * and calling their [AnalyticsProvider.logEvent] method.
     * Each call to a provider is wrapped in a try-catch block to prevent crashes.
     *
     * @param screenName The name of the screen being viewed.
     * @param screenClass The class name associated with the screen.
     * @param parameters An optional map of additional key-value pairs for the event.
     */
    override fun logScreenView(
        screenName: String,
        screenClass: String,
        parameters: Map<String, Any>,
    ) {
        scope.launch {
            val mergedParameters = globalParameters + parameters
            providers.forEach { provider ->
                try {
                    provider.logEvent(
                        "screen_view",
                        mapOf(
                            "screen_name" to screenName,
                            "screen_class" to screenClass,
                        ) + mergedParameters,
                    )
                } catch (t: Throwable) {
                    // Report error to global error handler if configured
                    ScreenTracking.getErrorHandler()?.invoke(t)
                }
            }
        }
    }

    /**
     * Logs a custom event by iterating through all registered [AnalyticsProvider]s
     * and calling their [AnalyticsProvider.logEvent] method.
     * Each call to a provider is wrapped in a try-catch block to prevent crashes.
     *
     * @param eventName The name of the event.
     * @param parameters A map of key-value pairs representing the event parameters.
     */
    override fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
        includeGlobalParameters: Boolean,
    ) {
        scope.launch {
            val mergedParameters =
                if (includeGlobalParameters) globalParameters + parameters else parameters
            providers.forEach { provider ->
                try {
                    provider.logEvent(eventName, mergedParameters)
                } catch (t: Throwable) {
                    Log.e(
                        "AnalyticsManager",
                        "Failed to log event to ${provider.javaClass.simpleName}",
                        t,
                    )
                    // Report error to global error handler if configured
                    ScreenTracking.getErrorHandler()?.invoke(t)
                }
            }
        }
    }

    /**
     * Sets global parameters that will be included with all subsequent analytics events.
     * Existing global parameters will be replaced by the new ones.
     *
     * @param parameters A map of key-value pairs representing the global parameters.
     */
    override fun setGlobalParameters(parameters: Map<String, Any>) {
        this.globalParameters = parameters
    }

    /**
     * Releases any resources held by the analytics manager, canceling any ongoing coroutine tasks.
     */
    override fun release() {
        scope.cancel()
    }
}
