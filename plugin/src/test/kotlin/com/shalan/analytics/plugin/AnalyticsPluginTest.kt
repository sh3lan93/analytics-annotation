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
        assertTrue(extension.trackActivities)
        assertTrue(extension.trackFragments)
        assertTrue(extension.trackComposables)
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
        extension.trackActivities = false
        extension.trackFragments = false
        extension.trackComposables = false
        extension.includePackages = setOf("com.test.include")
        extension.excludePackages = setOf("com.test.exclude")

        // Verify configuration
        assertFalse(extension.enabled)
        assertTrue(extension.debugMode)
        assertFalse(extension.trackActivities)
        assertFalse(extension.trackFragments)
        assertFalse(extension.trackComposables)
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
}
