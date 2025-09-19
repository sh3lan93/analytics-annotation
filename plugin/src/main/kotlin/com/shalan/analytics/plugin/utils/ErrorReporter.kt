package com.shalan.analytics.plugin.utils

import com.shalan.analytics.plugin.AnalyticsPluginExtension
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Enhanced error reporting utility for the Analytics Transform plugin.
 * Provides detailed error information, source location mapping, and debugging context.
 */
object ErrorReporter {
    data class TransformError(
        val className: String,
        val sourceFile: String? = null,
        val errorType: ErrorType,
        val message: String,
        val throwable: Throwable? = null,
        val context: Map<String, Any> = emptyMap(),
    )

    enum class ErrorType {
        BYTECODE_READING_ERROR,
        BYTECODE_WRITING_ERROR,
        ANNOTATION_SCANNING_ERROR,
        TRANSFORMATION_ERROR,
        VALIDATION_ERROR,
        IO_ERROR,
    }

    private val errors = mutableListOf<TransformError>()

    fun reportError(
        className: String,
        errorType: ErrorType,
        message: String,
        throwable: Throwable? = null,
        sourceFile: String? = null,
        context: Map<String, Any> = emptyMap(),
    ) {
        val error =
            TransformError(
                className = className,
                sourceFile = sourceFile,
                errorType = errorType,
                message = message,
                throwable = throwable,
                context = context,
            )

        synchronized(errors) {
            errors.add(error)
        }

        // Log immediately for debugging
        logError(error)
    }

    private fun getErrors(): List<TransformError> = synchronized(errors) { errors.toList() }

    fun clearErrors() = synchronized(errors) { errors.clear() }

    fun hasErrors(): Boolean = synchronized(errors) { errors.isNotEmpty() }

    fun generateErrorReport(extension: AnalyticsPluginExtension): String {
        val report = StringBuilder()
        val errorsByType = getErrors().groupBy { it.errorType }

        report.appendLine("=== Analytics Transform Error Report ===")
        report.appendLine("Total errors: ${getErrors().size}")
        report.appendLine()

        errorsByType.forEach { (type, typeErrors) ->

            report.appendLine("${type.name} (${typeErrors.size} errors):")
            typeErrors.forEach { error ->
                report.appendLine("  - Class: ${error.className}")
                if (error.sourceFile != null) {
                    report.appendLine("    Source: ${error.sourceFile}")
                }
                report.appendLine("    Message: ${error.message}")

                if (error.context.isNotEmpty()) {
                    report.appendLine("    Context:")
                    error.context.forEach { (key, value) ->
                        report.appendLine("      $key: $value")
                    }
                }

                if (extension.debugMode && error.throwable != null) {
                    report.appendLine("    Stack trace:")
                    val sw = StringWriter()
                    error.throwable.printStackTrace(PrintWriter(sw))
                    sw.toString().lines().forEach { line ->
                        report.appendLine("      $line")
                    }
                }
                report.appendLine()
            }
        }

        report.appendLine("=========================================")
        return report.toString()
    }

    private fun logError(error: TransformError) {
        println("ERROR [${error.errorType}]: ${error.message}")
        println("  Class: ${error.className}")
        if (error.sourceFile != null) {
            println("  Source: ${error.sourceFile}")
        }

        if (error.context.isNotEmpty()) {
            println("  Context: ${error.context}")
        }

        error.throwable?.let { throwable ->
            println("  Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
        }
    }
}
