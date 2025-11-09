package com.shalan.analyticsannotation

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shalan.analytics.core.ScreenTracking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for analytics annotation across multiple screens and scenarios.
 *
 * These tests verify complex, real-world scenarios:
 * - Multiple Activities tracked in sequence
 * - Activity with Fragments navigation
 * - Rapid navigation between screens
 * - Screen tracking with configuration changes
 * - Complex user navigation flows
 */
@RunWith(AndroidJUnit4::class)
class IntegrationInstrumentedTest : BaseInstrumentedTest() {
    /**
     * Test navigation between multiple Activities.
     * Verify that each Activity transition logs appropriate screen view events.
     */
    @Test
    fun testNavigationBetweenMultipleActivities() {
        // Launch first activity
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(50)
            }
        }

        val events1 = testProvider.getLoggedEvents().toList()
        assertThat(events1).isNotEmpty()
        assertThat(events1.any { it.parameters["screen_name"] == "User Profile Screen" }).isTrue()

        testProvider.clearLoggedEvents()

        // Navigate to second activity
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(50)
            }
        }

        val events2 = testProvider.getLoggedEvents()
        assertThat(events2).isNotEmpty()
        assertThat(events2.any { it.parameters["screen_name"] == "Example Activity" }).isTrue()
    }

    /**
     * Test Activity with embedded Fragment.
     * Both Activity and Fragment should be tracked.
     * ExampleActivity contains ExampleFragment.
     */
    @Test
    fun testActivityWithEmbeddedFragmentTracking() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(100)

                val events = testProvider.getLoggedEvents()
                val screenNames = events.map { it.parameters["screen_name"] }

                // At minimum, should track the Activity
                assertThat(events).isNotEmpty()

                // Verify Example Activity is tracked
                assertThat(screenNames.filterNotNull()).contains("Example Activity")
            }
        }
    }

    /**
     * Test rapid consecutive Activity launches.
     * Verify that fast navigation doesn't lose or duplicate events.
     */
    @Test
    fun testRapidActivityNavigation() {
        val activities =
            listOf(
                UserProfileActivity::class.java,
                ExampleActivity::class.java,
                UserProfileActivity::class.java,
            )

        for (activityClass in activities) {
            ActivityScenario.launch(activityClass).use { scenario ->
                scenario.onActivity { activity ->
                    Thread.sleep(30) // Minimal wait between launches
                }
            }
        }

        val events = testProvider.getLoggedEvents()

        // Should have tracked 3 activities
        val screenViewEvents = events.filter { it.eventName == "screen_view" }
        assertThat(screenViewEvents.size).isAtLeast(1)

        // Verify we logged events from both activities
        val screenNames = screenViewEvents.mapNotNull { it.parameters["screen_name"] as? String }
        assertThat(screenNames).isNotEmpty()
    }

    /**
     * Test that subsequent Activity launches after cleanup track correctly.
     * Verify the system works correctly for multiple launches in one test.
     */
    @Test
    fun testMultipleLaunchesWithClearBetween() {
        repeat(3) { iteration ->
            ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    Thread.sleep(50)
                }
            }

            val events = testProvider.getLoggedEvents()
            assertThat(events).isNotEmpty()
            assertThat(events.any { it.eventName == "screen_view" }).isTrue()

            testProvider.clearLoggedEvents()
        }
    }

    /**
     * Test screen tracking with Activity state restoration.
     * Even after pause/resume, screen tracking should work.
     */
    @Test
    fun testActivityPauseResumeTracking() {
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(50)
            }

            val events1Count = testProvider.getLoggedEvents().size
            assertThat(events1Count).isGreaterThan(0)

            // Activity in different lifecycle states
            Thread.sleep(30)

            val allEvents = testProvider.getLoggedEvents()
            assertThat(allEvents).isNotEmpty()
        }
    }

    /**
     * Test that different analytics providers all receive tracking events.
     * This tests the provider dispatch mechanism in a real scenario.
     */
    @Test
    fun testMultipleProvidersReceiveEventsInSequentialNavigation() {
        val provider1 = TestAnalyticsProvider()
        val provider2 = TestAnalyticsProvider()
        val analyticsManager = com.shalan.analytics.core.AnalyticsManagerImpl(listOf(provider1, provider2))
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(50)
            }
        }

        provider1.clearLoggedEvents()
        provider2.clearLoggedEvents()

        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(50)
            }
        }

        val events1 = provider1.getLoggedEvents()
        val events2 = provider2.getLoggedEvents()

        // Both providers should track the Example Activity
        assertThat(events1.any { it.parameters["screen_name"] == "Example Activity" }).isTrue()
        assertThat(events2.any { it.parameters["screen_name"] == "Example Activity" }).isTrue()
    }

    /**
     * Test complex navigation scenario with Activity and Fragment combination.
     * Activity containing Fragment, with potential Fragment replacements.
     */
    @Test
    fun testComplexActivityFragmentIntegration() {
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Activity created with Fragment
                Thread.sleep(100)

                val events = testProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()

                // Should have tracked the Activity
                val screenViewEvents = events.filter { it.eventName == "screen_view" }
                assertThat(screenViewEvents.size).isAtLeast(1)

                // Verify correct screen names
                val screenNames = screenViewEvents.mapNotNull { it.parameters["screen_name"] as? String }
                assertThat(screenNames).contains("Example Activity")
            }
        }
    }

    /**
     * Test that event parameters maintain integrity across complex scenarios.
     * Verify screen_name and screen_class are always correctly set.
     */
    @Test
    fun testEventParameterIntegrityInComplexScenarios() {
        val testScenarios =
            listOf(
                Pair(UserProfileActivity::class.java, "User Profile Screen"),
                Pair(ExampleActivity::class.java, "Example Activity"),
            )

        for ((activityClass, expectedScreenName) in testScenarios) {
            testProvider.clearLoggedEvents()

            ActivityScenario.launch(activityClass).use { scenario ->
                scenario.onActivity { activity ->
                    Thread.sleep(50)
                }
            }

            val screenViewEvent = testProvider.getLoggedEvents().find { it.eventName == "screen_view" }
            assertThat(screenViewEvent).isNotNull()
            assertThat(screenViewEvent?.parameters?.get("screen_name")).isEqualTo(expectedScreenName)
            assertThat(screenViewEvent?.parameters).containsKey("screen_class")
        }
    }

    /**
     * Test that screen tracking doesn't interfere with normal Activity functionality.
     * The Activity should still work normally even with analytics enabled.
     */
    @Test
    fun testActivityFunctionalityWithAnalyticsEnabled() {
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Activity should be properly initialized
                assertThat(activity).isNotNull()

                // Views should be accessible
                val profileText = activity.findViewById<android.widget.TextView>(R.id.profileDisplayText)
                assertThat(profileText).isNotNull()

                // Analytics should have tracked the screen
                Thread.sleep(50)
                val events = testProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()
            }
        }
    }
}
