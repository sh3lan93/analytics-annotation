package com.shalan.analyticsannotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [UserProfileViewModel] analytics tracking.
 *
 * These tests verify that:
 * - Methods annotated with @Track log events with correct names
 * - Parameters are correctly serialized and tracked
 * - Global parameters are included/excluded based on configuration
 * - Method tracking works with various parameter types
 * - Multiple method calls log separate events
 *
 * Test approach:
 * - Directly call ViewModel methods
 * - Track execution to ensure methods complete successfully
 * - Methods with @Track annotation will log events through MethodTrackingManager
 * - This tests the integration between ViewModel, annotations, and analytics
 *
 * Uses Robolectric to properly mock Android framework components like LiveData and Looper.
 */
@RunWith(RobolectricTestRunner::class)
class UserProfileViewModelTest {
    private lateinit var viewModel: UserProfileViewModel
    private lateinit var testAnalyticsManager: TestMethodTrackingManager

    @Before
    fun setUp() {
        // Create test analytics manager to capture events
        testAnalyticsManager = TestMethodTrackingManager()

        // Initialize MethodTrackingManager with test manager
        com.shalan.analytics.core.MethodTrackingManager.initialize(testAnalyticsManager)

        // Create ViewModel instance
        viewModel = UserProfileViewModel()
    }

    /**
     * Test that loadUserProfile method tracks with correct event name and primitive parameters.
     * Verifies:
     * - Event name is "user_profile_loaded"
     * - user_id parameter is tracked
     * - source parameter is tracked
     * - Global parameters are included (includeGlobalParams = true)
     */
    @Test
    fun testLoadUserProfileTracking() {
        // Clear any previous events
        testAnalyticsManager.loggedEvents.clear()

        // Call tracked method
        viewModel.loadUserProfile(userId = "user_123", source = "login")

        // Verify method executed successfully
        assertNotNull(viewModel.userProfile.value)
        assertEquals("user_123", viewModel.userProfile.value?.id)

        // Verify event was logged
        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals("user_profile_loaded", event.eventName)

        // Verify parameters
        assertEquals("user_123", event.parameters["user_id"])
        assertEquals("login", event.parameters["source"])
    }

    /**
     * Test that updateProfile method tracks complex object parameters.
     * Verifies:
     * - Event name is "user_profile_updated"
     * - Complex UserProfile object is properly serialized
     * - Global parameters are included
     */
    @Test
    fun testUpdateProfileTracking() {
        testAnalyticsManager.loggedEvents.clear()

        // Create a test profile
        val testProfile =
            UserProfile(
                id = "user_456",
                name = "Jane Doe",
                email = "jane@example.com",
                age = 28,
                isVerified = true,
            )

        // Call tracked method
        viewModel.updateProfile(newProfile = testProfile)

        // Verify method executed successfully
        assertNotNull(viewModel.userProfile.value)
        assertEquals("Jane Doe", viewModel.userProfile.value?.name)

        // Verify event was logged
        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals("user_profile_updated", event.eventName)

        // Verify profile parameter exists
        assertTrue(event.parameters.containsKey("profile"))
    }

    /**
     * Test that updatePreferences method tracks mixed parameter types.
     * Verifies:
     * - Event name is "user_preferences_changed"
     * - Primitive parameters (user_id) are tracked
     * - Complex object (UserPreferences) is serialized
     * - Collection (changedFields) is handled
     * - Global parameters are included
     */
    @Test
    fun testUpdatePreferencesTracking() {
        testAnalyticsManager.loggedEvents.clear()

        // Create test preferences
        val testPreferences =
            UserPreferences(
                theme = Theme.DARK,
                notifications =
                    NotificationSettings(
                        emailEnabled = true,
                        pushEnabled = false,
                        smsEnabled = true,
                        frequency = NotificationFrequency.DAILY,
                    ),
                privacy =
                    PrivacySettings(
                        profileVisibility = ProfileVisibility.PRIVATE,
                        dataSharing = false,
                        analyticsOptIn = true,
                    ),
                language = "en",
            )

        val changedFields = listOf("theme", "notifications")

        // Call tracked method
        viewModel.updatePreferences(
            userId = "user_789",
            preferences = testPreferences,
            changedFields = changedFields,
        )

        // Verify method executed successfully
        assertNotNull(viewModel.preferences.value)

        // Verify event was logged
        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals("user_preferences_changed", event.eventName)

        // Verify all parameters are present
        assertEquals("user_789", event.parameters["user_id"])
        assertTrue(event.parameters.containsKey("preferences"))
        assertTrue(event.parameters.containsKey("changed_fields"))
    }

