package com.shalan.analyticsannotation

import com.shalan.analytics.core.AnalyticsEvent
import com.shalan.analytics.core.DebugAnalyticsProvider

/**
 * A test implementation of [DebugAnalyticsProvider] for instrumented tests.
 *
 * This provider stores all logged events in memory, allowing test code to verify
 * that the correct analytics events were tracked with the correct parameters.
 * It's designed to work seamlessly with instrumented tests and Activity/Fragment lifecycle events.
 */
class TestAnalyticsProvider : DebugAnalyticsProvider {
    private val loggedEvents = mutableListOf<AnalyticsEvent>()
    private var mockModeEnabled = false

    override fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
    ) {
        if (!mockModeEnabled) {
            loggedEvents.add(AnalyticsEvent(eventName, parameters))
        }
    }

    override fun setUserId(userId: String?) {
        // No-op for testing
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        // No-op for testing
    }

    override fun getLoggedEvents(): List<AnalyticsEvent> {
        return loggedEvents.toList()
    }

    override fun clearLoggedEvents() {
        loggedEvents.clear()
    }

    override fun setMockMode(enabled: Boolean) {
        mockModeEnabled = enabled
    }

    /**
     * Helper method to assert that an event with a specific name was logged.
     */
    fun assertEventLogged(eventName: String) {
        val found = loggedEvents.any { it.eventName == eventName }
        require(found) { "Expected event '$eventName' not found in logged events. Events: ${loggedEvents.map { it.eventName }}" }
    }

    /**
     * Helper method to assert that a specific event with specific parameters was logged.
     */
    fun assertEventLoggedWithParams(
        eventName: String,
        expectedParams: Map<String, Any>,
    ) {
        val found =
            loggedEvents.any { event ->
                event.eventName == eventName && event.parameters == expectedParams
            }
        require(found) {
            "Expected event '$eventName' with params $expectedParams not found. " +
                "Events: ${loggedEvents.map { "${it.eventName}: ${it.parameters}" }}"
        }
    }

    /**
     * Helper method to assert that a specific parameter was set in an event.
     */
    fun assertEventHasParameter(
        eventName: String,
        paramKey: String,
        paramValue: Any,
    ) {
        val event = loggedEvents.find { it.eventName == eventName }
        require(event != null) { "Event '$eventName' not found" }
        require(event.parameters[paramKey] == paramValue) {
            "Event '$eventName' parameter '$paramKey' = '${event.parameters[paramKey]}', expected '$paramValue'"
        }
    }

    /**
     * Get the count of events with a specific name.
     */
    fun getEventCount(eventName: String): Int {
        return loggedEvents.count { it.eventName == eventName }
    }
}
