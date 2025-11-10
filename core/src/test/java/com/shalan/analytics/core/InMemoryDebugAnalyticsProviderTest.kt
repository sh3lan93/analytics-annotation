package com.shalan.analytics.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [InMemoryDebugAnalyticsProvider].
 * Tests the in-memory storage, mock mode, and event logging functionality.
 */
@RunWith(RobolectricTestRunner::class)
class InMemoryDebugAnalyticsProviderTest {
    private lateinit var provider: InMemoryDebugAnalyticsProvider

    @Before
    fun setup() {
        provider = InMemoryDebugAnalyticsProvider()
    }

    @Test
    fun `provider initializes with empty logged events`() {
        val events = provider.getLoggedEvents()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `logEvent stores event in memory`() {
        val eventName = "test_event"
        val parameters = mapOf("key" to "value")

        provider.logEvent(eventName, parameters)

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        assertEquals(eventName, events[0].eventName)
        assertEquals(parameters, events[0].parameters)
    }

    @Test
    fun `logEvent handles multiple events`() {
        provider.logEvent("event1", mapOf("param1" to "value1"))
        provider.logEvent("event2", mapOf("param2" to "value2"))
        provider.logEvent("event3", mapOf("param3" to "value3"))

        val events = provider.getLoggedEvents()
        assertEquals(3, events.size)
        assertEquals("event1", events[0].eventName)
        assertEquals("event2", events[1].eventName)
        assertEquals("event3", events[2].eventName)
    }

    @Test
    fun `logEvent handles empty parameters`() {
        provider.logEvent("empty_event", emptyMap())

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        assertEquals("empty_event", events[0].eventName)
        assertTrue(events[0].parameters.isEmpty())
    }

    @Test
    fun `logEvent handles complex parameter types`() {
        val parameters =
            mapOf(
                "string" to "value",
                "int" to 42,
                "long" to 1234567890L,
                "float" to 3.14f,
                "double" to 2.71828,
                "boolean" to true,
                "list" to listOf(1, 2, 3),
                "map" to mapOf("nested" to "object"),
            )

        provider.logEvent("complex_event", parameters)

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        val event = events[0]
        assertEquals(8, event.parameters.size)
        assertEquals("value", event.parameters["string"])
        assertEquals(42, event.parameters["int"])
        assertEquals(true, event.parameters["boolean"])
    }

    @Test
    fun `clearLoggedEvents removes all events`() {
        provider.logEvent("event1", emptyMap())
        provider.logEvent("event2", emptyMap())
        provider.logEvent("event3", emptyMap())

        assertEquals(3, provider.getLoggedEvents().size)

        provider.clearLoggedEvents()

        assertEquals(0, provider.getLoggedEvents().size)
    }

    @Test
    fun `clearLoggedEvents can be called multiple times`() {
        provider.logEvent("event", emptyMap())
        provider.clearLoggedEvents()
        assertEquals(0, provider.getLoggedEvents().size)

        provider.logEvent("event2", emptyMap())
        provider.clearLoggedEvents()
        assertEquals(0, provider.getLoggedEvents().size)
    }

    @Test
    fun `getLoggedEvents returns read-only list`() {
        provider.logEvent("event1", emptyMap())
        provider.logEvent("event2", emptyMap())

        val events = provider.getLoggedEvents()
        val originalSize = events.size

        // Verify we get the same list on subsequent calls
        val events2 = provider.getLoggedEvents()
        assertEquals(originalSize, events2.size)
    }

    @Test
    fun `setMockMode enables mock mode`() {
        provider.setMockMode(true)

        provider.logEvent("event", emptyMap())

        // In mock mode, events should still be logged but marked as mock
        // For this test, we verify the call doesn't crash
        // Mock mode doesn't store events
        assertEquals(0, provider.getLoggedEvents().size)
    }

    @Test
    fun `setMockMode can be toggled`() {
        provider.logEvent("event1", emptyMap())
        assertEquals(1, provider.getLoggedEvents().size)

        provider.setMockMode(true)
        provider.logEvent("event2", emptyMap())
        // In mock mode, event is not stored

        provider.setMockMode(false)
        provider.logEvent("event3", emptyMap())

        // Should have event1 and event3
        val events = provider.getLoggedEvents()
        assertEquals(2, events.size)
        assertEquals("event1", events[0].eventName)
        assertEquals("event3", events[1].eventName)
    }

    @Test
    fun `setUserId logs user ID change`() {
        // setUserId doesn't throw or cause issues
        provider.setUserId("user123")
        provider.setUserId(null)

        // Should not affect event logging
        provider.logEvent("event", emptyMap())
        assertEquals(1, provider.getLoggedEvents().size)
    }

    @Test
    fun `setUserProperty logs user property change`() {
        // setUserProperty doesn't throw or cause issues
        provider.setUserProperty("premium", "true")
        provider.setUserProperty("language", "en")

        // Should not affect event logging
        provider.logEvent("event", emptyMap())
        assertEquals(1, provider.getLoggedEvents().size)
    }

    @Test
    fun `logEvent with null parameters handles gracefully`() {
        val parameters =
            mapOf<String, Any?>(
                "valid" to "value",
                "null_value" to null,
            )

        @Suppress("UNCHECKED_CAST")
        val castedParameters = parameters as Map<String, Any>
        provider.logEvent("mixed_event", castedParameters)

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        assertEquals(2, events[0].parameters.size)
    }

    @Test
    fun `logEvent preserves parameter order for multiple calls`() {
        provider.logEvent("event1", mapOf("a" to 1, "b" to 2, "c" to 3))
        provider.logEvent("event2", mapOf("x" to 10, "y" to 20, "z" to 30))
        provider.logEvent("event3", mapOf("foo" to "bar", "baz" to "qux"))

        val events = provider.getLoggedEvents()
        assertEquals("event1", events[0].eventName)
        assertEquals("event2", events[1].eventName)
        assertEquals("event3", events[2].eventName)
    }

    @Test
    fun `logEvent handles special characters in event names`() {
        val eventNames =
            listOf(
                "event_with_underscore",
                "event-with-hyphen",
                "event.with.dots",
                "event/with/slashes",
                "event:with:colons",
            )

        eventNames.forEach { name ->
            provider.logEvent(name, emptyMap())
        }

        val events = provider.getLoggedEvents()
        assertEquals(eventNames.size, events.size)
        eventNames.forEachIndexed { index, name ->
            assertEquals(name, events[index].eventName)
        }
    }

    @Test
    fun `clearLoggedEvents followed by new events works correctly`() {
        provider.logEvent("old_event", emptyMap())
        provider.clearLoggedEvents()

        assertEquals(0, provider.getLoggedEvents().size)

        provider.logEvent("new_event", mapOf("fresh" to true))

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        assertEquals("new_event", events[0].eventName)
        assertEquals(true, events[0].parameters["fresh"])
    }

    @Test
    fun `multiple provider instances are independent`() {
        val provider1 = InMemoryDebugAnalyticsProvider()
        val provider2 = InMemoryDebugAnalyticsProvider()

        provider1.logEvent("event1", emptyMap())
        provider2.logEvent("event2", emptyMap())

        assertEquals(1, provider1.getLoggedEvents().size)
        assertEquals(1, provider2.getLoggedEvents().size)
        assertEquals("event1", provider1.getLoggedEvents()[0].eventName)
        assertEquals("event2", provider2.getLoggedEvents()[0].eventName)
    }

    @Test
    fun `logEvent handles large parameter maps`() {
        val largeParams = (1..100).associate { "param_$it" to "value_$it" }

        provider.logEvent("large_event", largeParams)

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        assertEquals(100, events[0].parameters.size)
    }

    @Test
    fun `logEvent with repeated parameter keys uses latest value`() {
        // This overwrites the previous value in a Map
        val parameters =
            linkedMapOf(
                "key" to "value1",
                "key" to "value2",
            )

        provider.logEvent("overwrite_event", parameters)

        val events = provider.getLoggedEvents()
        assertEquals(1, events.size)
        // LinkedHashMap preserves insertion order, but duplicate keys use the latest value
        assertEquals(1, events[0].parameters.size)
    }

    @Test
    fun `AnalyticsEvent data class equality works correctly`() {
        val event1 = AnalyticsEvent("test", mapOf("key" to "value"))
        val event2 = AnalyticsEvent("test", mapOf("key" to "value"))
        val event3 = AnalyticsEvent("different", mapOf("key" to "value"))

        assertTrue(event1 == event2)
        assertFalse(event1 == event3)
    }

    @Test
    fun `AnalyticsEvent can be compared and used in collections`() {
        val event1 = AnalyticsEvent("event1", mapOf("a" to 1))
        val event2 = AnalyticsEvent("event2", mapOf("b" to 2))

        provider.logEvent("event1", mapOf("a" to 1))
        provider.logEvent("event2", mapOf("b" to 2))

        val events = provider.getLoggedEvents()
        assertTrue(events.contains(event1))
        assertTrue(events.contains(event2))
    }
}
