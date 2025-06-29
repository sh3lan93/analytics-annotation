package com.shalan.analytics.core

class TestAnalyticsManager : AnalyticsManager {
    val loggedEvents = mutableListOf<LoggedEvent>()

    override fun logScreenView(
        screenName: String,
        screenClass: String,
        parameters: Map<String, Any>,
    ) {
        loggedEvents.add(LoggedEvent(screenName, screenClass, parameters))
    }

    override fun logEvent(
        eventName: String,
        parameters: Map<String, Any>,
    ) {
        // Not needed for testing
    }

    override fun setGlobalParameters(parameters: Map<String, Any>) {
        // Not needed for testing
    }

    override fun release() {
        // Not needed for testing
    }

    data class LoggedEvent(
        val screenName: String,
        val screenClass: String,
        val parameters: Map<String, Any>,
    )
}
