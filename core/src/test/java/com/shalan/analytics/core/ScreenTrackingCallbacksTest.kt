package com.shalan.analytics.core

import android.app.Activity
import android.os.Bundle
import com.shalan.analytics.annotation.TrackScreen
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScreenTrackingCallbacksTest {
    @MockK(relaxed = true)
    lateinit var mockAnalyticsManager: AnalyticsManager

    private lateinit var screenTrackingCallbacks: ScreenTrackingCallbacks

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        clearAllMocks()
        screenTrackingCallbacks = ScreenTrackingCallbacks(mockAnalyticsManager)
    }

    @After
    fun tearDown() {
        // No specific tear down needed for Robolectric or MockK beyond clearAllMocks
    }

    @Test
    fun `onActivityCreated should log screen view for annotated activity`() {
        // Given
        val activity = TestActivity()
        val bundle = Bundle()

        // When
        screenTrackingCallbacks.onActivityCreated(activity, bundle)

        // Then
        verify(exactly = 1) {
            mockAnalyticsManager.logScreenView(
                screenName = "Test Screen",
                screenClass = "TestActivity",
                parameters = mapOf("user_id" to "123", "user_name" to "Test User"),
            )
        }
    }

    @Test
    fun `onActivityCreated should not log screen view for unannotated activity`() {
        // Given
        val activity = UnannotatedActivity()
        val bundle = Bundle()

        // When
        screenTrackingCallbacks.onActivityCreated(activity, bundle)

        // Then
        verify(exactly = 0) { mockAnalyticsManager.logScreenView(any(), any(), any()) }
    }

    @Test
    fun `onActivityCreated should filter additional params based on annotation`() {
        // Given
        val activity = ActivityWithFilteredParams()
        val bundle = Bundle()

        // When
        screenTrackingCallbacks.onActivityCreated(activity, bundle)

        // Then
        verify(exactly = 1) {
            mockAnalyticsManager.logScreenView(
                screenName = "Filtered Screen",
                screenClass = "ActivityWithFilteredParams",
                parameters = mapOf("param1" to "value1"),
            )
        }
    }

    @Test
    fun `onActivityCreated should use custom screenClass if provided`() {
        // Given
        val activity = ActivityWithCustomScreenClass()
        val bundle = Bundle()

        // When
        screenTrackingCallbacks.onActivityCreated(activity, bundle)

        // Then
        verify(exactly = 1) {
            mockAnalyticsManager.logScreenView(
                screenName = "Custom Class Screen",
                screenClass = "MyCustomScreen",
                parameters = emptyMap(),
            )
        }
    }
}

@TrackScreen(screenName = "Test Screen")
class TestActivity : Activity(), TrackedScreenParamsProvider {
    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf("user_id" to "123", "user_name" to "Test User", "extra_param" to "should_be_filtered")
    }
}

class UnannotatedActivity : Activity()

@TrackScreen(screenName = "Filtered Screen")
class ActivityWithFilteredParams : Activity(), TrackedScreenParamsProvider {
    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf("param1" to "value1", "param2" to "value2")
    }
}

@TrackScreen(screenName = "Custom Class Screen", screenClass = "MyCustomScreen")
class ActivityWithCustomScreenClass : Activity()
