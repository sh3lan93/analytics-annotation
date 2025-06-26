package com.shalan.analytics.core

/**
 * A Kotlin DSL (Domain Specific Language) function for configuring [AnalyticsConfig].
 * This function provides a concise and readable way to build an [AnalyticsConfig] instance.
 *
 * @param block A lambda with [AnalyticsConfig] as its receiver, allowing for direct configuration
 *   of [AnalyticsConfig] properties within the lambda.
 * @return A fully configured [AnalyticsConfig] instance.
 */
fun analyticsConfig(block: AnalyticsConfig.() -> Unit): AnalyticsConfig {
    return AnalyticsConfig().apply(block)
}
