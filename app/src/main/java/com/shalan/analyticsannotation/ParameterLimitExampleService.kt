package com.shalan.analyticsannotation

import com.shalan.analytics.annotation.Param
import com.shalan.analytics.annotation.Track
import com.shalan.analytics.annotation.Trackable

/**
 * Example service demonstrating parameter limit configuration.
 *
 * This class shows how the plugin handles methods with many parameters
 * and respects the maxParametersPerMethod configuration setting.
 *
 * The plugin is configured with maxParametersPerMethod = 10 in build.gradle.kts,
 * so only the first 10 @Param annotated parameters will be tracked.
 */
@Trackable
class ParameterLimitExampleService {
    /**
     * Example method with exactly 10 parameters - all will be tracked.
     * This method demonstrates the limit boundary.
     */
    @Track(eventName = "data_export_initiated", includeGlobalParams = true)
    fun exportDataWithTenParams(
        @Param("export_format") format: String,
        @Param("date_range") dateRange: String,
        @Param("user_id") userId: String,
        @Param("department") department: String,
        @Param("include_metadata") includeMetadata: Boolean,
        @Param("compression_level") compressionLevel: Int,
        @Param("export_size_mb") exportSize: Double,
        @Param("destination") destination: String,
        @Param("priority") priority: String,
        @Param("notification_email") notificationEmail: String,
    ) {
        println("Exporting data with format: $format, range: $dateRange")
        println("All 10 parameters will be tracked in analytics")
    }

    /**
     * Example method with more than 10 parameters - only first 10 will be tracked.
     * This demonstrates the parameter limit enforcement.
     */
    @Track(eventName = "complex_data_processing", includeGlobalParams = true)
    fun processComplexDataBeyondLimit(
        @Param("param1") p1: String,
        @Param("param2") p2: String,
        @Param("param3") p3: String,
        @Param("param4") p4: String,
        @Param("param5") p5: String,
        @Param("param6") p6: String,
        @Param("param7") p7: String,
        @Param("param8") p8: String,
        @Param("param9") p9: String,
        @Param("param10") p10: String,
        // Will be ignored (beyond limit)
        @Param("param11_ignored") p11: String,
        // Will be ignored (beyond limit)
        @Param("param12_ignored") p12: String,
        // Will be ignored (beyond limit)
        @Param("param13_ignored") p13: String,
        // Not annotated, always ignored
        nonTrackedParam: String,
        // Not annotated, always ignored
        anotherNonTracked: Int,
    ) {
        println("Processing complex data with many parameters")
        println("Only first 10 @Param annotated parameters will be tracked")
        println("Parameters 11-13 and non-annotated params will be ignored")
    }

    /**
     * Example showing mixed parameter types that respect the limit.
     * Demonstrates different data types within the parameter limit.
     */
    @Track(eventName = "user_preferences_updated", includeGlobalParams = true)
    fun updateUserPreferencesWithLimit(
        @Param("user_id") userId: String,
        @Param("theme") theme: String,
        @Param("language") language: String,
        @Param("notifications_enabled") notificationsEnabled: Boolean,
        @Param("sync_frequency_hours") syncFrequency: Int,
        @Param("data_usage_limit_mb") dataLimit: Long,
        @Param("auto_backup") autoBackup: Boolean,
        @Param("privacy_level") privacyLevel: String,
        @Param("font_size") fontSize: Float,
        @Param("timeout_minutes") timeoutMinutes: Int,
        // Will be ignored (11th param)
        @Param("extra_setting1") extraSetting1: String,
        // Will be ignored (12th param)
        @Param("extra_setting2") extraSetting2: String,
    ) {
        println("Updating user preferences")
        println("First 10 parameters tracked, last 2 ignored due to limit")
    }

    /**
     * Helper method to demonstrate the service functionality.
     * This method is not tracked since it has no @Track annotation.
     */
    fun demonstrateParameterLimits() {
        println("=== Parameter Limit Examples ===")

        // Example 1: Exactly at the limit (10 parameters)
        exportDataWithTenParams(
            format = "CSV",
            dateRange = "2024-01-01_to_2024-12-31",
            userId = "user123",
            department = "Engineering",
            includeMetadata = true,
            compressionLevel = 5,
            exportSize = 25.5,
            destination = "S3",
            priority = "HIGH",
            notificationEmail = "admin@example.com",
        )

        // Example 2: Beyond the limit (15 parameters total)
        processComplexDataBeyondLimit(
            p1 = "value1", p2 = "value2", p3 = "value3", p4 = "value4", p5 = "value5",
            p6 = "value6", p7 = "value7", p8 = "value8", p9 = "value9", p10 = "value10",
            p11 = "ignored_value11", p12 = "ignored_value12", p13 = "ignored_value13",
            nonTrackedParam = "never_tracked",
            anotherNonTracked = 42,
        )

        // Example 3: Mixed types with limit
        updateUserPreferencesWithLimit(
            userId = "user456",
            theme = "dark",
            language = "en",
            notificationsEnabled = true,
            syncFrequency = 24,
            dataLimit = 1000L,
            autoBackup = false,
            privacyLevel = "medium",
            fontSize = 16.0f,
            timeoutMinutes = 30,
            extraSetting1 = "ignored_extra1",
            extraSetting2 = "ignored_extra2",
        )

        println("=== All examples executed ===")
        println("Check analytics logs to see parameter tracking in action")
        println("Note: Parameters beyond the limit (${MAX_PARAMS_DEFAULT}) are not tracked")
    }

    companion object {
        private const val MAX_PARAMS_DEFAULT = 10
    }
}
