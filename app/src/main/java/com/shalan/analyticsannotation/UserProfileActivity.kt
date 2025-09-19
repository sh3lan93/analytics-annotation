package com.shalan.analyticsannotation

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shalan.analytics.annotation.TrackScreen
import kotlin.random.Random

/**
 * Activity demonstrating ViewModel method tracking with custom serializers.
 * This shows how @Track annotations work in ViewModels and how custom serializers
 * can be used to control analytics data collection.
 */
@TrackScreen(screenName = "User Profile Screen", screenClass = "UserProfile")
class UserProfileActivity : AppCompatActivity() {
    private val viewModel: UserProfileViewModel by viewModels()

    private lateinit var userIdEditText: EditText
    private lateinit var loadProfileButton: Button
    private lateinit var updateProfileButton: Button
    private lateinit var updatePreferencesButton: Button
    private lateinit var trackViewButton: Button
    private lateinit var complexOperationButton: Button
    private lateinit var profileDisplayText: TextView
    private lateinit var preferencesDisplayText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        initializeViews()
        setupObservers()
        setupClickListeners()

        showToast("UserProfileActivity created - demonstrates ViewModel method tracking")
    }

    private fun initializeViews() {
        userIdEditText = findViewById(R.id.userIdEditText)
        loadProfileButton = findViewById(R.id.loadProfileButton)
        updateProfileButton = findViewById(R.id.updateProfileButton)
        updatePreferencesButton = findViewById(R.id.updatePreferencesButton)
        trackViewButton = findViewById(R.id.trackViewButton)
        complexOperationButton = findViewById(R.id.complexOperationButton)
        profileDisplayText = findViewById(R.id.profileDisplayText)
        preferencesDisplayText = findViewById(R.id.preferencesDisplayText)

        // Set default user ID
        userIdEditText.setText("user_${Random.nextInt(1000, 9999)}")
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.userProfile.observe(
            this,
            Observer { profile ->
                profileDisplayText.text = "Profile: ${profile.name} (${profile.email})\n" +
                    "Age: ${profile.age}, Verified: ${profile.isVerified}"
            },
        )

        viewModel.preferences.observe(
            this,
            Observer { preferences ->
                preferencesDisplayText.text = "Theme: ${preferences.theme}\n" +
                    "Notifications: Email=${preferences.notifications.emailEnabled}, " +
                    "Push=${preferences.notifications.pushEnabled}\n" +
                    "Language: ${preferences.language}"
            },
        )
    }

    private fun setupClickListeners() {
        loadProfileButton.setOnClickListener {
            val userId = userIdEditText.text.toString().ifEmpty { "anonymous" }
            // This method call will be tracked by @Track annotation
            viewModel.loadUserProfile(userId, "manual_load")
            showToast("Loading profile for user: $userId")
        }

        updateProfileButton.setOnClickListener {
            val currentProfile = viewModel.userProfile.value
            if (currentProfile != null) {
                val updatedProfile =
                    currentProfile.copy(
                        name = "${currentProfile.name} Updated",
                        age = currentProfile.age + 1,
                        metadata =
                            mapOf(
                                "last_update" to System.currentTimeMillis().toString(),
                                "update_source" to "user_interaction",
                                // This will be redacted by PrivacyAwareSerializer
                                "password" to "secret123",
                                // This will also be redacted
                                "credit_card" to "1234-5678-9012-3456",
                            ),
                    )
                // This method call will be tracked with custom UserProfile serialization
                viewModel.updateProfile(updatedProfile)
                showToast("Profile updated with custom serialization")
            } else {
                showToast("Load a profile first!")
            }
        }

        updatePreferencesButton.setOnClickListener {
            val preferences =
                UserPreferences(
                    theme = Theme.values().random(),
                    notifications =
                        NotificationSettings(
                            emailEnabled = Random.nextBoolean(),
                            pushEnabled = Random.nextBoolean(),
                            smsEnabled = Random.nextBoolean(),
                            frequency = NotificationFrequency.values().random(),
                        ),
                    privacy =
                        PrivacySettings(
                            profileVisibility = ProfileVisibility.values().random(),
                            dataSharing = Random.nextBoolean(),
                            analyticsOptIn = Random.nextBoolean(),
                        ),
                    language = listOf("en", "es", "fr", "de").random(),
                )

            val changedFields = listOf("theme", "notifications", "privacy", "language")
            val userId = userIdEditText.text.toString().ifEmpty { "anonymous" }

            // This demonstrates tracking with multiple parameter types
            viewModel.updatePreferences(userId, preferences, changedFields)
            showToast("Preferences updated with mixed parameter types")
        }

        trackViewButton.setOnClickListener {
            val viewDuration = Random.nextLong(1000, 30000)
            val sectionsViewed =
                arrayOf(
                    "profile_info",
                    "preferences",
                    "security",
                    "billing",
                    "notifications",
                    "privacy",
                    "data_export",
                    "help",
                )
            val interactionCount = Random.nextInt(1, 20)

            // This tracks without global parameters and demonstrates array handling
            viewModel.trackProfileView(viewDuration, sectionsViewed, interactionCount)
            showToast("Profile view tracked (no global params)")
        }

        complexOperationButton.setOnClickListener {
            val operationType = listOf("data_export", "account_merge", "privacy_update").random()
            val operationParams =
                mapOf(
                    "target_format" to "json",
                    "include_history" to true,
                    "compression" to "gzip",
                    // Will be redacted
                    "token" to "secret-token-123",
                    // Will be redacted
                    "api_key" to "sk-1234567890abcdef",
                )

            val result =
                OperationResult(
                    success = Random.nextBoolean(),
                    message =
                        if (Random.nextBoolean()) {
                            "Operation completed successfully"
                        } else {
                            "Operation completed with warnings"
                        },
                    executionTimeMs = Random.nextLong(500, 5000),
                    affectedRecords = Random.nextInt(0, 1000),
                    warnings =
                        if (Random.nextBoolean()) {
                            listOf("Data size exceeded recommended limit", "Some legacy fields were skipped")
                        } else {
                            emptyList()
                        },
                )

            // This demonstrates complex object serialization
            viewModel.performComplexOperation(operationType, operationParams, result)
            showToast("Complex operation tracked with custom serialization")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
