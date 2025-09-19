package com.shalan.analyticsannotation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.Param
import com.shalan.analytics.annotation.Track
import com.shalan.analytics.annotation.TrackScreen
import com.shalan.analytics.core.TrackedScreenParamsProvider
import com.shalan.analyticsannotation.exclude.ExcludeActivity

@TrackScreen(screenName = "Main Screen")
class MainActivity : AppCompatActivity(), TrackedScreenParamsProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.example_button).setOnClickListener {
            navigateToScreen("ExampleActivity", "button_click")
            startActivity(Intent(this, ExampleActivity::class.java))
        }

        findViewById<Button>(R.id.composable_button).setOnClickListener {
            navigateToScreen("ComposableActivity", "button_click")
            startActivity(Intent(this, ComposableActivity::class.java))
        }
        findViewById<Button>(R.id.exclude_button).setOnClickListener {
            navigateToScreen("ExcludeActivity", "button_click")
            startActivity(Intent(this, ExcludeActivity::class.java))
        }

        findViewById<Button>(R.id.method_tracking_button).setOnClickListener {
            navigateToScreen("MethodTrackingExample", "button_click")
            startActivity(Intent(this, MethodTrackingExampleActivity::class.java))
        }

        findViewById<Button>(R.id.viewmodel_tracking_button).setOnClickListener {
            navigateToScreen("UserProfileActivity", "button_click")
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        findViewById<Button>(R.id.parameter_limit_button).setOnClickListener {
            navigateToScreen("ParameterLimitDemo", "button_click")
            demonstrateParameterLimits()
        }

        // Demonstrate method tracking with user actions
        performUserAction("app_launch", "MainActivity", isNewUser = false)
    }

    override fun getTrackedScreenParams(): Map<String, Any> {
        // Provide dynamic parameters here
        return mapOf(
            "user_id" to "12345",
            "user_name" to "John Doe",
        )
    }

    /**
     * Example of method tracking with parameters.
     * This method tracks navigation events with custom parameters.
     */
    @Track(eventName = "navigation_event", includeGlobalParams = true)
    private fun navigateToScreen(
        @Param("destination") destination: String,
        @Param("trigger") trigger: String,
    ) {
        Toast.makeText(this, "Navigating to $destination via $trigger", Toast.LENGTH_SHORT).show()
    }

    /**
     * Example of method tracking with different parameter types.
     * This tracks user actions with various data types.
     */
    @Track(eventName = "user_action", includeGlobalParams = false)
    private fun performUserAction(
        @Param("action_type") actionType: String,
        @Param("screen_context") screenContext: String,
        @Param("is_new_user") isNewUser: Boolean,
    ) {
        // Simulate some user action processing
        val timestamp = System.currentTimeMillis()
        logUserBehavior(actionType, timestamp)
    }

    /**
     * Example of method tracking with primitive and object parameters.
     * This demonstrates parameter serialization capabilities.
     */
    @Track(eventName = "user_behavior_logged")
    private fun logUserBehavior(
        @Param("action") action: String,
        @Param("timestamp") timestamp: Long,
    ) {
        // Example business logic that gets automatically tracked
        println("Logging behavior: $action at $timestamp")
    }

    /**
     * Example of tracked method without parameters.
     * This shows basic method tracking for simple events.
     */
    @Track(eventName = "feature_accessed")
    fun onFeatureButtonClicked() {
        // This method will be tracked with execution timing
        Thread.sleep(100) // Simulate some processing time
        Toast.makeText(this, "Feature accessed - this call was tracked!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Demonstrates parameter limit functionality by using the ParameterLimitExampleService.
     * This method shows how the plugin handles methods with many parameters.
     */
    private fun demonstrateParameterLimits() {
        val parameterLimitService = ParameterLimitExampleService()

        Toast.makeText(this, "Demonstrating parameter limits - check logs!", Toast.LENGTH_LONG).show()

        // Execute the parameter limit examples
        parameterLimitService.demonstrateParameterLimits()

        Toast.makeText(this, "Parameter limit demo completed", Toast.LENGTH_SHORT).show()
    }
}
