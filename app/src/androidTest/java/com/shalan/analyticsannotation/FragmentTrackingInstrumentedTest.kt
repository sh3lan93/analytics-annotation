package com.shalan.analyticsannotation

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for @TrackScreen and @Track annotations on Fragments.
 *
 * These tests verify that:
 * - Fragments annotated with @TrackScreen log screen view events
 * - Custom screenName and screenClass parameters are correctly captured
 * - Fragment methods annotated with @Track log events with correct parameters
 * - Global parameters are included/excluded based on @Track configuration
 * - Fragment lifecycle events are properly tracked
 * - Events are logged to all registered analytics providers
 * - The library works with real Fragment lifecycle events
 *
 * Test Setup:
 * - ExampleActivity contains ExampleFragment
 * - ExampleFragment has @TrackScreen annotation and @Track methods
 * - This allows testing both Activity and Fragment tracking in a realistic scenario
 */
@RunWith(AndroidJUnit4::class)
class FragmentTrackingInstrumentedTest : BaseInstrumentedTest() {

    /**
     * Test that ExampleFragment with @TrackScreen annotation logs correct screen view event.
     * Verifies:
     * - Screen view event is logged when fragment is created
     * - screenName parameter is correctly set
     * - screenClass parameter is correctly set
     */
    @Test
    fun testFragmentScreenTracking() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for events to be logged (Activity + Fragment)
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()

                // Find the fragment screen view event
                val fragmentScreenViewEvent =
                    events.find { event ->
                        event.eventName == "screen_view" &&
                            event.parameters["screen_name"] == "Example Fragment"
                    }

