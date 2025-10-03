package com.shalan.analytics.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shalan.analytics.core.ScreenTracking
import com.shalan.analytics.core.TestAnalyticsManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackScreenOnceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testAnalyticsManager: TestAnalyticsManager

    @Before
    fun setup() {
        testAnalyticsManager = TestAnalyticsManager()
        ScreenTracking.setAnalyticsManagerForTesting(testAnalyticsManager)
    }

    @After
    fun tearDown() {
        // Clean up after each test if necessary
    }

    @Test
    fun trackScreenOnceShouldLogScreenViewOnFirstComposition() {
        val screenName = "TestScreen"
        val screenClass = "TestClass"

        composeTestRule.setContent {
            TrackScreenOnce(
                screenName = screenName,
                screenClass = screenClass,
            )
            Text("Test Content")
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()

        // Verify the screen view was logged exactly once
        assert(testAnalyticsManager.loggedEvents.size == 1) {
            "Expected 1 logged event, but got ${testAnalyticsManager.loggedEvents.size}"
        }
        assert(testAnalyticsManager.loggedEvents[0].screenName == screenName) {
            "Expected screen name '$screenName', but got '${testAnalyticsManager.loggedEvents[0].screenName}'"
        }
        assert(testAnalyticsManager.loggedEvents[0].screenClass == screenClass) {
            "Expected screen class '$screenClass', but got '${testAnalyticsManager.loggedEvents[0].screenClass}'"
        }
    }

    @Test
    fun trackScreenOnceShouldNotLogScreenViewOnRecomposition() {
        val screenName = "TestScreen"
        val screenClass = "TestClass"
        var triggerRecomposition by mutableStateOf(false)

        composeTestRule.setContent {
            TrackScreenOnce(
                screenName = screenName,
                screenClass = screenClass,
            )
            // This text change will trigger recomposition
            Text(if (triggerRecomposition) "Updated Content" else "Original Content")
        }

        composeTestRule.waitForIdle()

        // Verify initial tracking
        assert(testAnalyticsManager.loggedEvents.size == 1) {
            "Expected 1 logged event initially, but got ${testAnalyticsManager.loggedEvents.size}"
        }

        // Trigger recomposition multiple times
        composeTestRule.runOnIdle { triggerRecomposition = true }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { triggerRecomposition = false }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { triggerRecomposition = true }
        composeTestRule.waitForIdle()

        // Should still only have one logged event despite multiple recompositions
        assert(testAnalyticsManager.loggedEvents.size == 1) {
            "Expected 1 logged event after recomposition, but got ${testAnalyticsManager.loggedEvents.size}"
        }
        assert(testAnalyticsManager.loggedEvents[0].screenName == screenName)
        assert(testAnalyticsManager.loggedEvents[0].screenClass == screenClass)
    }

    @Test
    fun trackScreenOnceShouldPassCorrectScreenNameAndClass() {
        val screenName = "AnotherScreen"
        val screenClass = "AnotherClass"

        composeTestRule.setContent {
            TrackScreenOnce(
                screenName = screenName,
                screenClass = screenClass,
            )
            Text("Another Test Content")
        }

        composeTestRule.waitForIdle()

        assert(testAnalyticsManager.loggedEvents.size == 1)
        assert(testAnalyticsManager.loggedEvents[0].screenName == screenName)
        assert(testAnalyticsManager.loggedEvents[0].screenClass == screenClass)
    }

    @Test
    fun trackScreenOnceShouldPassParameters() {
        val screenName = "ParameterizedScreen"
        val screenClass = "ParameterizedClass"
        val parameters =
            mapOf(
                "param1" to "value1",
                "param2" to 42,
                "param3" to true,
            )

        composeTestRule.setContent {
            TrackScreenOnce(
                screenName = screenName,
                screenClass = screenClass,
                parameters = parameters,
            )
            Text("Parameterized Test Content")
        }

        composeTestRule.waitForIdle()

        assert(testAnalyticsManager.loggedEvents.size == 1)
        val loggedEvent = testAnalyticsManager.loggedEvents[0]
        assert(loggedEvent.screenName == screenName)
        assert(loggedEvent.screenClass == screenClass)
        assert(loggedEvent.parameters == parameters) {
            "Expected parameters $parameters, but got ${loggedEvent.parameters}"
        }
    }

    @Test
    fun trackScreenOnceShouldHandleEmptyParameters() {
        val screenName = "EmptyParamsScreen"
        val screenClass = "EmptyParamsClass"

        composeTestRule.setContent {
            TrackScreenOnce(
                screenName = screenName,
                screenClass = screenClass,
                parameters = emptyMap(),
            )
            Text("Empty Params Test Content")
        }

        composeTestRule.waitForIdle()

        assert(testAnalyticsManager.loggedEvents.size == 1)
        val loggedEvent = testAnalyticsManager.loggedEvents[0]
        assert(loggedEvent.parameters.isEmpty()) {
            "Expected empty parameters, but got ${loggedEvent.parameters}"
        }
    }
}
