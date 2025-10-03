package com.shalan.analytics.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackScreenHelperTest {
    private lateinit var testAnalyticsManager: TestAnalyticsManager

    @Before
    fun setup() {
        testAnalyticsManager = TestAnalyticsManager()
        ScreenTracking.setAnalyticsManagerForTesting(testAnalyticsManager)
    }

    @After
    fun tearDown() {
        // Clean up after each test
    }

    @Test
    fun `trackScreen logs screen view with correct parameters for non-provider instance`() {
        val instance = Any()
        val screenName = "TestScreen"
        val screenClass = "TestClass"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
        assertTrue(event.parameters.isEmpty())
    }

    @Test
    fun `trackScreen logs screen view with parameters from TrackedScreenParamsProvider`() {
        val expectedParams = mapOf("key1" to "value1", "key2" to 42)
        val instance =
            object : TrackedScreenParamsProvider {
                override fun getTrackedScreenParams(): Map<String, Any> = expectedParams
            }
        val screenName = "ProviderScreen"
        val screenClass = "ProviderClass"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
        assertEquals(expectedParams, event.parameters)
    }

    @Test
    fun `trackScreen handles empty parameters from TrackedScreenParamsProvider`() {
        val instance =
            object : TrackedScreenParamsProvider {
                override fun getTrackedScreenParams(): Map<String, Any> = emptyMap()
            }
        val screenName = "EmptyParamsScreen"
        val screenClass = "EmptyParamsClass"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
        assertTrue(event.parameters.isEmpty())
    }

    @Test
    fun `trackScreen handles complex parameter types from TrackedScreenParamsProvider`() {
        val complexParams =
            mapOf(
                "string" to "value",
                "number" to 123,
                "boolean" to true,
                "double" to 45.67,
                "list" to listOf("a", "b", "c"),
                "nested" to mapOf("inner" to "value"),
            )
        val instance =
            object : TrackedScreenParamsProvider {
                override fun getTrackedScreenParams(): Map<String, Any> = complexParams
            }
        val screenName = "ComplexParamsScreen"
        val screenClass = "ComplexParamsClass"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
        assertEquals(complexParams, event.parameters)
    }

    @Test
    fun `trackScreen silently handles exceptions without crashing`() {
        // Create a mock analytics manager that throws an exception
        val mockManager = mockk<AnalyticsManager>(relaxed = true)
        every { mockManager.logScreenView(any(), any(), any()) } throws RuntimeException("Test exception")
        ScreenTracking.setAnalyticsManagerForTesting(mockManager)

        val instance = Any()
        val screenName = "ErrorScreen"
        val screenClass = "ErrorClass"

        // Should not throw an exception
        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        // Verify that the method was called even though it threw
        verify { mockManager.logScreenView(screenName, screenClass, emptyMap()) }
    }

    @Test
    fun `trackScreen handles exception from TrackedScreenParamsProvider gracefully`() {
        val instance =
            object : TrackedScreenParamsProvider {
                override fun getTrackedScreenParams(): Map<String, Any> {
                    throw RuntimeException("Provider error")
                }
            }
        val screenName = "ProviderErrorScreen"
        val screenClass = "ProviderErrorClass"

        // Should not throw an exception
        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        // Should not have logged any events due to exception
        assertEquals(0, testAnalyticsManager.loggedEvents.size)
    }

    @Test
    fun `trackScreen works with Activity-like instance`() {
        // Simulate an Activity (which doesn't implement TrackedScreenParamsProvider)
        class MockActivity

        val instance = MockActivity()
        val screenName = "MainActivity"
        val screenClass = "MainActivity"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
        assertTrue(event.parameters.isEmpty())
    }

    @Test
    fun `trackScreen works with Fragment-like instance with parameters`() {
        // Simulate a Fragment that implements TrackedScreenParamsProvider
        class MockFragment : TrackedScreenParamsProvider {
            override fun getTrackedScreenParams(): Map<String, Any> =
                mapOf(
                    "fragmentId" to "fragment_123",
                    "source" to "navigation",
                )
        }

        val instance = MockFragment()
        val screenName = "DetailFragment"
        val screenClass = "DetailFragment"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
        assertEquals(2, event.parameters.size)
        assertEquals("fragment_123", event.parameters["fragmentId"])
        assertEquals("navigation", event.parameters["source"])
    }

    @Test
    fun `trackScreen handles special characters in screen names`() {
        val instance = Any()
        val screenName = "Screen/With\\Special:Characters"
        val screenClass = "SpecialClass"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
    }

    @Test
    fun `trackScreen handles empty screen names`() {
        val instance = Any()
        val screenName = ""
        val screenClass = "EmptyNameClass"

        TrackScreenHelper.trackScreen(instance, screenName, screenClass)

        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals(screenName, event.screenName)
        assertEquals(screenClass, event.screenClass)
    }

    @Test
    fun `trackScreen can be called multiple times from same instance`() {
        val instance = Any()

        TrackScreenHelper.trackScreen(instance, "Screen1", "Class1")
        TrackScreenHelper.trackScreen(instance, "Screen2", "Class2")
        TrackScreenHelper.trackScreen(instance, "Screen3", "Class3")

        assertEquals(3, testAnalyticsManager.loggedEvents.size)
        assertEquals("Screen1", testAnalyticsManager.loggedEvents[0].screenName)
        assertEquals("Screen2", testAnalyticsManager.loggedEvents[1].screenName)
        assertEquals("Screen3", testAnalyticsManager.loggedEvents[2].screenName)
    }

    @Test
    fun `trackScreen with TrackedScreenParamsProvider can provide dynamic parameters`() {
        var parameterValue = "initial"
        val instance =
            object : TrackedScreenParamsProvider {
                override fun getTrackedScreenParams(): Map<String, Any> =
                    mapOf("dynamicValue" to parameterValue)
            }

        TrackScreenHelper.trackScreen(instance, "DynamicScreen", "DynamicClass")
        assertEquals("initial", testAnalyticsManager.loggedEvents[0].parameters["dynamicValue"])

        parameterValue = "updated"
        TrackScreenHelper.trackScreen(instance, "DynamicScreen", "DynamicClass")
        assertEquals("updated", testAnalyticsManager.loggedEvents[1].parameters["dynamicValue"])
    }
}