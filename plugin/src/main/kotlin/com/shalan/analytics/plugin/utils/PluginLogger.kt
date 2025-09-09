package com.shalan.analytics.plugin.utils

/**
 * Centralized logging utility for the Analytics Transform plugin.
 * Provides consistent logging across all plugin components with configurable debug mode.
 */
object PluginLogger {
    private var debugMode: Boolean = false

    /**
     * Sets the debug mode for the logger.
     * When debug mode is enabled, debug messages will be printed to the console.
     *
     * @param enabled Whether to enable debug logging
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
    }

    /**
     * Logs a debug message. Only printed when debug mode is enabled.
     *
     * @param message The debug message to log
     * @param tag Optional tag to identify the source of the log message
     */
    fun debug(
        message: String,
        tag: String? = null,
    ) {
        if (debugMode) {
            val prefix = if (tag != null) "DEBUG [$tag]" else "DEBUG"
            println("$prefix: $message")
        }
    }

    /**
     * Logs an informational message. Always printed regardless of debug mode.
     *
     * @param message The information message to log
     * @param tag Optional tag to identify the source of the log message
     */
    fun info(
        message: String,
        tag: String? = null,
    ) {
        val prefix = if (tag != null) "INFO [$tag]" else "INFO"
        println("$prefix: $message")
    }

    /**
     * Logs a warning message. Always printed regardless of debug mode.
     *
     * @param message The warning message to log
     * @param tag Optional tag to identify the source of the log message
     */
    fun warn(
        message: String,
        tag: String? = null,
    ) {
        val prefix = if (tag != null) "WARN [$tag]" else "WARN"
        println("$prefix: $message")
    }

    /**
     * Logs an error message. Always printed regardless of debug mode.
     *
     * @param message The error message to log
     * @param throwable Optional throwable for stack trace
     * @param tag Optional tag to identify the source of the log message
     */
    fun error(
        message: String,
        throwable: Throwable? = null,
        tag: String? = null,
    ) {
        val prefix = if (tag != null) "ERROR [$tag]" else "ERROR"
        println("$prefix: $message")
        throwable?.printStackTrace()
    }

    /**
     * Logs a force debug message. These are critical debug messages that are
     * always printed regardless of debug mode setting.
     *
     * @param message The force debug message to log
     * @param tag Optional tag to identify the source of the log message
     */
    fun forceDebug(
        message: String,
        tag: String? = null,
    ) {
        val prefix = if (tag != null) "FORCE_DEBUG [$tag]" else "FORCE_DEBUG"
        println("$prefix: $message")
    }
}
