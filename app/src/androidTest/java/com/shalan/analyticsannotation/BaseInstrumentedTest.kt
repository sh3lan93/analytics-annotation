package com.shalan.analyticsannotation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.shalan.analytics.core.ScreenTracking
import com.shalan.analytics.core.analyticsConfig
import org.junit.After
import org.junit.Before

/**
 * Base class for all instrumented tests in the analytics annotation app.
 *
 * Simulates the actual app initialization from [SampleApp] by:
 * - Using ScreenTracking.initialize() with analyticsConfig (matching real app setup)
 * - Configuring TestAnalyticsProvider for event capture
 * - Setting up global parameters
 * - Providing common setup and teardown
 *
 * This approach ensures tests validate the library as it would be used in production.
 */
open class BaseInstrumentedTest {
    protected lateinit var app: Application
    protected lateinit var testProvider: TestAnalyticsProvider

    @Before
    open fun setUp() {
        app = ApplicationProvider.getApplicationContext()

        // Create a fresh test analytics provider for this test
        testProvider = TestAnalyticsProvider()

        // Initialize ScreenTracking with the same pattern as SampleApp.onCreate()
        // This ensures tests validate the library as it would be used in a real app
        ScreenTracking.initialize(
            config =
                analyticsConfig {
                    debugMode = true
                    providers.add(testProvider)
                },
        )

        // Set global parameters like SampleApp does
        // This simulates the real application environment
        ScreenTracking.setGlobalParameters(
            mapOf(
                "app_version" to "1.0.0",
                "test_environment" to "instrumented_test",
                "session_id" to "test_session_${System.currentTimeMillis()}",
                "platform" to "Android",
            ),
        )
    }

    @After
    open fun tearDown() {
        // Clear all logged events after each test
        testProvider.clearLoggedEvents()
    }

    /**
     * Helper to wait for async event logging.
     * Some analytics operations may be async, so we need to wait a bit.
     */
    protected fun waitForAsyncOperations(delayMs: Long = 100) {
        Thread.sleep(delayMs)
    }
}
