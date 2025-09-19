package com.shalan.analyticsannotation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shalan.analytics.annotation.Param
import com.shalan.analytics.annotation.Track
import com.shalan.analytics.annotation.Trackable

/**
 * Example ViewModel demonstrating method tracking with @Track annotation.
 * Shows various use cases including custom data class serialization.
 *
 * @Trackable annotation marks this class as containing methods with @Track annotations,
 * enabling efficient processing by the analytics plugin.
 */
@Trackable
class UserProfileViewModel : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _preferences = MutableLiveData<UserPreferences>()
    val preferences: LiveData<UserPreferences> = _preferences

    /**
     * Example of tracking a method with primitive parameters.
     * This demonstrates basic parameter tracking with global parameters included.
     */
    @Track(eventName = "user_profile_loaded", includeGlobalParams = true)
    fun loadUserProfile(
        @Param("user_id") userId: String,
        @Param("source") source: String,
    ) {
        // Simulate loading user profile
        val profile =
            UserProfile(
                id = userId,
                name = "John Doe",
                email = "john.doe@example.com",
                age = 30,
                isVerified = true,
            )
        _userProfile.value = profile
    }

    /**
     * Example of tracking a method with complex object parameters.
     * The UserProfile data class will be serialized using JsonParameterSerializer.
     */
    @Track(eventName = "user_profile_updated", includeGlobalParams = true)
    fun updateProfile(
        @Param("profile") newProfile: UserProfile,
    ) {
        _userProfile.value = newProfile
    }

    /**
     * Example of tracking with mixed parameter types including collections.
     * This shows how different types are handled by different serializers.
     */
    @Track(eventName = "user_preferences_changed", includeGlobalParams = true)
    fun updatePreferences(
        @Param("user_id") userId: String,
        @Param("preferences") preferences: UserPreferences,
        @Param("changed_fields") changedFields: List<String>,
    ) {
        _preferences.value = preferences
    }

    /**
     * Example of tracking without global parameters.
     * This demonstrates selective parameter inclusion.
     */
    @Track(eventName = "profile_view_analytics", includeGlobalParams = false)
    fun trackProfileView(
        @Param("view_duration_ms") duration: Long,
        @Param("sections_viewed") sections: Array<String>,
        @Param("interaction_count") interactions: Int,
    ) {
        // Analytics-only method - no business logic
    }

    /**
     * Example of tracking a method that handles errors.
     * Shows how analytics can track both success and failure scenarios.
     */
    @Track(eventName = "profile_operation_completed", includeGlobalParams = true)
    fun performComplexOperation(
        @Param("operation_type") operationType: String,
        @Param("parameters") operationParams: Map<String, Any>,
        @Param("result") result: OperationResult,
    ) {
        // Complex operation logic here
    }
}

/**
 * Data class representing a user profile.
 * This will be serialized to JSON by JsonParameterSerializer.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val isVerified: Boolean,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Data class representing user preferences.
 * Another example of automatic JSON serialization.
 */
data class UserPreferences(
    val theme: Theme,
    val notifications: NotificationSettings,
    val privacy: PrivacySettings,
    val language: String = "en",
)

data class NotificationSettings(
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val smsEnabled: Boolean,
    val frequency: NotificationFrequency,
)

data class PrivacySettings(
    val profileVisibility: ProfileVisibility,
    val dataSharing: Boolean,
    val analyticsOptIn: Boolean,
)

/**
 * Operation result that demonstrates custom object serialization.
 */
data class OperationResult(
    val success: Boolean,
    val message: String,
    val executionTimeMs: Long,
    val affectedRecords: Int = 0,
    val warnings: List<String> = emptyList(),
)

enum class Theme {
    LIGHT,
    DARK,
    AUTO,
}

enum class NotificationFrequency {
    IMMEDIATE,
    HOURLY,
    DAILY,
    WEEKLY,
    NEVER,
}

enum class ProfileVisibility {
    PUBLIC,
    FRIENDS,
    PRIVATE,
}