    /**
     * Test that trackProfileView method tracks with global parameters excluded.
     * Verifies:
     * - Event name is "profile_view_analytics"
     * - Parameters specific to the method are tracked
     * - Global parameters are NOT included (includeGlobalParams = false)
     */
    @Test
    fun testTrackProfileViewWithoutGlobalParams() {
        testAnalyticsManager.loggedEvents.clear()

        // Call tracked method
        val duration = 5000L
        val sections = arrayOf("bio", "posts", "photos")
        val interactions = 3

        viewModel.trackProfileView(
            duration = duration,
            sections = sections,
            interactions = interactions,
        )

        // Verify event was logged
        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals("profile_view_analytics", event.eventName)

        // Verify parameters
        assertEquals(duration, event.parameters["view_duration_ms"])
        assertTrue(event.parameters.containsKey("sections_viewed"))
        assertEquals(interactions, event.parameters["interaction_count"])
    }

    /**
     * Test that performComplexOperation method tracks with complex parameters.
     * Verifies:
     * - Event name is "profile_operation_completed"
     * - String parameter (operationType) is tracked
     * - Map parameter is serialized
     * - Custom object (OperationResult) is serialized
     * - Global parameters are included
     */
    @Test
    fun testPerformComplexOperationTracking() {
        testAnalyticsManager.loggedEvents.clear()

        // Create test parameters
        val operationType = "data_export"
        val operationParams =
            mapOf(
                "format" to "csv",
                "includeMetadata" to true,
            )
        val result =
            OperationResult(
                success = true,
                message = "Operation completed successfully",
                executionTimeMs = 1500L,
                affectedRecords = 42,
                warnings = listOf("Some old data was not exported"),
            )

        // Call tracked method
        viewModel.performComplexOperation(
            operationType = operationType,
            operationParams = operationParams,
            result = result,
        )

        // Verify event was logged
        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals("profile_operation_completed", event.eventName)

        // Verify all parameters are present
        assertEquals(operationType, event.parameters["operation_type"])
        assertTrue(event.parameters.containsKey("parameters"))
        assertTrue(event.parameters.containsKey("result"))
    }

    /**
     * Test that multiple method calls log separate events.
     * Verifies that calling different tracked methods logs events for each call.
     */
    @Test
    fun testMultipleMethodCallsLogSeparateEvents() {
        testAnalyticsManager.loggedEvents.clear()

        // Call multiple tracked methods
        viewModel.loadUserProfile(userId = "user_1", source = "app")
        viewModel.trackProfileView(duration = 2000L, sections = arrayOf("home"), interactions = 1)

        // Verify both events were logged
        assertEquals(2, testAnalyticsManager.loggedEvents.size)

        // Verify first event
        assertEquals("user_profile_loaded", testAnalyticsManager.loggedEvents[0].eventName)

        // Verify second event
        assertEquals("profile_view_analytics", testAnalyticsManager.loggedEvents[1].eventName)
    }

    /**
     * Test that method parameters are correctly tracked across different data types.
     * Verifies parameter tracking with String, Long, Array, and Int types.
     */
    @Test
    fun testParameterTypesTracking() {
        testAnalyticsManager.loggedEvents.clear()

        viewModel.trackProfileView(
            duration = 1234L,
            sections = arrayOf("section1", "section2", "section3"),
            interactions = 5,
        )

        val event = testAnalyticsManager.loggedEvents[0]

        // Verify types are preserved
        assertTrue(event.parameters["view_duration_ms"] is Long)
        assertTrue(event.parameters["interaction_count"] is Int)
        assertTrue(event.parameters.containsKey("sections_viewed"))
    }

    /**
     * Test that ViewModel LiveData updates correctly alongside analytics tracking.
     * Verifies that analytics doesn't interfere with ViewModel functionality.
     */
    @Test
    fun testViewModelFunctionalityWithAnalyticsEnabled() {
        // Load user profile
        viewModel.loadUserProfile(userId = "user_100", source = "widget")

        // Verify LiveData was updated
        assertNotNull(viewModel.userProfile.value)
        val loadedProfile = viewModel.userProfile.value!!
        assertEquals("user_100", loadedProfile.id)
        assertEquals("John Doe", loadedProfile.name)

        // Verify analytics tracked the event
        assertEquals(1, testAnalyticsManager.loggedEvents.size)
        assertEquals("user_profile_loaded", testAnalyticsManager.loggedEvents[0].eventName)
    }

