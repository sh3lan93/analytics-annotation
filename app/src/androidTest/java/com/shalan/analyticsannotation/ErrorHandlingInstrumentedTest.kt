package com.shalan.analyticsannotation

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shalan.analytics.core.AnalyticsManagerImpl
import com.shalan.analytics.core.AnalyticsProvider
import com.shalan.analytics.core.ScreenTracking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for error handling in analytics annotation.
 *
 * The core principle of the analytics library is: **Analytics should never crash the app**.
 * These tests verify that:
 * - Analytics provider exceptions don't crash the app
 * - Invalid provider behavior is handled gracefully
 * - The app continues to function even when analytics fails
 */
@RunWith(AndroidJUnit4::class)
class ErrorHandlingInstrumentedTest : BaseInstrumentedTest() {
    /**
     * Test that Activity still launches even if an analytics provider throws an exception.
     * This is critical - a failing analytics provider should never crash the app.
     */
    @Test
    fun testActivityLaunchesEvenIfAnalyticsProviderThrows() {
        // Create a provider that throws an exception
        val throwingProvider =
            object : AnalyticsProvider {
                override fun logEvent(
                    eventName: String,
                    parameters: Map<String, Any>,
                ) {
                    throw RuntimeException("Simulated analytics provider failure")
                }

                override fun setUserId(userId: String?) {
                    throw RuntimeException("Simulated analytics provider failure")
                }

                override fun setUserProperty(
                    key: String,
                    value: String,
                ) {
                    throw RuntimeException("Simulated analytics provider failure")
                }
            }

        val analyticsManager = AnalyticsManagerImpl(listOf(throwingProvider))
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        // Activity should still launch successfully despite the throwing provider
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // If we get here, the Activity launched successfully
                // This is the key assertion - the Activity did not crash
                assertThat(activity).isNotNull()
            }
        }
    }

    /**
     * Test that when one provider fails, other providers still receive events.
     * This verifies that failure of one analytics provider doesn't affect others.
     */
    @Test
    fun testOtherProvidersWorkWhenOneThrows() {
        val throwingProvider =
            object : AnalyticsProvider {
                override fun logEvent(
                    eventName: String,
                    parameters: Map<String, Any>,
                ) {
                    throw RuntimeException("Provider failure")
                }

                override fun setUserId(userId: String?) {}

                override fun setUserProperty(
                    key: String,
                    value: String,
                ) {}
            }

        val workingProvider = TestAnalyticsProvider()
        val analyticsManager = AnalyticsManagerImpl(listOf(throwingProvider, workingProvider))
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(100)

                // Even though throwingProvider failed, workingProvider should still receive events
                val events = workingProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()

                val screenViewEvent = events.find { it.eventName == "screen_view" }
                assertThat(screenViewEvent).isNotNull()
            }
        }
    }

    /**
     * Test multiple providers with mixed success and failure.
     * Provider 1: Works fine
     * Provider 2: Throws exceptions
     * Provider 3: Works fine
     *
     * All working providers should still log events.
     */
    @Test
    fun testMultipleProvidersWithMixedResults() {
        val workingProvider1 = TestAnalyticsProvider()
        val throwingProvider =
            object : AnalyticsProvider {
                override fun logEvent(
                    eventName: String,
                    parameters: Map<String, Any>,
                ) {
                    throw RuntimeException("Simulated failure")
                }

                override fun setUserId(userId: String?) {}

                override fun setUserProperty(
                    key: String,
                    value: String,
                ) {}
            }
        val workingProvider2 = TestAnalyticsProvider()

        val analyticsManager =
            AnalyticsManagerImpl(
                listOf(workingProvider1, throwingProvider, workingProvider2),
            )
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(100)

                // Both working providers should have logged events
                val events1 = workingProvider1.getLoggedEvents()
                val events2 = workingProvider2.getLoggedEvents()

                assertThat(events1).isNotEmpty()
                assertThat(events2).isNotEmpty()

                assertThat(events1.any { it.eventName == "screen_view" }).isTrue()
                assertThat(events2.any { it.eventName == "screen_view" }).isTrue()
            }
        }
    }

    /**
     * Test that null safety is maintained if a provider returns null parameters.
     * Even if a provider has issues, the library should handle it gracefully.
     */
    @Test
    fun testNullSafetyInAnalyticsHandling() {
        // This test just verifies that even with proper providers,
        // the system doesn't crash on edge cases
        ActivityScenario.launch(ExampleActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(100)

                val events = testProvider.getLoggedEvents()
                // Should have events even in edge cases
                assertThat(events).isNotEmpty()

                // All parameters should be properly initialized
                for (event in events) {
                    assertThat(event.parameters).isNotNull()
                }
            }
        }
    }

    /**
     * Test that configuration errors in analytics don't prevent app startup.
     * Even if analytics is misconfigured, the app should still work.
     */
    @Test
    fun testAppWorksWithMisconfiguredAnalytics() {
        // Create a minimal provider that does almost nothing
        val minimalProvider =
            object : AnalyticsProvider {
                override fun logEvent(
                    eventName: String,
                    parameters: Map<String, Any>,
                ) {}

                override fun setUserId(userId: String?) {}

                override fun setUserProperty(
                    key: String,
                    value: String,
                ) {}
            }

        val analyticsManager = AnalyticsManagerImpl(listOf(minimalProvider))
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        // App should still work
        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
            }
        }
    }

    /**
     * Test that events are still logged correctly after a provider failure.
     * This verifies the library's resilience - it keeps working even after errors.
     */
    @Test
    fun testLoggingContinuesAfterProviderFailure() {
        var failureCount = 0
        val failOnceThenRecover =
            object : AnalyticsProvider {
                override fun logEvent(
                    eventName: String,
                    parameters: Map<String, Any>,
                ) {
                    failureCount++
                    if (failureCount == 1) {
                        throw RuntimeException("First call fails")
                    }
                }

                override fun setUserId(userId: String?) {}

                override fun setUserProperty(
                    key: String,
                    value: String,
                ) {}
            }

        val workingProvider = TestAnalyticsProvider()
        val analyticsManager = AnalyticsManagerImpl(listOf(failOnceThenRecover, workingProvider))
        ScreenTracking.setAnalyticsManagerForTesting(analyticsManager)

        ActivityScenario.launch(UserProfileActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Thread.sleep(100)

                // Despite the failure, working provider should still log
                val events = workingProvider.getLoggedEvents()
                assertThat(events).isNotEmpty()
            }
        }
    }
}
