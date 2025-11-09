package com.shalan.analyticsannotation

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shalan.analytics.core.ScreenTracking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for @TrackScreen annotation on Activities.
 *
 * These tests verify that:
 * - Activities annotated with @TrackScreen log screen view events
 * - Custom screenName and screenClass parameters are correctly captured
 * - Events are logged to all registered analytics providers
 * - The library works with real Activity lifecycle events
 */
@RunWith(AndroidJUnit4::class)
class ActivityTrackingInstrumentedTest : BaseInstrumentedTest() {
    /**
     * Test that UserProfileActivity with @TrackScreen annotation logs correct screen view event.
     * This tests:
     * - Custom screenName parameter
     * - Custom screenClass parameter
     * - Screen view event is logged
     */
    @Test
    fun testUserProfileActivityTracking() {
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for screen view event using idling resource (not Thread.sleep)
                AnalyticsTestHelper.waitForEventNamed(testProvider, "screen_view")

                // Verify screen view event was logged
                val events = testProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()

                // Check for screen view event
                val screenViewEvent = events.find { it.eventName == "screen_view" }
                assertThat(screenViewEvent).isNotNull()

                // Verify screen name parameter
                assertThat(screenViewEvent?.parameters).containsEntry("screen_name", "User Profile Screen")

                // Verify screen class parameter
                assertThat(screenViewEvent?.parameters).containsEntry("screen_class", "UserProfile")
            }
        }
    }

    /**
     * Test that ExampleActivity logs screen view event with correct parameters.
     */
    @Test
    fun testExampleActivityTracking() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEventNamed(testProvider, "screen_view")

                val events = testProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()

                val screenViewEvent = events.find { it.eventName == "screen_view" }
                assertThat(screenViewEvent).isNotNull()

                assertThat(screenViewEvent?.parameters).containsEntry("screen_name", "Example Activity")
                assertThat(screenViewEvent?.parameters).containsEntry("screen_class", "ExampleScreen")
            }
        }
    }

    /**
     * Test that screen view event contains expected structure.
     * The event should have:
     * - eventName: "screen_view"
     * - parameters: Map containing screen_name and screen_class
     */
    @Test
    fun testScreenViewEventStructure() {
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEventNamed(testProvider, "screen_view")

                val events = testProvider.getLoggedEvents()
                val screenViewEvent = events.find { it.eventName == "screen_view" }

                assertThat(screenViewEvent).isNotNull()
                assertThat(screenViewEvent?.eventName).isEqualTo("screen_view")
                assertThat(screenViewEvent?.parameters.orEmpty()).containsKey("screen_name")
                assertThat(screenViewEvent?.parameters.orEmpty()).containsKey("screen_class")
            }
        }
    }

    /**
     * Test that only one screen view event is logged for a single Activity creation.
     * This verifies that the annotation processing doesn't cause duplicate events.
     */
    @Test
    fun testSingleActivityCreationLogsOneEvent() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 1)

                val screenViewEvents =
                    testProvider.getLoggedEvents().filter { it.eventName == "screen_view" }
                        .filter { it.parameters["screen_name"] == "Example Activity" }
                assertThat(screenViewEvents).hasSize(1)
            }
        }
    }

    /**
     * Test with multiple analytics providers to ensure events go to all providers.
     * This verifies the AnalyticsManager correctly dispatches to multiple providers.
     */
    @Test
    fun testMultipleAnalyticsProvidersReceiveEvent() {
        val provider1 = TestAnalyticsProvider()
        val provider2 = TestAnalyticsProvider()
        val analyticsManager = com.shalan.analytics.core.AnalyticsManagerImpl(listOf(provider1, provider2))
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEvents(provider1, eventCount = 1)
                AnalyticsTestHelper.waitForEvents(provider2, eventCount = 1)

                // Verify both providers received the event
                val events1 = provider1.getLoggedEvents()
                val events2 = provider2.getLoggedEvents()

                assertThat(events1).isNotEmpty()
                assertThat(events2).isNotEmpty()

                val screenEvent1 = events1.find { it.eventName == "screen_view" }
                val screenEvent2 = events2.find { it.eventName == "screen_view" }

                assertThat(screenEvent1).isNotNull()
                assertThat(screenEvent2).isNotNull()
            }
        }
    }

    /**
     * Test sequential Activity launches track separate events.
     * This verifies that launching different activities logs events for each one.
     */
    @Test
    fun testSequentialActivityLaunchesLogSeparateEvents() {
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEventNamed(testProvider, "screen_view")
            }
        }

        testProvider.clearLoggedEvents()

        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEventNamed(testProvider, "screen_view")

                val events = testProvider.getLoggedEvents()
                val screenViewEvent = events.find { it.eventName == "screen_view" }

                // Should have logged event for ExampleActivity
                assertThat(screenViewEvent?.parameters).containsEntry("screen_name", "Example Activity")
            }
        }
    }
}
