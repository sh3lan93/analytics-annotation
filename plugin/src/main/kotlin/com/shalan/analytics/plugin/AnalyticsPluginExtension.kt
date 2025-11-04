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
     * Packages to exclude from tracking injection
     */
    var excludePackages: Set<String> = emptySet()

    /**
     * Packages to include for tracking injection (if empty, all packages are included)
     */
    var includePackages: Set<String> = emptySet()

    /**
     * Method tracking configuration
     */
    val methodTracking = MethodTrackingExtension()

    /**
     * DSL function for configuring method tracking within the analytics plugin block.
     *
     * ## Usage Example:
     * ```kotlin
     * analytics {
     *     enabled = true
     *     debugMode = false
     *
     *     methodTracking {
     *         enabled = true
     *         maxParametersPerMethod = 15
     *     }
     * }
     * ```
     */
    fun methodTracking(action: MethodTrackingExtension.() -> Unit) {
        methodTracking.apply(action)
    }

    override fun toString(): String {
        return "AnalyticsPluginExtension(" +
            "enabled=$enabled, " +
            "debugMode=$debugMode, " +
            "trackActivities=$trackActivities, " +
            "trackFragments=$trackFragments, " +
            "excludePackages=$excludePackages, " +
            "includePackages=$includePackages, " +
            "methodTracking=$methodTracking" +
            ")"
    }
}

/**
 * Configuration extension for method tracking in the Gradle plugin.
 * This controls build-time behavior for @Track annotation processing.
 */
open class MethodTrackingExtension {
    /**
     * Whether method tracking instrumentation is enabled during build.
     * When false, @Track annotations are not processed by ASM.
     * Default: true
     */
    var enabled: Boolean = true

    /**
     * Maximum number of parameters to process per @Track method.
     * Methods with more @Param annotations will have additional parameters ignored.
     * This prevents build performance issues with methods having many parameters.
     * Default: 10
     */
    var maxParametersPerMethod: Int = 10

    /**
     * Whether to validate @Track annotation usage during build.
     * When enabled, performs compile-time validation of @Track and @Param usage.
     * Default: true
     */
    var validateAnnotations: Boolean = true

    /**
     * Set of method names to exclude from tracking, even if they have @Track annotation.
     * Useful for excluding problematic methods from instrumentation.
     */
    var excludeMethods: Set<String> = emptySet()

    /**
     * Set of class patterns to include for method tracking.
     * If empty, all classes are eligible for method tracking.
     * Patterns support wildcards: "com.example.*", "*.Activity"
     */
    var includeClassPatterns: Set<String> = emptySet()

    /**
     * Set of class patterns to exclude from method tracking.
     * Patterns support wildcards: "com.example.*", "*.Activity"
     */
    var excludeClassPatterns: Set<String> = emptySet()

    override fun toString(): String {
        return "MethodTrackingExtension(" +
            "enabled=$enabled, " +
            "maxParametersPerMethod=$maxParametersPerMethod, " +
            "validateAnnotations=$validateAnnotations, " +
            "excludeMethods=$excludeMethods, " +
            "includeClassPatterns=$includeClassPatterns, " +
            "excludeClassPatterns=$excludeClassPatterns" +
            ")"
    }
}
