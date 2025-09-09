package com.shalan.analytics.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.shalan.analytics.plugin.instrumentation.AnalyticsClassVisitorFactory
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

        // Register modern instrumentation using androidComponents
        registerModernInstrumentation(project, extension)

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

    private fun registerModernInstrumentation(
        project: Project,
        extension: AnalyticsPluginExtension,
    ) {
        try {
            val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                if (extension.enabled) {
                    project.logger.info("Analytics Plugin: Registering instrumentation for variant ${variant.name}")

                    variant.instrumentation.transformClassesWith(
                        AnalyticsClassVisitorFactory::class.java,
                        InstrumentationScope.ALL,
                    ) { params ->
                        // Basic configuration
                        params.enabled.set(extension.enabled)
                        params.debugMode.set(extension.debugMode)
                        params.trackActivities.set(extension.trackActivities)
                        params.trackFragments.set(extension.trackFragments)
                        params.trackComposables.set(extension.trackComposables)
                        params.includePackages.set(extension.includePackages.toList())
                        params.excludePackages.set(extension.excludePackages.toList())

                        // Method tracking configuration
                        params.methodTrackingEnabled.set(extension.methodTracking.enabled)
                        params.maxParametersPerMethod.set(extension.methodTracking.maxParametersPerMethod)
                        params.validateAnnotations.set(extension.methodTracking.validateAnnotations)
                        params.excludeMethods.set(extension.methodTracking.excludeMethods.toList())
                        params.includeClassPatterns.set(extension.methodTracking.includeClassPatterns.toList())
                        params.excludeClassPatterns.set(extension.methodTracking.excludeClassPatterns.toList())
                    }

                    // Set frames computation mode for better performance
                    variant.instrumentation.setAsmFramesComputationMode(
                        FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
                    )

                    project.logger.info("Analytics Plugin: Modern instrumentation registered successfully for ${variant.name}")
                } else {
                    project.logger.info("Analytics Plugin: Skipping instrumentation for ${variant.name} (disabled)")
                }
            }
        } catch (e: Exception) {
            project.logger.error("Analytics Plugin: Failed to register modern instrumentation", e)
        }
    }
}
