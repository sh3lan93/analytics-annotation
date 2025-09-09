package com.shalan.analytics.core

/**
 * Configuration class for the Screen Tracking Analytics library.
 * Use the [analyticsConfig] DSL function to create and configure an instance of this class.
 *
 * @property providers A mutable list of [AnalyticsProvider] implementations.
 *   All registered providers will receive analytics events.
 * @property debugMode A boolean flag indicating whether the library is in debug mode.
 *   In debug mode, additional logging or debugging features might be enabled.
 * @property methodTracking Configuration for method-level tracking using @Track annotation.
 */
class AnalyticsConfig {
    val providers = mutableListOf<AnalyticsProvider>()
    var debugMode: Boolean = false
    val methodTracking = MethodTrackingConfig()
}

/**
 * Configuration for method-level tracking functionality.
 * This controls how @Track annotated methods are processed and handled.
 */
class MethodTrackingConfig {
    /**
     * Whether method tracking is enabled. When false, @Track annotations are ignored.
     * Default: true
     */
    var enabled: Boolean = true

    /**
     * Custom error handler for method tracking failures.
     * If not set, errors are silently ignored to prevent app crashes.
     */
    var errorHandler: ((Throwable) -> Unit)? = null

    /**
     * Custom parameter serializers for method parameters.
     * These are checked in addition to the built-in serializers.
     */
    val customSerializers = mutableListOf<ParameterSerializer>()
}
