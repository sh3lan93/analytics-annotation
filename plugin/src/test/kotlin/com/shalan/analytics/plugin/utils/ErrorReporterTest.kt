package com.shalan.analytics.plugin.utils

import com.shalan.analytics.plugin.AnalyticsPluginExtension
import org.junit.After
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorReporterTest {
    
    @After
    fun cleanup() {
        ErrorReporter.clearErrors()
    }
    
    @Test
    fun `hasErrors returns false initially`() {
        assertFalse(ErrorReporter.hasErrors())
    }
    
    @Test
    fun `hasErrors returns true after reporting error`() {
        ErrorReporter.reportError(
            className = "TestClass", 
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Test error"
        )
        
        assertTrue(ErrorReporter.hasErrors())
    }
    
    @Test
    fun `clearErrors removes all errors`() {
        ErrorReporter.reportError(
            className = "TestClass1",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Error 1"
        )
        ErrorReporter.reportError(
            className = "TestClass2",
            errorType = ErrorReporter.ErrorType.ANNOTATION_SCANNING_ERROR,
            message = "Error 2"
        )
        
        assertTrue(ErrorReporter.hasErrors())
        
        ErrorReporter.clearErrors()
        
        assertFalse(ErrorReporter.hasErrors())
    }
    
    @Test
    fun `reportError with minimal parameters works`() {
        ErrorReporter.reportError(
            className = "MinimalClass",
            errorType = ErrorReporter.ErrorType.VALIDATION_ERROR,
            message = "Minimal error"
        )
        
        assertTrue(ErrorReporter.hasErrors())
        
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        assertTrue(report.contains("MinimalClass"))
        assertTrue(report.contains("Minimal error"))
        assertTrue(report.contains("VALIDATION_ERROR"))
    }
    
    @Test
    fun `reportError with throwable includes stack trace in debug mode`() {
        val testException = RuntimeException("Test exception")
        
        ErrorReporter.reportError(
            className = "ExceptionClass",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Error with exception",
            throwable = testException
        )
        
        val debugExtension = AnalyticsPluginExtension().apply { debugMode = true }
        val debugReport = ErrorReporter.generateErrorReport(debugExtension)
        
        assertTrue(debugReport.contains("Test exception"))
        assertTrue(debugReport.contains("RuntimeException"))
    }
    
    @Test
    fun `reportError with throwable excludes stack trace when debug mode disabled`() {
        val testException = RuntimeException("Test exception")
        
        ErrorReporter.reportError(
            className = "ExceptionClass",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Error with exception",
            throwable = testException
        )
        
        val releaseExtension = AnalyticsPluginExtension().apply { debugMode = false }
        val releaseReport = ErrorReporter.generateErrorReport(releaseExtension)
        
        assertTrue(releaseReport.contains("Error with exception"))
        assertFalse(releaseReport.contains("RuntimeException")) // Stack trace should be excluded
    }
    
    @Test
    fun `reportError with context includes additional information`() {
        ErrorReporter.reportError(
            className = "ContextClass",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Error with context",
            context = mapOf(
                "methodName" to "onCreate",
                "annotationType" to "@TrackScreen",
                "lineNumber" to "42"
            )
        )
        
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        assertTrue(report.contains("methodName: onCreate"))
        assertTrue(report.contains("annotationType: @TrackScreen"))
        assertTrue(report.contains("lineNumber: 42"))
    }
    
    @Test
    fun `generateErrorReport groups errors by type`() {
        ErrorReporter.reportError(
            className = "Class1",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Transform error 1"
        )
        ErrorReporter.reportError(
            className = "Class2", 
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Transform error 2"
        )
        ErrorReporter.reportError(
            className = "Class3",
            errorType = ErrorReporter.ErrorType.VALIDATION_ERROR,
            message = "Validation error 1"
        )
        
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        assertTrue(report.contains("TRANSFORMATION_ERROR (2 errors)"))
        assertTrue(report.contains("VALIDATION_ERROR (1 errors)"))
    }
    
    @Test
    fun `generateErrorReport includes summary`() {
        ErrorReporter.reportError(
            className = "Class1",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Error 1"
        )
        ErrorReporter.reportError(
            className = "Class2",
            errorType = ErrorReporter.ErrorType.VALIDATION_ERROR,
            message = "Error 2"
        )
        
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        assertTrue(report.contains("Analytics Plugin Error Report"))
        assertTrue(report.contains("Total Errors: 2"))
        assertTrue(report.contains("Error Types: 2"))
    }
    
    @Test
    fun `generateErrorReport for empty errors returns no errors message`() {
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        assertTrue(report.contains("No errors reported"))
    }
    
    @Test
    fun `error reporting is thread-safe`() {
        val threadCount = 10
        val errorsPerThread = 5
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        
        repeat(threadCount) { threadIndex ->
            executor.submit {
                try {
                    repeat(errorsPerThread) { errorIndex ->
                        ErrorReporter.reportError(
                            className = "Thread${threadIndex}Class$errorIndex",
                            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
                            message = "Error from thread $threadIndex, error $errorIndex"
                        )
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        latch.await()
        executor.shutdown()
        
        assertTrue(ErrorReporter.hasErrors())
        
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        // Should have exactly threadCount * errorsPerThread errors
        assertTrue(report.contains("Total Errors: ${threadCount * errorsPerThread}"))
    }
    
    @Test
    fun `error types enum covers all expected cases`() {
        val errorTypes = ErrorReporter.ErrorType.values()
        
        assertTrue(errorTypes.contains(ErrorReporter.ErrorType.TRANSFORMATION_ERROR))
        assertTrue(errorTypes.contains(ErrorReporter.ErrorType.ANNOTATION_SCANNING_ERROR))
        assertTrue(errorTypes.contains(ErrorReporter.ErrorType.VALIDATION_ERROR))
        assertTrue(errorTypes.contains(ErrorReporter.ErrorType.BYTECODE_READING_ERROR))
        assertTrue(errorTypes.contains(ErrorReporter.ErrorType.BYTECODE_WRITING_ERROR))
        assertTrue(errorTypes.contains(ErrorReporter.ErrorType.IO_ERROR))
        
        assertEquals(6, errorTypes.size)
    }
    
    @Test
    fun `TransformError data class stores all information correctly`() {
        val context = mapOf("key1" to "value1", "key2" to "value2")
        val throwable = RuntimeException("Test exception")
        
        val error = ErrorReporter.TransformError(
            className = "TestClass",
            errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
            message = "Test message",
            throwable = throwable,
            context = context
        )
        
        assertEquals("TestClass", error.className)
        assertEquals(ErrorReporter.ErrorType.TRANSFORMATION_ERROR, error.errorType)
        assertEquals("Test message", error.message)
        assertEquals(throwable, error.throwable)
        assertEquals(context, error.context)
    }
    
    @Test
    fun `multiple error types are reported correctly`() {
        ErrorReporter.ErrorType.values().forEach { errorType ->
            ErrorReporter.reportError(
                className = "TestClass${errorType.name}",
                errorType = errorType,
                message = "Test error for ${errorType.name}"
            )
        }
        
        assertTrue(ErrorReporter.hasErrors())
        
        val extension = AnalyticsPluginExtension()
        val report = ErrorReporter.generateErrorReport(extension)
        
        ErrorReporter.ErrorType.values().forEach { errorType ->
            assertTrue(report.contains(errorType.name))
        }
    }
}