package com.shalan.analytics.plugin

open class AnalyticsPluginExtension {
    /**
     * Enable or disable the analytics plugin
     */
    var enabled: Boolean = true

    /**
     * Enable debug mode for verbose logging
     */
    var debugMode: Boolean = false

    /**
     * Enable tracking for Activities
     */
    var trackActivities: Boolean = true

    /**
     * Enable tracking for Fragments
     */
    var trackFragments: Boolean = true

    /**
     * Enable tracking for Composables
     */
    var trackComposables: Boolean = true

    /**
     * Packages to exclude from tracking injection
     */
    var excludePackages: Set<String> = emptySet()

    /**
     * Packages to include for tracking injection (if empty, all packages are included)
     */
    var includePackages: Set<String> = emptySet()

    override fun toString(): String {
        return "AnalyticsPluginExtension(" +
            "enabled=$enabled, " +
            "debugMode=$debugMode, " +
            "trackActivities=$trackActivities, " +
            "trackFragments=$trackFragments, " +
            "trackComposables=$trackComposables, " +
            "excludePackages=$excludePackages, " +
            "includePackages=$includePackages" +
            ")"
    }
}
