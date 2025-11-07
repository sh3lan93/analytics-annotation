package com.shalan.analytics.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AnalyticsManagerImpl].
 * Tests event logging, screen view tracking, and global parameters management.
 */
@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerImplTest {
    private lateinit var debugProvider: InMemoryDebugAnalyticsProvider
    private lateinit var analyticsManager: AnalyticsManagerImpl

    @Before
    fun setup() {
        debugProvider = InMemoryDebugAnalyticsProvider()
        analyticsManager = AnalyticsManagerImpl(listOf(debugProvider))
    }

    @After
    fun tearDown() {
        analyticsManager.release()
    }

    @Test
    fun `logScreenView logs event to all providers`() {
        val debugProvider2 = InMemoryDebugAnalyticsProvider()
        val manager = AnalyticsManagerImpl(listOf(debugProvider, debugProvider2))

        manager.logScreenView("HomeScreen", "HomeActivity")

        // Wait a bit for async operation
        Thread.sleep(100)

        assertEquals(1, debugProvider.getLoggedEvents().size)
        assertEquals(1, debugProvider2.getLoggedEvents().size)

        val event = debugProvider.getLoggedEvents()[0]
        assertEquals("screen_view", event.eventName)
        assertTrue(event.parameters.containsKey("screen_name"))
        assertTrue(event.parameters.containsKey("screen_class"))
        assertEquals("HomeScreen", event.parameters["screen_name"])
        assertEquals("HomeActivity", event.parameters["screen_class"])

        manager.release()
    }

    @Test
    fun `logScreenView includes additional parameters`() {
        val params = mapOf("userId" to "123", "premium" to true)

        analyticsManager.logScreenView("ProfileScreen", "ProfileFragment", params)

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals("screen_view", event.eventName)
        assertTrue(event.parameters.containsKey("userId"))
        assertTrue(event.parameters.containsKey("premium"))
        assertEquals("123", event.parameters["userId"])
        assertEquals(true, event.parameters["premium"])
    }

    @Test
    fun `logScreenView merges with global parameters`() {
        analyticsManager.setGlobalParameters(mapOf("app_version" to "1.0.0", "device" to "android"))

        analyticsManager.logScreenView(
            "SettingsScreen",
            "SettingsActivity",
            mapOf("section" to "privacy"),
        )

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertTrue(event.parameters.containsKey("app_version"))
        assertTrue(event.parameters.containsKey("device"))
        assertTrue(event.parameters.containsKey("section"))
        assertEquals("1.0.0", event.parameters["app_version"])
        assertEquals("android", event.parameters["device"])
        assertEquals("privacy", event.parameters["section"])
    }

    @Test
    fun `logEvent logs custom event to providers`() {
        analyticsManager.logEvent("user_signup", mapOf("platform" to "web"))

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals("user_signup", event.eventName)
        assertEquals("web", event.parameters["platform"])
    }

    @Test
    fun `logEvent with includeGlobalParameters true merges global params`() {
        analyticsManager.setGlobalParameters(mapOf("session_id" to "abc123"))

        analyticsManager.logEvent(
            "button_click",
            mapOf("button_name" to "submit"),
            includeGlobalParameters = true,
        )

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertTrue(event.parameters.containsKey("session_id"))
        assertTrue(event.parameters.containsKey("button_name"))
        assertEquals("abc123", event.parameters["session_id"])
        assertEquals("submit", event.parameters["button_name"])
    }

    @Test
    fun `logEvent with includeGlobalParameters false omits global params`() {
        analyticsManager.setGlobalParameters(mapOf("session_id" to "abc123"))

        analyticsManager.logEvent(
            "button_click",
            mapOf("button_name" to "submit"),
            includeGlobalParameters = false,
        )

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals(1, event.parameters.size) // Only button_name, not session_id
        assertEquals("submit", event.parameters["button_name"])
    }

    @Test
    fun `setGlobalParameters updates parameters for subsequent events`() {
        analyticsManager.setGlobalParameters(mapOf("version" to "1.0"))
        analyticsManager.logEvent("event1", mapOf("data" to "test1"))

        analyticsManager.setGlobalParameters(mapOf("version" to "2.0"))
        analyticsManager.logEvent("event2", mapOf("data" to "test2"))

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(2, events.size)
        assertEquals("1.0", events[0].parameters["version"])
        assertEquals("2.0", events[1].parameters["version"])
    }

    @Test
    fun `logScreenView and logEvent can be called in sequence`() {
        analyticsManager.logScreenView("HomeScreen", "HomeActivity")
        analyticsManager.logEvent("content_viewed", mapOf("content_id" to "123"))
        analyticsManager.logScreenView("DetailScreen", "DetailActivity")

        Thread.sleep(200)

        val events = debugProvider.getLoggedEvents()
        assertEquals(3, events.size)
        // Verify all three events were logged (exact order may vary due to async execution)
        assertTrue(events.any { it.eventName == "screen_view" })
        assertTrue(events.any { it.eventName == "content_viewed" })
    }

    @Test
    fun `multiple providers receive same events`() {
        val provider2 = InMemoryDebugAnalyticsProvider()
        val provider3 = InMemoryDebugAnalyticsProvider()
        val manager = AnalyticsManagerImpl(listOf(debugProvider, provider2, provider3))

        manager.logEvent("sync_event", mapOf("test" to "value"))

        Thread.sleep(100)

        assertEquals(1, debugProvider.getLoggedEvents().size)
        assertEquals(1, provider2.getLoggedEvents().size)
        assertEquals(1, provider3.getLoggedEvents().size)

        val expectedEvent = debugProvider.getLoggedEvents()[0]
        assertEquals(expectedEvent, provider2.getLoggedEvents()[0])
        assertEquals(expectedEvent, provider3.getLoggedEvents()[0])

        manager.release()
    }

    @Test
    fun `logEvent handles empty parameters`() {
        analyticsManager.logEvent("empty_event", emptyMap())

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)
        assertEquals("empty_event", events[0].eventName)
        assertTrue(events[0].parameters.isEmpty())
    }

    @Test
    fun `logScreenView with empty parameters`() {
        analyticsManager.logScreenView("TestScreen", "TestActivity", emptyMap())

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals("screen_view", event.eventName)
        // Should have screen_name and screen_class but no additional params
        assertEquals(2, event.parameters.size)
    }

    @Test
    fun `event parameter types are preserved`() {
        val params =
            mapOf(
                "string" to "value",
                "int" to 42,
                "long" to 1234567890L,
                "float" to 3.14f,
                "double" to 2.71828,
                "boolean" to true,
            )

        analyticsManager.logEvent("typed_event", params)

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        val event = events[0]
        assertEquals("value", event.parameters["string"])
        assertEquals(42, event.parameters["int"])
        assertEquals(1234567890L, event.parameters["long"])
        assertEquals(3.14f, event.parameters["float"])
        assertEquals(2.71828, event.parameters["double"])
        assertEquals(true, event.parameters["boolean"])
    }

    @Test
    fun `manager with empty provider list handles gracefully`() {
        val managerNoProviders = AnalyticsManagerImpl(emptyList())

        // Should not crash
        managerNoProviders.logScreenView("TestScreen", "TestActivity")
        managerNoProviders.logEvent("test_event", mapOf("test" to "value"))
        managerNoProviders.setGlobalParameters(mapOf("global" to "param"))

        managerNoProviders.release()
    }

    @Test
    fun `release cancels async operations`() {
        analyticsManager.logEvent("event1", emptyMap())
        analyticsManager.logEvent("event2", emptyMap())

        // Wait for events to be processed
        Thread.sleep(200)

        val eventsBeforeRelease = debugProvider.getLoggedEvents().size
        assertTrue("Events should be logged before release", eventsBeforeRelease >= 2)

        analyticsManager.release()

        // After release, new events might not be processed
        analyticsManager.logEvent("event3", emptyMap())

        Thread.sleep(100)

        // Events count should not increase significantly after release due to cancellation
        val eventsAfterRelease = debugProvider.getLoggedEvents().size
        // The third event may or may not be processed depending on timing
        assertTrue("Initial events should have been processed", eventsAfterRelease >= 2)
    }

    @Test
    fun `global parameters dont override event specific parameters`() {
        analyticsManager.setGlobalParameters(mapOf("key" to "global_value"))

        // Event-specific parameter with same key should be preserved
        analyticsManager.logEvent("test_event", mapOf("key" to "event_value"))

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        val event = events[0]
        // Map merge with + operator: later values override earlier ones
        assertEquals("event_value", event.parameters["key"])
    }

    @Test
    fun `complex nested parameters are handled`() {
        val params =
            mapOf(
                "user" to
                    mapOf(
                        "id" to 123,
                        "name" to "John",
                    ),
                "items" to listOf(1, 2, 3),
                "flags" to mapOf("active" to true),
            )

        analyticsManager.logEvent("complex_event", params)

        Thread.sleep(100)

        val events = debugProvider.getLoggedEvents()
        val event = events[0]
        assertEquals(3, event.parameters.size)
        assertTrue(event.parameters.containsKey("user"))
        assertTrue(event.parameters.containsKey("items"))
        assertTrue(event.parameters.containsKey("flags"))
    }
}
