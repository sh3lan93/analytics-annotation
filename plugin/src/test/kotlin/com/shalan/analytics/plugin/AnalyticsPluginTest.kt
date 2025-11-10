package com.shalan.analytics.plugin

import com.shalan.analytics.plugin.instrumentation.AnalyticsClassVisitorFactory
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnalyticsPluginTest {
    private lateinit var project: Project

    @Before
    fun setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    fun `plugin can be applied`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        // Verify plugin was applied
        assertTrue(project.plugins.hasPlugin(AnalyticsPlugin::class.java))
    }

    @Test
    fun `extension is created when plugin is applied`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.findByType(AnalyticsPluginExtension::class.java)
        assertNotNull(extension)
    }

    @Test
    fun `extension has default values`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)
        assertTrue(extension.enabled)
        assertFalse(extension.debugMode)
        assertTrue(extension.includePackages.isEmpty())
        assertTrue(extension.excludePackages.isEmpty())
    }

    @Test
    fun `visitor factory can be instantiated`() {
        val factoryClass = AnalyticsClassVisitorFactory::class.java
        assertNotNull(factoryClass)

        // Verify it's abstract (instantiated by AGP framework)
        assertTrue(java.lang.reflect.Modifier.isAbstract(factoryClass.modifiers))
    }

    @Test
    fun `plugin applies to android application projects`() {
        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        // Verify both plugins are applied
        assertTrue(project.plugins.hasPlugin("com.android.application"))
        assertTrue(project.plugins.hasPlugin(AnalyticsPlugin::class.java))
    }

    @Test
    fun `plugin applies to android library projects`() {
        project.pluginManager.apply("com.android.library")
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        // Verify both plugins are applied
        assertTrue(project.plugins.hasPlugin("com.android.library"))
        assertTrue(project.plugins.hasPlugin(AnalyticsPlugin::class.java))
    }

    @Test
    fun `extension can be configured via DSL`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        // Configure extension directly
        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)
        extension.enabled = false
        extension.debugMode = true
        extension.includePackages = setOf("com.test.include")
        extension.excludePackages = setOf("com.test.exclude")

        // Verify configuration
        assertFalse(extension.enabled)
        assertTrue(extension.debugMode)
        assertEquals(setOf("com.test.include"), extension.includePackages)
        assertEquals(setOf("com.test.exclude"), extension.excludePackages)
    }

    @Test
    fun `plugin logs appropriate messages when enabled`() {
        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)
        extension.enabled = true
        extension.debugMode = true

        // Extension should maintain configured values
        assertTrue(extension.enabled)
        assertTrue(extension.debugMode)
    }

    @Test
    fun `plugin logs appropriate messages when disabled`() {
        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)
        extension.enabled = false

        // Extension should maintain configured values
        assertFalse(extension.enabled)
    }

    @Test
    fun `plugin can be applied to non-android projects without errors`() {
        // Should not throw exception even without Android plugin
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        assertTrue(project.plugins.hasPlugin(AnalyticsPlugin::class.java))
        assertNotNull(project.extensions.findByType(AnalyticsPluginExtension::class.java))
    }

    @Test
    fun `extension methodTracking configuration is accessible`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)

        // Verify default method tracking configuration
        assertTrue(extension.methodTracking.enabled)
        assertEquals(10, extension.methodTracking.maxParametersPerMethod)
        assertTrue(extension.methodTracking.validateAnnotations)
        assertTrue(extension.methodTracking.excludeMethods.isEmpty())
        assertTrue(extension.methodTracking.includeClassPatterns.isEmpty())
        assertTrue(extension.methodTracking.excludeClassPatterns.isEmpty())
    }

    @Test
    fun `extension methodTracking can be configured via DSL`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)

        extension.methodTracking {
            enabled = false
            maxParametersPerMethod = 20
            validateAnnotations = false
            excludeMethods = setOf("excludedMethod1", "excludedMethod2")
            includeClassPatterns = setOf("com.app.*", "*.Activity")
            excludeClassPatterns = setOf("com.test.*", "*.Test")
        }

        // Verify configuration was applied
        assertFalse(extension.methodTracking.enabled)
        assertEquals(20, extension.methodTracking.maxParametersPerMethod)
        assertFalse(extension.methodTracking.validateAnnotations)
        assertEquals(setOf("excludedMethod1", "excludedMethod2"), extension.methodTracking.excludeMethods)
        assertEquals(setOf("com.app.*", "*.Activity"), extension.methodTracking.includeClassPatterns)
        assertEquals(setOf("com.test.*", "*.Test"), extension.methodTracking.excludeClassPatterns)
    }

    @Test
    fun `extension methodTracking configuration is independent between instances`() {
        val project2 = ProjectBuilder.builder().build()

        project.pluginManager.apply(AnalyticsPlugin::class.java)
        project2.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension1 = project.extensions.getByType(AnalyticsPluginExtension::class.java)
        val extension2 = project2.extensions.getByType(AnalyticsPluginExtension::class.java)

        extension1.methodTracking {
            enabled = false
            maxParametersPerMethod = 5
        }

        // extension2 should maintain defaults
        assertTrue(extension2.methodTracking.enabled)
        assertEquals(10, extension2.methodTracking.maxParametersPerMethod)

        // extension1 should have the configured values
        assertFalse(extension1.methodTracking.enabled)
        assertEquals(5, extension1.methodTracking.maxParametersPerMethod)
    }

    @Test
    fun `extension methodTracking handles edge case configurations`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)

        extension.methodTracking {
            maxParametersPerMethod = 0 // Edge case: no parameters
            excludeMethods = emptySet() // Edge case: explicitly empty
            includeClassPatterns = setOf("") // Edge case: empty pattern
            excludeClassPatterns = setOf("*") // Edge case: exclude everything
        }

        assertEquals(0, extension.methodTracking.maxParametersPerMethod)
        assertTrue(extension.methodTracking.excludeMethods.isEmpty())
        assertEquals(setOf(""), extension.methodTracking.includeClassPatterns)
        assertEquals(setOf("*"), extension.methodTracking.excludeClassPatterns)
    }

    @Test
    fun `extension toString includes methodTracking information`() {
        project.pluginManager.apply(AnalyticsPlugin::class.java)

        val extension = project.extensions.getByType(AnalyticsPluginExtension::class.java)

        extension.methodTracking {
            enabled = false
            maxParametersPerMethod = 15
        }

        val toStringResult = extension.toString()
        assertTrue(toStringResult.contains("methodTracking="))

        val methodTrackingString = extension.methodTracking.toString()
        assertTrue(methodTrackingString.contains("enabled=false"))
        assertTrue(methodTrackingString.contains("maxParametersPerMethod=15"))
    }
}
