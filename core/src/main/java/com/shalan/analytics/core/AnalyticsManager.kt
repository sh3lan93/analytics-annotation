package com.shalan.analytics.core

/**
 * Defines the contract for an analytics manager responsible for logging screen view events.
 * Implementations of this interface will typically dispatch events to one or more [AnalyticsProvider]s.
 */
interface AnalyticsManager {
    /**
     * Logs a screen view event.
     *
     * @param screenName The name of the screen being viewed (e.g., "Home Screen", "Product Detail").
     * @param screenClass The class name associated with the screen (e.g., "MainActivity", "ProductFragment").
     * @param parameters An optional map of additional key-value pairs to be sent with the event.
     */
    fun logScreenView(
        screenName: String,
        screenClass: String,
        parameters: Map<String, Any> = emptyMap(),
    )

    /**
     * Logs a custom event.
     *
     * @param eventName The name of the event to log.
     * @param parameters A map of key-value pairs representing the event parameters.
     */
    fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
        includeGlobalParameters: Boolean = true,
    )

    /**
     * Releases any resources held by the analytics manager.
     * This should be called when the application is no longer needing analytics, e.g., on application shutdown.
     */
    fun release()

    /**
     * Sets global parameters that will be included with all subsequent analytics events.
     * These parameters are merged with event-specific parameters, with event-specific
     * parameters taking precedence in case of key conflicts.
     *
     * @param parameters A map of key-value pairs representing the global parameters.
     */
    fun setGlobalParameters(parameters: Map<String, Any>)
}
