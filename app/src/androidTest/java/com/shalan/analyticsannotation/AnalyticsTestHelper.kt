package com.shalan.analyticsannotation

import androidx.test.espresso.IdlingRegistry

/**
 * Helper class for managing analytics idling resources in tests.
 *
 * Provides convenient methods for waiting on analytics events without using Thread.sleep().
 */
object AnalyticsTestHelper {
    /**
     * Wait for analytics events to be logged.
     * Creates a temporary idling resource, waits for events, then unregisters it.
     *
     * @param provider The test analytics provider to monitor
     * @param timeoutMs Maximum time to wait in milliseconds (default 5000ms)
     * @param eventCount Number of events to wait for (default 1)
     */
    fun waitForEvents(
        provider: TestAnalyticsProvider,
        timeoutMs: Long = 5000,
        eventCount: Int = 1,
    ) {
        val idlingResource =
            AnalyticsIdlingResource("AnalyticsEvents") {
                provider.getLoggedEvents().size >= eventCount
            }

        IdlingRegistry.getInstance().register(idlingResource)
        try {
            waitWithTimeout(timeoutMs) {
                provider.getLoggedEvents().size >= eventCount
            }
        } finally {
            IdlingRegistry.getInstance().unregister(idlingResource)
        }
    }

    /**
     * Wait for a specific event to be logged.
     *
     * @param provider The test analytics provider to monitor
     * @param eventName The name of the event to wait for
     * @param timeoutMs Maximum time to wait in milliseconds (default 5000ms)
     */
    fun waitForEventNamed(
        provider: TestAnalyticsProvider,
        eventName: String,
        timeoutMs: Long = 5000,
    ) {
        val idlingResource =
            AnalyticsIdlingResource("AnalyticsEvent:$eventName") {
                provider.getLoggedEvents().any { it.eventName == eventName }
            }

        IdlingRegistry.getInstance().register(idlingResource)
        try {
            waitWithTimeout(timeoutMs) {
                provider.getLoggedEvents().any { it.eventName == eventName }
            }
        } finally {
            IdlingRegistry.getInstance().unregister(idlingResource)
        }
    }

    /**
     * Wait for a condition with timeout.
     * This is a utility for fallback scenarios where idling resources aren't enough.
     */
    private fun waitWithTimeout(
        timeoutMs: Long,
        condition: () -> Boolean,
    ) {
        val startTime = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - startTime < timeoutMs) {
            Thread.sleep(50) // Poll every 50ms
        }

        if (!condition()) {
            throw AssertionError("Timeout waiting for condition after ${timeoutMs}ms")
        }
    }

    /**
     * Clear all idling resources (useful for test cleanup).
     */
    fun clearAllIdlingResources() {
        try {
            val registry = IdlingRegistry.getInstance()
            // Get all registered resources and unregister them
            val resources = registry.javaClass.getDeclaredField("mIdlingResources")
            resources.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val idlingResources = resources.get(registry) as? List<*>
            idlingResources?.forEach { resource ->
                if (resource is androidx.test.espresso.IdlingResource) {
                    try {
                        registry.unregister(resource)
                    } catch (e: Exception) {
                        // Ignore errors during cleanup
                    }
                }
            }
        } catch (e: Exception) {
            // Reflection may fail, but that's okay
        }
    }
}
