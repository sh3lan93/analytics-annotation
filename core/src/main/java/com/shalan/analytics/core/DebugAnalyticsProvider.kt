package com.shalan.analytics.core

/**
 * An extended [AnalyticsProvider] interface specifically for debugging purposes.
 * Implementations of this interface can provide additional functionalities like
 * retrieving logged events or enabling/disabling mock modes.
 */
interface DebugAnalyticsProvider : AnalyticsProvider {
    /**
     * Retrieves a list of all events that have been logged by this provider.
     * This is useful for verifying analytics calls during development and testing.
     *
     * @return A [List] of [AnalyticsEvent] objects that have been logged.
     */
    fun getLoggedEvents(): List<AnalyticsEvent>

    /**
     * Clears all previously logged events from this provider's internal storage.
     * This is useful for resetting the state between tests or debugging sessions.
     */
    fun clearLoggedEvents()

    /**
     * Sets the mock mode for this provider.
     * When mock mode is enabled, the provider might simulate analytics calls without
     * actually sending data to a backend, or it might alter its behavior for testing.
     *
     * @param enabled `true` to enable mock mode, `false` to disable it.
     */
    fun setMockMode(enabled: Boolean)
}

/**
 * A data class representing a generic analytics event.
 * Used by [DebugAnalyticsProvider] to store and expose logged event data.
 *
 * @property eventName The name of the event.
 * @property parameters A map of key-value pairs associated with the event.
 */
data class AnalyticsEvent(
    val eventName: String,
    val parameters: Map<String, Any>,
)
