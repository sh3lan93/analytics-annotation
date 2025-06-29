package com.shalan.analytics.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class AnalyticsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension for configuration
        val extension =
            project.extensions.create(
                "analytics",
                AnalyticsPluginExtension::class.java,
            )

        // Only apply to Android projects
        project.plugins.withId("com.android.application") {
            configureAndroidProject(project, extension)
        }

        project.plugins.withId("com.android.library") {
            configureAndroidProject(project, extension)
        }
    }

    private fun configureAndroidProject(
        project: Project,
        extension: AnalyticsPluginExtension,
    ) {
        project.logger.info("Analytics Plugin: Configuring Android project ${project.name}")

        // TODO: Register transform for bytecode manipulation
        // This will be implemented in the next patch

        project.afterEvaluate {
            if (extension.enabled) {
                project.logger.info("Analytics Plugin: Enabled for project ${project.name}")
                if (extension.debugMode) {
                    project.logger.info("Analytics Plugin: Debug mode enabled")
                }
            } else {
                project.logger.info("Analytics Plugin: Disabled for project ${project.name}")
            }
        }
    }
}
