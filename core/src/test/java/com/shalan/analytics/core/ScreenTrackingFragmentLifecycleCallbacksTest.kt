package com.shalan.analytics.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.shalan.analytics.annotation.TrackScreen
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScreenTrackingFragmentLifecycleCallbacksTest {
    @MockK(relaxed = true)
    lateinit var mockAnalyticsManager: AnalyticsManager

    @RelaxedMockK
    lateinit var mockFragmentManager: FragmentManager

    @RelaxedMockK
    lateinit var mockView: View

    private lateinit var screenTrackingCallbacks: ScreenTrackingFragmentLifecycleCallbacks

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        screenTrackingCallbacks = ScreenTrackingFragmentLifecycleCallbacks(mockAnalyticsManager)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `onFragmentViewCreated should log screen view for annotated fragment`() {
        // Given
        val fragment = TestFragment()
        val bundle: Bundle? = null

        // When
        screenTrackingCallbacks.onFragmentViewCreated(mockFragmentManager, fragment, mockView, bundle)

        // Then
        verify(exactly = 1) {
            mockAnalyticsManager.logScreenView(
                screenName = "Test Fragment",
                screenClass = "TestFragment",
                parameters = mapOf("param1" to "value1"),
            )
        }
    }

    @Test
    fun `onFragmentViewCreated should not log screen view for unannotated fragment`() {
        // Given
        val fragment = UnannotatedFragment()
        val bundle: Bundle? = null

        // When
        screenTrackingCallbacks.onFragmentViewCreated(mockFragmentManager, fragment, mockView, bundle)

        // Then
        verify(exactly = 0) {
            mockAnalyticsManager.logScreenView(any(), any(), any())
        }
    }

    @Test
    fun `onFragmentViewCreated should use custom screenClass if provided`() {
        // Given
        val fragment = FragmentWithCustomScreenClass()
        val bundle: Bundle? = null

        // When
        screenTrackingCallbacks.onFragmentViewCreated(mockFragmentManager, fragment, mockView, bundle)

        // Then
        verify(exactly = 1) {
            mockAnalyticsManager.logScreenView(
                screenName = "Custom Class Fragment",
                screenClass = "MyCustomFragment",
                parameters = emptyMap(),
            )
        }
    }

    @Test
    fun `onFragmentViewCreated should filter additional params based on annotation`() {
        // Given
        val fragment = FragmentWithFilteredParams()
        val bundle: Bundle? = null

        // When
        screenTrackingCallbacks.onFragmentViewCreated(mockFragmentManager, fragment, mockView, bundle)

        // Then
        verify(exactly = 1) {
            mockAnalyticsManager.logScreenView(
                screenName = "Filtered Fragment",
                screenClass = "FragmentWithFilteredParams",
                parameters = mapOf("param1" to "value1"),
            )
        }
    }
}

// Test Fragment Implementations

@TrackScreen(screenName = "Test Fragment", additionalParams = ["param1"])
class TestFragment : Fragment(), TrackedScreenParamsProvider {
    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf("param1" to "value1", "param2" to "value2")
    }
}

class UnannotatedFragment : Fragment()

@TrackScreen(screenName = "Custom Class Fragment", screenClass = "MyCustomFragment")
class FragmentWithCustomScreenClass : Fragment()

@TrackScreen(screenName = "Filtered Fragment", additionalParams = ["param1"])
class FragmentWithFilteredParams : Fragment(), TrackedScreenParamsProvider {
    override fun getTrackedScreenParams(): Map<String, Any> {
        return mapOf("param1" to "value1", "should_be_filtered" to "valueX")
    }
}
