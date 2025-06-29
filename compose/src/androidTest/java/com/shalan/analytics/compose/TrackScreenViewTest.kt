package com.shalan.analytics.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
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
class TrackScreenViewTest {
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
    fun trackScreenViewShouldLogScreenViewOnFirstComposition() {
        val screenName = "TestScreen"

        composeTestRule.setContent {
            TrackScreenView(screenName = screenName) {
                Text("Test Content")
            }
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()

        // Verify the screen view was logged
        assert(testAnalyticsManager.loggedEvents.size == 1) {
            "Expected 1 logged event, but got ${testAnalyticsManager.loggedEvents.size}"
        }
        assert(testAnalyticsManager.loggedEvents[0].screenName == screenName) {
            "Expected screen name '$screenName', but got '${testAnalyticsManager.loggedEvents[0].screenName}'"
        }
        assert(testAnalyticsManager.loggedEvents[0].screenClass == screenName) {
            "Expected screen class '$screenName', but got '${testAnalyticsManager.loggedEvents[0].screenClass}'"
        }
    }

    @Test
    fun trackScreenViewShouldNotLogScreenViewOnRecomposition() {
        val screenName = "TestScreen"
        var triggerRecomposition by androidx.compose.runtime.mutableStateOf(false)

        composeTestRule.setContent {
            TrackScreenView(screenName = screenName) {
                Text(if (triggerRecomposition) "Updated Content" else "Original Content")
            }
        }

        composeTestRule.waitForIdle()

        // Trigger recomposition
        composeTestRule.runOnIdle { triggerRecomposition = true }
        composeTestRule.waitForIdle()

        // Should still only have one logged event
        assert(testAnalyticsManager.loggedEvents.size == 1) {
            "Expected 1 logged event after recomposition, but got ${testAnalyticsManager.loggedEvents.size}"
        }
        assert(testAnalyticsManager.loggedEvents[0].screenName == screenName)
        assert(testAnalyticsManager.loggedEvents[0].screenClass == screenName)
    }

    @Test
    fun trackScreenViewShouldPassCorrectScreenNameAndClass() {
        val screenName = "AnotherScreen"
        val expectedScreenClass = "AnotherScreen"

        composeTestRule.setContent {
            TrackScreenView(screenName = screenName) {
                Text("Another Test Content")
            }
        }

        composeTestRule.waitForIdle()

        assert(testAnalyticsManager.loggedEvents.size == 1)
        assert(testAnalyticsManager.loggedEvents[0].screenName == screenName)
        assert(testAnalyticsManager.loggedEvents[0].screenClass == expectedScreenClass)
    }
}
