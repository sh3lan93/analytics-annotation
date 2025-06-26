package com.shalan.analytics.core

/**
 * Defines the contract for an analytics provider. Concrete implementations of this interface
 * will integrate with specific analytics services (e.g., Firebase, Mixpanel).
 */
interface AnalyticsProvider {
    /**
     * Logs a generic event to the analytics service.
     *
     * @param eventName The name of the event to log (e.g., "screen_view", "button_click").
     * @param parameters A map of key-value pairs representing the event's properties.
     */
    fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
    )

    /**
     * Sets the user ID for the current analytics session.
     *
     * @param userId The unique identifier for the user. Can be null to clear the user ID.
     */
    fun setUserId(userId: String?)

    /**
     * Sets a user property for the current analytics session.
     *
     * @param key The name of the user property.
     * @param value The value of the user property.
     */
    fun setUserProperty(
        key: String,
        value: String,
    )
}
