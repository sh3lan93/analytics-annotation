package com.shalan.analytics.core

/**
 * A Kotlin DSL (Domain Specific Language) function for configuring [AnalyticsConfig].
 * This function provides a concise and readable way to build an [AnalyticsConfig] instance.
 *
 * @param block A lambda with [AnalyticsConfig] as its receiver, allowing for direct configuration
 *   of [AnalyticsConfig] properties within the lambda.
 * @return A fully configured [AnalyticsConfig] instance.
 *
 * ## Usage Example:
 * ```kotlin
 * val config = analyticsConfig {
 *     debugMode = true
 *     errorHandler = { throwable ->
 *         Log.e("Analytics", "Analytics error", throwable)
 *     }
 *     providers.add(LogcatAnalyticsProvider())
 *     providers.add(DebugAnalyticsProvider())
 *
 *     methodTracking {
 *         enabled = true
 *         customSerializers.add(MyCustomParameterSerializer())
 *     }
 * }
 * ```
 */
fun analyticsConfig(block: AnalyticsConfig.() -> Unit): AnalyticsConfig {
    return AnalyticsConfig().apply(block)
}

/**
 * A Kotlin DSL function for configuring method tracking within an [AnalyticsConfig] block.
 * This function provides a concise way to configure method-level tracking options.
 *
 * Error handling is now configured at the global [AnalyticsConfig] level using the errorHandler property.
 *
 * @param block A lambda with [MethodTrackingConfig] as its receiver for configuration.
 *
 * ## Usage Example:
 * ```kotlin
 * val config = analyticsConfig {
 *     debugMode = true
 *     providers.add(LogcatAnalyticsProvider())
 *
 *     // Global error handler for all analytics operations
 *     errorHandler = { throwable ->
 *         Log.e("Analytics", "Analytics error", throwable)
 *     }
 *
 *     methodTracking {
 *         enabled = true
 *         customSerializers.add(MyCustomParameterSerializer())
 *     }
 * }
 * ```
 */
fun AnalyticsConfig.methodTracking(block: MethodTrackingConfig.() -> Unit) {
    methodTracking.apply(block)
}