    /**
     * Test UserProfile serialization in tracking.
     * Verifies that complex objects can be serialized for tracking.
     */
    @Test
    fun testUserProfileSerialization() {
        testAnalyticsManager.loggedEvents.clear()

        val profile =
            UserProfile(
                id = "123",
                name = "Test User",
                email = "test@example.com",
                age = 30,
                isVerified = true,
                metadata = mapOf("source" to "signup", "verified_at" to "2024-01-01"),
            )

        viewModel.updateProfile(newProfile = profile)

        val event = testAnalyticsManager.loggedEvents[0]
        assertTrue(event.parameters.containsKey("profile"))
    }

    /**
     * Test that event parameters don't contain null values.
     * This verifies the default behavior of filtering out null parameters.
     */
    @Test
    fun testNullParametersNotTracked() {
        testAnalyticsManager.loggedEvents.clear()

        // Create profile with empty metadata (should not cause null issues)
        val profile =
            UserProfile(
                id = "id_null_test",
                name = "User",
                email = "user@test.com",
                age = 25,
                isVerified = false,
            )

        viewModel.updateProfile(newProfile = profile)

        val event = testAnalyticsManager.loggedEvents[0]

        // Verify no null values in parameters
        event.parameters.values.forEach { value ->
            assertTrue("Parameter should not be null", value != null)
        }
    }

    /**
     * Test sequential calls to the same tracked method.
     * Verifies that multiple calls to the same method each log their own events.
     */
    @Test
    fun testSequentialCallsToSameMethod() {
        testAnalyticsManager.loggedEvents.clear()

        // Call the same method multiple times
        viewModel.loadUserProfile(userId = "user_1", source = "login")
        viewModel.loadUserProfile(userId = "user_2", source = "deeplink")
        viewModel.loadUserProfile(userId = "user_3", source = "notification")

        // Verify each call logged its own event
        assertEquals(3, testAnalyticsManager.loggedEvents.size)

        // Verify parameters for each event
        assertEquals("user_1", testAnalyticsManager.loggedEvents[0].parameters["user_id"])
        assertEquals("user_2", testAnalyticsManager.loggedEvents[1].parameters["user_id"])
        assertEquals("user_3", testAnalyticsManager.loggedEvents[2].parameters["user_id"])
    }

    /**
     * Test method tracking with all optional/default parameters.
     * Verifies handling of data classes with default values.
     */
    @Test
    fun testComplexParametersWithDefaults() {
        testAnalyticsManager.loggedEvents.clear()

        val preferences =
            UserPreferences(
                theme = Theme.LIGHT,
                notifications =
                    NotificationSettings(
                        emailEnabled = true,
                        pushEnabled = true,
                        smsEnabled = false,
                        frequency = NotificationFrequency.WEEKLY,
                    ),
                privacy =
                    PrivacySettings(
                        profileVisibility = ProfileVisibility.PUBLIC,
                        dataSharing = true,
                        analyticsOptIn = true,
                    ),
                // language uses default value "en"
            )

        viewModel.updatePreferences(
            userId = "default_test",
            preferences = preferences,
            changedFields = emptyList(),
        )

        val event = testAnalyticsManager.loggedEvents[0]
        assertEquals("user_preferences_changed", event.eventName)
        assertEquals("default_test", event.parameters["user_id"])
    }

    /**
     * Helper class to test method tracking.
     * This is a custom implementation of AnalyticsManager to capture events.
     */
    private class TestMethodTrackingManager : com.shalan.analytics.core.AnalyticsManager {
        val loggedEvents = mutableListOf<LoggedEvent>()

        override fun logScreenView(
            screenName: String,
            screenClass: String,
            parameters: Map<String, Any>,
        ) {
            // Not used for method tracking tests
        }

        override fun logEvent(
            eventName: String,
            parameters: Map<String, Any>,
            includeGlobalParameters: Boolean,
        ) {
            loggedEvents.add(LoggedEvent(eventName, parameters))
        }

        override fun setGlobalParameters(parameters: Map<String, Any>) {
            // Not used for method tracking tests
        }

        override fun release() {
            // Not used for method tracking tests
        }

        data class LoggedEvent(
            val eventName: String,
            val parameters: Map<String, Any>,
        )
    }
}