                // Verify fragment was tracked
                assertThat(fragmentScreenViewEvent).isNotNull()
                assertThat(fragmentScreenViewEvent?.parameters).containsEntry("screen_class", "ExampleScreen")
            }
        }
    }

    /**
     * Test that both Activity and Fragment are tracked in a single scenario.
     * Verifies that Activity and Fragment screen view events are both logged.
     */
    @Test
    fun testActivityAndFragmentBothTracked() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for events (Activity onCreate + Fragment events)
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()
                val screenViewEvents = events.filter { it.eventName == "screen_view" }

                // Should have screen view events
                assertThat(screenViewEvents).isNotEmpty()

                // Verify Activity is tracked
                val activityEvent =
                    screenViewEvents.find { it.parameters["screen_name"] == "Example Activity" }
                assertThat(activityEvent).isNotNull()

                // Verify Fragment is tracked
                val fragmentEvent =
                    screenViewEvents.find { it.parameters["screen_name"] == "Example Fragment" }
                assertThat(fragmentEvent).isNotNull()
            }
        }
    }

    /**
     * Test that fragment method with @Track annotation logs correct event.
     * The init() method in ExampleFragment has @Track(eventName = "fragment_init", includeGlobalParams = true)
     * Verifies:
     * - Event name is correctly captured
     * - Global parameters are included
     */
    @Test
    fun testFragmentMethodTrackingWithGlobalParams() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for all events including method tracking
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // Find the fragment_init event
                val fragmentInitEvent = events.find { it.eventName == "fragment_init" }
                assertThat(fragmentInitEvent).isNotNull()

                // Verify global parameters are included
                assertThat(fragmentInitEvent?.parameters).containsKey("app_version")
                assertThat(fragmentInitEvent?.parameters).containsKey("session_id")
                assertThat(fragmentInitEvent?.parameters).containsKey("platform")
            }
        }
    }

    /**
     * Test that fragment method with @Track annotation and includeGlobalParams=false
     * logs event without global parameters.
     * The setup() method has @Track(eventName = "fragment_setup", includeGlobalParams = false)
     * Verifies:
     * - Event name is correctly captured
     * - Global parameters are NOT included
     */
    @Test
    fun testFragmentMethodTrackingWithoutGlobalParams() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for all events
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // Find the fragment_setup event
                val fragmentSetupEvent = events.find { it.eventName == "fragment_setup" }
                assertThat(fragmentSetupEvent).isNotNull()

                // Verify global parameters are NOT included
                assertThat(fragmentSetupEvent?.parameters).doesNotContainKey("app_version")
                assertThat(fragmentSetupEvent?.parameters).doesNotContainKey("session_id")
                assertThat(fragmentSetupEvent?.parameters).doesNotContainKey("platform")
            }
        }
    }

    /**
     * Test that all expected events are logged for fragment lifecycle.
     * When ExampleActivity is launched with ExampleFragment, we should see:
     * 1. Activity screen_view event
     * 2. Fragment screen_view event
     * 3. Fragment method events (init, setup)
     */
    @Test
    fun testFragmentCompleteLifecycleTracking() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for sufficient events
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // Verify we have multiple events
                assertThat(events.size).isAtLeast(3)

                // Count event types
                val screenViewEvents = events.filter { it.eventName == "screen_view" }
                val methodEvents =
                    events.filter { it.eventName in listOf("fragment_init", "fragment_setup") }

                // Should have at least one screen view event (Activity or Fragment)
                assertThat(screenViewEvents).isNotEmpty()

                // Should have method tracking events
                assertThat(methodEvents).isNotEmpty()
            }
        }
    }

    /**
     * Test that fragment screen view event contains all required fields.
     * Verifies the event structure is correct.
     */
    @Test
    fun testFragmentScreenViewEventStructure() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()
                val fragmentScreenViewEvent =
                    events.find { event ->
                        event.eventName == "screen_view" &&
                            event.parameters["screen_name"] == "Example Fragment"
                    }

                assertThat(fragmentScreenViewEvent).isNotNull()
                assertThat(fragmentScreenViewEvent?.eventName).isEqualTo("screen_view")
                assertThat(fragmentScreenViewEvent?.parameters.orEmpty()).containsKey("screen_name")
                assertThat(fragmentScreenViewEvent?.parameters.orEmpty()).containsKey("screen_class")
            }
        }
    }

    /**
     * Test that multiple method calls on fragment are tracked separately.
     * Both init() and setup() methods should log separate events.
     */
    @Test
    fun testMultipleFragmentMethodCallsTracked() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Wait for enough events (screen views + both method calls)
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // Both methods should be tracked
                val initEventCount = testProvider.getEventCount("fragment_init")
                val setupEventCount = testProvider.getEventCount("fragment_setup")

                assertThat(initEventCount).isAtLeast(1)
                assertThat(setupEventCount).isAtLeast(1)
            }
        }
    }

    /**
     * Test that global parameters are correctly included in screen view events.
     * Screen view events should have global parameters by default.
     */
    @Test
    fun testFragmentScreenViewIncludesGlobalParameters() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()
                val fragmentScreenViewEvent =
                    events.find { event ->
                        event.eventName == "screen_view" &&
                            event.parameters["screen_name"] == "Example Fragment"
                    }

                // Global parameters should be included in screen view
                assertThat(fragmentScreenViewEvent?.parameters).containsKey("app_version")
                assertThat(fragmentScreenViewEvent?.parameters).containsKey("session_id")
                assertThat(fragmentScreenViewEvent?.parameters).containsKey("platform")
            }
        }
    }

    /**
     * Test that fragment tracking works correctly in an Activity with Container.
     * Specifically tests ExampleActivity which inflates Fragment in onCreate.
     */
    @Test
    fun testFragmentInflatedInActivityContainer() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // The Activity should have successfully inflated the Fragment
                assertThat(activity).isNotNull()

                // Wait for tracking events
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // Verify both Activity and Fragment were tracked
                val allScreenNames = events.mapNotNull { it.parameters["screen_name"] as? String }
                assertThat(allScreenNames).contains("Example Activity")
                assertThat(allScreenNames).contains("Example Fragment")
            }
        }
    }

    /**
     * Test that fragment events maintain correct event structure across multiple scenarios.
     * Verifies consistency of event data.
     */
    @Test
    fun testFragmentEventConsistency() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // All screen_view events should have required fields
                events.filter { it.eventName == "screen_view" }.forEach { event ->
                    assertThat(event.parameters).containsKey("screen_name")
                    assertThat(event.parameters).containsKey("screen_class")
                }

                // All method tracking events should exist
                val methodEvents =
                    events.filter { it.eventName in listOf("fragment_init", "fragment_setup") }
                assertThat(methodEvents).isNotEmpty()
            }
        }
    }

    /**
     * Test that fragment analytics doesn't interfere with normal fragment functionality.
     * The fragment should still work normally with analytics enabled.
     */
    @Test
    fun testFragmentFunctionalityWithAnalyticsEnabled() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Activity should be properly initialized
                assertThat(activity).isNotNull()

                // Container should exist and have content
                val fragmentContainer =
                    activity.findViewById<android.view.ViewGroup>(R.id.fragment_container)
                assertThat(fragmentContainer).isNotNull()

                // Analytics should have tracked the events
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)
                val events = testProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()
            }
        }
    }

    /**
     * Test that only one screen_view event is logged per fragment creation.
     * Verifies no duplicate tracking occurs.
     */
    @Test
    fun testSingleFragmentCreationLogsOneScreenViewEvent() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AnalyticsTestHelper.waitForEvents(testProvider, eventCount = 3)

                val events = testProvider.getLoggedEvents()

                // Count fragment screen view events
                val fragmentScreenViewEvents =
                    events.filter { event ->
                        event.eventName == "screen_view" &&
                            event.parameters["screen_name"] == "Example Fragment"
                    }

                // Should have exactly one fragment screen view event
                assertThat(fragmentScreenViewEvents).hasSize(1)
            }
        }
    }
}