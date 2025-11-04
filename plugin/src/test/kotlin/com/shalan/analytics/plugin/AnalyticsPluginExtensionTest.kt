package com.shalan.analytics.plugin

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnalyticsPluginExtensionTest {
    @Test
    fun `extension has correct default values`() {
        val extension = AnalyticsPluginExtension()

        assertTrue(extension.enabled)
        assertFalse(extension.debugMode)
        assertTrue(extension.trackActivities)
        assertTrue(extension.trackFragments)
        assertTrue(extension.includePackages.isEmpty())
        assertTrue(extension.excludePackages.isEmpty())
    }

    @Test
    fun `extension properties can be modified`() {
        val extension = AnalyticsPluginExtension()

        extension.enabled = false
        extension.debugMode = true
        extension.trackActivities = false
        extension.trackFragments = false
        extension.includePackages = setOf("com.test")
        extension.excludePackages = setOf("com.exclude")

        assertFalse(extension.enabled)
        assertTrue(extension.debugMode)
        assertFalse(extension.trackActivities)
        assertFalse(extension.trackFragments)
        assertEquals(setOf("com.test"), extension.includePackages)
        assertEquals(setOf("com.exclude"), extension.excludePackages)
    }

    @Test
    fun `extension toString provides complete information`() {
        val extension = AnalyticsPluginExtension()
        extension.enabled = false
        extension.debugMode = true
        extension.trackActivities = false
        extension.trackFragments = false
        extension.includePackages = setOf("com.include")
        extension.excludePackages = setOf("com.exclude")

        val toString = extension.toString()
        assertNotNull(toString)
        assertTrue(toString.contains("enabled=false"))
        assertTrue(toString.contains("debugMode=true"))
        assertTrue(toString.contains("trackActivities=false"))
        assertTrue(toString.contains("trackFragments=false"))
        assertTrue(toString.contains("includePackages=[com.include]"))
        assertTrue(toString.contains("excludePackages=[com.exclude]"))
    }

    @Test
    fun `extension handles empty package sets correctly`() {
        val extension = AnalyticsPluginExtension()

        assertTrue(extension.includePackages.isEmpty())
        assertTrue(extension.excludePackages.isEmpty())

        val toString = extension.toString()
        assertTrue(toString.contains("includePackages=[]"))
        assertTrue(toString.contains("excludePackages=[]"))
    }

    @Test
    fun `extension handles multiple package entries`() {
        val extension = AnalyticsPluginExtension()

        extension.includePackages = setOf("com.app", "com.feature", "com.ui")
        extension.excludePackages = setOf("com.test", "com.debug", "com.mock")

        assertEquals(3, extension.includePackages.size)
        assertEquals(3, extension.excludePackages.size)
        assertTrue(extension.includePackages.contains("com.app"))
        assertTrue(extension.includePackages.contains("com.feature"))
        assertTrue(extension.includePackages.contains("com.ui"))
        assertTrue(extension.excludePackages.contains("com.test"))
        assertTrue(extension.excludePackages.contains("com.debug"))
        assertTrue(extension.excludePackages.contains("com.mock"))
    }

    @Test
    fun `extension properties are independent`() {
        val extension1 = AnalyticsPluginExtension()
        val extension2 = AnalyticsPluginExtension()

        extension1.enabled = false
        extension1.debugMode = true
        extension1.includePackages = setOf("com.first")

        // extension2 should maintain default values
        assertTrue(extension2.enabled)
        assertFalse(extension2.debugMode)
        assertTrue(extension2.includePackages.isEmpty())
    }

    @Test
    fun `extension tracking flags work independently`() {
        val extension = AnalyticsPluginExtension()

        // Test individual tracking flags
        extension.trackActivities = false
        assertTrue(extension.trackFragments) // Should remain true

        extension.trackFragments = false
        assertFalse(extension.trackActivities) // Should remain false
    }

    @Test
    fun `extension has methodTracking with correct defaults`() {
        val extension = AnalyticsPluginExtension()

        assertNotNull(extension.methodTracking)
        assertTrue(extension.methodTracking.enabled)
        assertEquals(10, extension.methodTracking.maxParametersPerMethod)
        assertTrue(extension.methodTracking.validateAnnotations)
        assertTrue(extension.methodTracking.excludeMethods.isEmpty())
        assertTrue(extension.methodTracking.includeClassPatterns.isEmpty())
        assertTrue(extension.methodTracking.excludeClassPatterns.isEmpty())
    }

    @Test
    fun `methodTracking DSL function works correctly`() {
        val extension = AnalyticsPluginExtension()

        extension.methodTracking {
            enabled = false
            maxParametersPerMethod = 15
            validateAnnotations = false
            excludeMethods = setOf("excludeMe", "andMe")
            includeClassPatterns = setOf("com.app.*", "*.Activity")
            excludeClassPatterns = setOf("com.test.*")
        }

        assertFalse(extension.methodTracking.enabled)
        assertEquals(15, extension.methodTracking.maxParametersPerMethod)
        assertFalse(extension.methodTracking.validateAnnotations)
        assertEquals(setOf("excludeMe", "andMe"), extension.methodTracking.excludeMethods)
        assertEquals(setOf("com.app.*", "*.Activity"), extension.methodTracking.includeClassPatterns)
        assertEquals(setOf("com.test.*"), extension.methodTracking.excludeClassPatterns)
    }

    @Test
    fun `methodTracking toString includes all properties`() {
        val extension = AnalyticsPluginExtension()

        extension.methodTracking {
            enabled = false
            maxParametersPerMethod = 20
            validateAnnotations = false
            excludeMethods = setOf("test")
            includeClassPatterns = setOf("com.*")
            excludeClassPatterns = setOf("*.Test")
        }

        val toString = extension.toString()
        assertTrue(toString.contains("methodTracking="))

        val methodTrackingString = extension.methodTracking.toString()
        assertTrue(methodTrackingString.contains("enabled=false"))
        assertTrue(methodTrackingString.contains("maxParametersPerMethod=20"))
        assertTrue(methodTrackingString.contains("validateAnnotations=false"))
        assertTrue(methodTrackingString.contains("excludeMethods=[test]"))
        assertTrue(methodTrackingString.contains("includeClassPatterns=[com.*]"))
        assertTrue(methodTrackingString.contains("excludeClassPatterns=[*.Test]"))
    }

    @Test
    fun `methodTracking instances are independent`() {
        val extension1 = AnalyticsPluginExtension()
        val extension2 = AnalyticsPluginExtension()

        extension1.methodTracking {
            enabled = false
            maxParametersPerMethod = 5
        }

        // extension2 should maintain defaults
        assertTrue(extension2.methodTracking.enabled)
        assertEquals(10, extension2.methodTracking.maxParametersPerMethod)
    }

    @Test
    fun `MethodTrackingExtension handles edge case parameter values`() {
        val extension = AnalyticsPluginExtension()

        extension.methodTracking {
            maxParametersPerMethod = 0 // Edge case: no parameters
            excludeMethods = emptySet() // Edge case: empty set
            includeClassPatterns = setOf("") // Edge case: empty pattern
        }

        assertEquals(0, extension.methodTracking.maxParametersPerMethod)
        assertTrue(extension.methodTracking.excludeMethods.isEmpty())
        assertEquals(setOf(""), extension.methodTracking.includeClassPatterns)
    }
}
