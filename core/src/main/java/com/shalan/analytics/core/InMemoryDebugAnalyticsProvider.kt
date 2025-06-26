package com.shalan.analytics.core

import android.util.Log

/**
 * An in-memory implementation of [DebugAnalyticsProvider] for testing and debugging.
 * It stores logged events in a list, allowing them to be retrieved and inspected.
 * It also supports a mock mode, which can be used to simulate analytics behavior.
 */
class InMemoryDebugAnalyticsProvider : DebugAnalyticsProvider {
    private val loggedEvents = mutableListOf<AnalyticsEvent>()
    private var mockModeEnabled: Boolean = false

    /**
     * Logs a generic event. If mock mode is enabled, it only logs to Logcat.
     * Otherwise, it stores the event in memory and logs to Logcat.
     *
     * @param eventName The name of the event to log.
     * @param parameters A map of key-value pairs representing the event's properties.
     */
    override fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
    ) {
        if (mockModeEnabled) {
            Log.d("DebugAnalyticsProvider", "[MOCK MODE] Logged event: $eventName with params: $parameters")
        } else {
            loggedEvents.add(AnalyticsEvent(eventName, parameters))
            Log.d("DebugAnalyticsProvider", "Logged event: $eventName with params: $parameters")
        }
    }

    /**
     * Sets the user ID. Logs to Logcat.
     *
     * @param userId The unique identifier for the user.
     */
    override fun setUserId(userId: String?) {
        Log.d("DebugAnalyticsProvider", "Set User ID: $userId")
    }

    /**
     * Sets a user property. Logs to Logcat.
     *
     * @param key The name of the user property.
     * @param value The value of the user property.
     */
    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        Log.d("DebugAnalyticsProvider", "Set User Property: $key = $value")
    }

    /**
     * Retrieves a read-only list of all events that have been logged.
     *
     * @return A [List] of [AnalyticsEvent] objects.
     */
    override fun getLoggedEvents(): List<AnalyticsEvent> {
        return loggedEvents.toList()
    }

    /**
     * Clears all previously logged events from memory.
     */
    override fun clearLoggedEvents() {
        loggedEvents.clear()
        Log.d("DebugAnalyticsProvider", "Cleared logged events.")
    }

    /**
     * Sets the mock mode for this provider.
     *
     * @param enabled `true` to enable mock mode, `false` to disable it.
     */
    override fun setMockMode(enabled: Boolean) {
        mockModeEnabled = enabled
        Log.d("DebugAnalyticsProvider", "Mock mode enabled: $enabled")
    }
}
