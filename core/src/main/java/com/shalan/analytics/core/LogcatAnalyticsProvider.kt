package com.shalan.analytics.core

import android.util.Log

/**
 * A simple implementation of [AnalyticsProvider] that logs all analytics events
 * to Logcat. This is primarily used for debugging and testing purposes during development.
 */
class LogcatAnalyticsProvider : AnalyticsProvider {
    /**
     * Logs a generic event to Logcat.
     *
     * @param eventName The name of the event to log.
     * @param parameters A map of key-value pairs representing the event's properties.
     */
    override fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
    ) {
        Log.i(
            "LogcatAnalyticsProvider",
            "eventName: $eventName, parameters: $parameters",
        )
    }

    /**
     * Logs the user ID to Logcat.
     *
     * @param userId The unique identifier for the user.
     */
    override fun setUserId(userId: String?) {
        Log.i("LogcatAnalyticsProvider", "setUserId: $userId")
    }

    /**
     * Logs a user property to Logcat.
     *
     * @param key The name of the user property.
     * @param value The value of the user property.
     */
    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        Log.i("LogcatAnalyticsProvider", "setUserProperty: $key = $value")
    }
}
