package com.shalan.analytics.core

/**
 * Configuration class for the Screen Tracking Analytics library.
 * Use the [analyticsConfig] DSL function to create and configure an instance of this class.
 *
 * @property providers A mutable list of [AnalyticsProvider] implementations.
 *   All registered providers will receive analytics events.
 * @property debugMode A boolean flag indicating whether the library is in debug mode.
 *   In debug mode, additional logging or debugging features might be enabled.
 */
class AnalyticsConfig {
    val providers = mutableListOf<AnalyticsProvider>()
    var debugMode: Boolean = false
}
