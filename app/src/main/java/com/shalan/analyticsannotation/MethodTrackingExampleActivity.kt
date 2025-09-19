package com.shalan.analyticsannotation

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.Param
import com.shalan.analytics.annotation.Track
import com.shalan.analytics.annotation.TrackScreen
import kotlinx.serialization.Serializable

/**
 * Activity demonstrating comprehensive method tracking with @Track and @Param annotations.
 * This shows various use cases for the method-level analytics tracking feature.
 */
@TrackScreen(screenName = "Method Tracking Demo")
class MethodTrackingExampleActivity : AppCompatActivity() {
    private lateinit var userNameInput: EditText
    private lateinit var searchInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_method_tracking_example)

        initializeViews()
        setupButtonListeners()

        // Track the screen initialization
        onScreenInitialized(System.currentTimeMillis())
    }

    private fun initializeViews() {
        userNameInput = findViewById(R.id.user_name_input)
        searchInput = findViewById(R.id.search_input)
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.login_button).setOnClickListener {
            val userName = userNameInput.text.toString()
            if (userName.isNotEmpty()) {
                performLogin(userName, "password_auth", isFirstLogin = true)
            }
        }

        findViewById<Button>(R.id.search_button).setOnClickListener {
            val query = searchInput.text.toString()
            if (query.isNotEmpty()) {
                performSearch(query, "user_initiated", maxResults = 20)
            }
        }

        findViewById<Button>(R.id.purchase_button).setOnClickListener {
            processPurchase("premium_subscription", 9.99, "USD", itemCount = 1)
        }

        findViewById<Button>(R.id.error_button).setOnClickListener {
            simulateErrorScenario()
        }

        findViewById<Button>(R.id.complex_operation_button).setOnClickListener {
            performComplexOperation(
                operationType = "data_processing",
                priority = "high",
                retryCount = 3,
                enableCaching = true,
                metadata = mapOf("source" to "user_action", "feature" to "demo"),
            )
        }

        findViewById<Button>(R.id.serializable_demo_button).setOnClickListener {
            demonstrateSerializableDataClasses()
        }
    }

    /**
     * Example: Basic method tracking without parameters
     */
    @Track(eventName = "screen_initialized")
    private fun onScreenInitialized(timestamp: Long) {
        println("Screen initialized at: $timestamp")
    }

    /**
     * Example: Method tracking with multiple parameter types
     */
    @Track(eventName = "user_login_attempt", includeGlobalParams = true)
    private fun performLogin(
        @Param("user_name") userName: String,
        @Param("auth_method") authMethod: String,
        @Param("is_first_login") isFirstLogin: Boolean,
    ) {
        // Simulate login processing
        Thread.sleep(150) // Show execution timing

        val success = userName.length > 3
        if (success) {
            onLoginSuccess(userName)
            Toast.makeText(this, "Login successful for $userName", Toast.LENGTH_SHORT).show()
        } else {
            onLoginFailure("invalid_username", userName.length)
            Toast.makeText(this, "Login failed - username too short", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Example: Tracking success scenarios
     */
    @Track(eventName = "login_success")
    private fun onLoginSuccess(
        @Param("user_name") userName: String,
    ) {
        // Track successful login
        updateUserSession(userName)
    }

    /**
     * Example: Tracking failure scenarios with error details
     */
    @Track(eventName = "login_failure", includeGlobalParams = false)
    private fun onLoginFailure(
        @Param("error_reason") errorReason: String,
        @Param("username_length") usernameLength: Int,
    ) {
        // Track login failure with context
        println("Login failed: $errorReason (username length: $usernameLength)")
    }

    /**
     * Example: Method with various primitive parameter types
     */
    @Track(eventName = "search_performed")
    private fun performSearch(
        @Param("query") searchQuery: String,
        @Param("search_type") searchType: String,
        @Param("max_results") maxResults: Int,
    ) {
        // Simulate search processing
        Thread.sleep(200)

        val resultCount = searchQuery.length * 3 // Mock result calculation
        displaySearchResults(searchQuery, resultCount, System.currentTimeMillis())

        Toast.makeText(this, "Found $resultCount results for '$searchQuery'", Toast.LENGTH_SHORT).show()
    }

    /**
     * Example: Method with numeric parameters and timestamp
     */
    @Track(eventName = "search_results_displayed")
    private fun displaySearchResults(
        @Param("original_query") query: String,
        @Param("result_count") count: Int,
        @Param("completion_time") completionTime: Long,
    ) {
        // Display results logic
        println("Search completed: query='$query', results=$count, time=$completionTime")
    }

    /**
     * Example: E-commerce tracking with price and currency
     */
    @Track(eventName = "purchase_initiated", includeGlobalParams = true)
    private fun processPurchase(
        @Param("item_name") itemName: String,
        @Param("price") price: Double,
        @Param("currency") currency: String,
        @Param("quantity") itemCount: Int,
    ) {
        // Simulate purchase processing
        Thread.sleep(300)

        val success = price > 0
        if (success) {
            onPurchaseComplete(itemName, price, currency)
            Toast.makeText(this, "Purchase successful: $itemName", Toast.LENGTH_SHORT).show()
        } else {
            onPurchaseError("invalid_price", itemName)
        }
    }

    @Track(eventName = "purchase_completed")
    private fun onPurchaseComplete(
        @Param("item") item: String,
        @Param("amount") amount: Double,
        @Param("currency") currency: String,
    ) {
        println("Purchase completed: $item for $amount $currency")
    }

    @Track(eventName = "purchase_error")
    private fun onPurchaseError(
        @Param("error_type") errorType: String,
        @Param("item_name") itemName: String,
    ) {
        println("Purchase error: $errorType for $itemName")
    }

    /**
     * Example: Method that demonstrates error handling in tracking
     */
    @Track(eventName = "error_scenario_triggered")
    private fun simulateErrorScenario() {
        try {
            // Simulate some operation that might fail
            val result = 10 / 0 // This will throw ArithmeticException
            handleSuccessfulOperation(result)
        } catch (e: Exception) {
            handleOperationError(e.javaClass.simpleName, e.message ?: "Unknown error")
        }
    }

    @Track(eventName = "operation_success")
    private fun handleSuccessfulOperation(
        @Param("result") result: Int,
    ) {
        Toast.makeText(this, "Operation successful: $result", Toast.LENGTH_SHORT).show()
    }

    @Track(eventName = "operation_error")
    private fun handleOperationError(
        @Param("exception_type") exceptionType: String,
        @Param("error_message") errorMessage: String,
    ) {
        Toast.makeText(this, "Error: $exceptionType - $errorMessage", Toast.LENGTH_LONG).show()
    }

    /**
     * Example: Complex method with many parameters to test limits
     */
    @Track(eventName = "complex_operation_performed")
    private fun performComplexOperation(
        @Param("operation_type") operationType: String,
        @Param("priority") priority: String,
        @Param("retry_count") retryCount: Int,
        @Param("enable_caching") enableCaching: Boolean,
        @Param("metadata") metadata: Map<String, String>,
    ) {
        // Simulate complex processing
        Thread.sleep(500) // Longer operation to show execution timing

        val result =
            ComplexOperationResult(
                operationType = operationType,
                success = true,
                processingTime = 500L,
                cacheUsed = enableCaching,
            )

        onComplexOperationComplete(result)
        Toast.makeText(this, "Complex operation completed: $operationType", Toast.LENGTH_SHORT).show()
    }

    @Track(eventName = "complex_operation_completed")
    private fun onComplexOperationComplete(
        @Param("result") result: ComplexOperationResult,
    ) {
        println("Complex operation result: $result")
    }

    /**
     * Example: Private method tracking (to test access modifier handling)
     */
    @Track(eventName = "session_updated")
    private fun updateUserSession(
        @Param("user_id") userId: String,
    ) {
        // Update user session logic
        println("User session updated for: $userId")
    }

    /**
     * Example: Demonstrating @Serializable data classes with kotlinx.serialization
     * This shows how JsonParameterSerializer now properly uses kotlinx.serialization
     * for data classes annotated with @Serializable
     */
    @Track(eventName = "serializable_demo_executed", includeGlobalParams = true)
    private fun demonstrateSerializableDataClasses() {
        // Create sample user profile with @Serializable data class
        val userProfile =
            UserProfile(
                userId = "user_123",
                displayName = "John Doe",
                email = "john.doe@example.com",
                isVerified = true,
                membershipLevel = "premium",
                // 2022-01-01 timestamp
                joinedDate = 1640995200L,
            )

        // Create analytics event data with nested @Serializable objects
        val eventData =
            AnalyticsEventData(
                eventType = "profile_updated",
                timestamp = System.currentTimeMillis(),
                userProfile = userProfile,
                metadata =
                    EventMetadata(
                        source = "settings_screen",
                        version = "2.1.0",
                        experiments = listOf("experiment_A", "experiment_B"),
                        customProperties =
                            mapOf(
                                "feature_flag_enabled" to "true",
                                "ab_test_group" to "control",
                            ),
                    ),
            )

        // Track the event - JsonParameterSerializer will use kotlinx.serialization
        // for proper JSON serialization of @Serializable classes
        trackSerializableObject(userProfile, eventData)

        Toast.makeText(
            this,
            "Serializable data classes demo completed - check logs for JSON output!",
            Toast.LENGTH_LONG,
        ).show()
    }

    /**
     * Helper method that receives @Serializable objects as parameters
     * This demonstrates how the JsonParameterSerializer handles them
     */
    @Track(eventName = "serializable_objects_tracked")
    private fun trackSerializableObject(
        @Param("user_profile") profile: UserProfile,
        @Param("event_data") eventData: AnalyticsEventData,
    ) {
        Toast.makeText(this, "Tracking user profile: ${profile.userId} (${profile.displayName})", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Tracking event data: ${eventData.eventType}", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "JsonParameterSerializer uses kotlinx.serialization for @Serializable classes!", Toast.LENGTH_LONG).show()
    }

    /**
     * Data class for demonstrating object parameter serialization (non-@Serializable)
     * This will be serialized using the fallback toString() approach
     */
    data class ComplexOperationResult(
        val operationType: String,
        val success: Boolean,
        val processingTime: Long,
        val cacheUsed: Boolean,
    )

    /**
     * @Serializable data classes demonstrating kotlinx.serialization integration
     * These classes will be serialized using the JsonParameterSerializer's kotlinx.serialization support
     */

    @Serializable
    data class UserProfile(
        val userId: String,
        val displayName: String,
        val email: String,
        val isVerified: Boolean,
        val membershipLevel: String,
        val joinedDate: Long,
    )

    @Serializable
    data class EventMetadata(
        val source: String,
        val version: String,
        val experiments: List<String>,
        val customProperties: Map<String, String>,
    )

    @Serializable
    data class AnalyticsEventData(
        val eventType: String,
        val timestamp: Long,
        val userProfile: UserProfile,
        val metadata: EventMetadata,
    )
}
