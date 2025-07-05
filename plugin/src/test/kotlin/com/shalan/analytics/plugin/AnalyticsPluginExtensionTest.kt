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
        assertTrue(extension.trackComposables)
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
        extension.trackComposables = false
        extension.includePackages = setOf("com.test")
        extension.excludePackages = setOf("com.exclude")

        assertFalse(extension.enabled)
        assertTrue(extension.debugMode)
        assertFalse(extension.trackActivities)
        assertFalse(extension.trackFragments)
        assertFalse(extension.trackComposables)
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
        extension.trackComposables = false
        extension.includePackages = setOf("com.include")
        extension.excludePackages = setOf("com.exclude")

        val toString = extension.toString()
        assertNotNull(toString)
        assertTrue(toString.contains("enabled=false"))
        assertTrue(toString.contains("debugMode=true"))
        assertTrue(toString.contains("trackActivities=false"))
        assertTrue(toString.contains("trackFragments=false"))
        assertTrue(toString.contains("trackComposables=false"))
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
        assertTrue(extension.trackComposables) // Should remain true
        
        extension.trackFragments = false 
        assertFalse(extension.trackActivities) // Should remain false
        assertTrue(extension.trackComposables) // Should remain true
        
        extension.trackComposables = false
        assertFalse(extension.trackActivities) // Should remain false
        assertFalse(extension.trackFragments) // Should remain false
    }
}
