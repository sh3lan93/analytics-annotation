package com.shalan.analytics.plugin.instrumentation

import com.shalan.analytics.plugin.utils.PluginLogger

/**
 * Optimized logger for the analytics bytecode instrumentation.
 *
 * This logger implements lazy evaluation of log messages and conditional logging
 * to minimize performance impact when debugMode is disabled.
 *
 * Benefits:
 * - Messages are only constructed if debug mode is enabled
 * - Uses lambdas for lazy evaluation
 * - Automatically prefixes all messages with context
 */
class TrackingLogger(
    val debugMode: Boolean,
    val context: String = "AnalyticsClassVisitor",
) {
    /**
     * Logs a debug message only if debug mode is enabled.
     *
     * The message is constructed lazily - only if it will actually be logged.
     *
     * Usage:
     * ```kotlin
     * logger.debug { "Expensive string interpolation: $expensiveValue" }
     * ```
     */
    inline fun debug(messageBuilder: () -> String) {
        if (debugMode) {
            val message = "$context: ${messageBuilder()}"
            PluginLogger.debug(message)
        }
    }

    /**
     * Logs a message unconditionally (for important operations).
     */
    inline fun info(messageBuilder: () -> String) {
        val message = "$context: ${messageBuilder()}"
        PluginLogger.debug(message)
    }

    /**
     * Logs an error message.
     */
    fun error(
        message: String,
        throwable: Throwable? = null,
    ) {
        PluginLogger.error("$context: $message", throwable)
    }

    /**
     * Creates a child logger with additional context.
     */
    fun withContext(additionalContext: String): TrackingLogger {
        return TrackingLogger(debugMode, "$context[$additionalContext]")
    }

    companion object {
        /**
         * Creates a logger for a specific class being instrumented.
         */
        fun forClass(
            className: String,
            debugMode: Boolean,
        ): TrackingLogger {
            return TrackingLogger(debugMode, "AnalyticsClassVisitor[$className]")
        }
    }
}
