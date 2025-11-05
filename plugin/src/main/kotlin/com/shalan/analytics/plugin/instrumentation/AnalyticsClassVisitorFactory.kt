package com.shalan.analytics.plugin.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.shalan.analytics.plugin.utils.ErrorReporter
import com.shalan.analytics.plugin.utils.PluginLogger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * AGP instrumentation factory that creates ASM class visitors for automatic screen tracking.
 *
 * **Refactored for improved maintainability:**
 * - Extracted constants into AnalyticsConstants object
 * - Separated class type detection into ClassTypeDetector
 * - Optimized logging with TrackingLogger (lazy evaluation)
 * - Created MethodInstrumentationStrategy for method selection
 * - Extracted LifecycleInstrumentingMethodVisitor into separate file
 * - Created AnnotationMetadata and AnnotationExtractor for type-safe annotation handling
 *
 * ## Transformation Strategy
 *
 * The plugin uses deferred transformation:
 * 1. First pass: Scan class for annotations and collect metadata
 * 2. Second pass: Inject tracking code based on collected metadata
 *
 * This approach allows the plugin to make informed decisions about what bytecode to generate
 * based on the complete class structure.
 *
 * ## Supported Annotations
 *
 * - **@TrackScreen**: For Activities and Fragments - injects tracking in onCreate/onViewCreated
 * - **@Trackable**: For classes with @Track method annotations - enables method-level tracking
 */
abstract class AnalyticsClassVisitorFactory :
    AsmClassVisitorFactory<AnalyticsClassVisitorFactory.Parameters> {
    /**
     * Configuration parameters for the Analytics Annotation Plugin.
     *
     * These parameters are configured via the Gradle plugin extension and control
     * which classes and methods get instrumented during the build.
     */
    interface Parameters : InstrumentationParameters {
        /** Whether the plugin is enabled. When false, no instrumentation occurs. */
        @get:Input
        val enabled: Property<Boolean>

        /** Whether to enable debug logging during bytecode transformation. */
        @get:Input
        @get:Optional
        val debugMode: Property<Boolean>

        /** List of package prefixes to include for instrumentation (e.g., ["com.myapp."]). */
        @get:Input
        @get:Optional
        val includePackages: ListProperty<String>

        /** List of package prefixes to exclude from instrumentation (e.g., ["com.myapp.test."]). */
        @get:Input
        @get:Optional
        val excludePackages: ListProperty<String>

        /** Whether to enable method-level tracking with @Track annotation. */
        @get:Input
        @get:Optional
        val methodTrackingEnabled: Property<Boolean>

        /** Maximum number of parameters to capture per @Track method (default: 10). */
        @get:Input
        @get:Optional
        val maxParametersPerMethod: Property<Int>

        /** List of method names to exclude from @Track instrumentation. */
        @get:Input
        @get:Optional
        val excludeMethods: ListProperty<String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor {
        val params = parameters.get()
        PluginLogger.setDebugMode(params.debugMode.getOrElse(false))

        if (!params.enabled.getOrElse(true)) {
            return nextClassVisitor
        }

        return AnalyticsClassVisitor(
            api = instrumentationContext.apiVersion.get(),
            nextClassVisitor = nextClassVisitor,
            parameters = params,
            className = classContext.currentClassData.className,
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val params = parameters.get()
        PluginLogger.setDebugMode(params.debugMode.getOrElse(false))

        if (!params.enabled.getOrElse(true)) {
            return false
        }

        val className = classData.className.replace('/', '.')

        // Skip system classes
        if (AnalyticsConstants.SystemPackages.ALL.any { prefix -> className.startsWith(prefix) }) {
            return false
        }

        // Check include packages
        val includePackages = params.includePackages.getOrElse(emptyList())
        if (includePackages.isNotEmpty()) {
            if (!includePackages.any { pkg -> className.startsWith(pkg) }) {
                return false
            }
        }

        // Check exclude packages
        val excludePackages = params.excludePackages.getOrElse(emptyList())
        if (excludePackages.isNotEmpty()) {
            if (excludePackages.any { pkg -> className.startsWith(pkg) }) {
                return false
            }
        }

        return true
    }

    /**
     * Refactored AnalyticsClassVisitor with better separation of concerns.
     *
     * Uses helper classes for:
     * - ClassTypeDetector: Determining if class is Activity/Fragment
     * - MethodInstrumentationStrategy: Deciding which methods to instrument
     * - AnnotationExtractor: Extracting annotation parameters
     * - TrackingLogger: Optimized logging with lazy evaluation
     */
    private inner class AnalyticsClassVisitor(
        api: Int,
        nextClassVisitor: ClassVisitor,
        private val parameters: Parameters,
        private val className: String,
    ) : ClassVisitor(api, nextClassVisitor) {
        private val logger = TrackingLogger.forClass(className, parameters.debugMode.getOrElse(false))
        private val classTypeDetector = ClassTypeDetector(parameters.debugMode.getOrElse(false))
        private val instrumentationStrategy = MethodInstrumentationStrategy()
        private val annotationExtractor = AnnotationExtractor(logger)

        private var classType: ClassTypeDetector.ClassType = ClassTypeDetector.ClassType.Other
        private var annotationMetadata: AnnotationMetadata = AnnotationMetadata.empty()
        private var internalClassName: String = ""
        private val methodsToInstrument = mutableListOf<String>()

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?,
        ) {
            super.visit(version, access, name, signature, superName, interfaces)

            name?.let { internalClassName = it }
            classType = classTypeDetector.detectClassType(superName)

            logger.debug { "Processing class: ${name?.replace('/', '.')} (type: $classType)" }
        }

        override fun visitAnnotation(
            descriptor: String?,
            visible: Boolean,
        ): AnnotationVisitor? {
            return when (descriptor) {
                AnalyticsConstants.Annotations.TRACK_SCREEN -> {
                    logger.debug { "Found @TrackScreen annotation" }
                    annotationExtractor.createTrackScreenVisitor(super.visitAnnotation(descriptor, visible))
                }

                AnalyticsConstants.Annotations.TRACKABLE -> {
                    logger.debug { "Found @Trackable annotation" }
                    super.visitAnnotation(descriptor, visible)
                }

                else -> super.visitAnnotation(descriptor, visible)
            }
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)

            // Check if this method should be instrumented
            val decision =
                instrumentationStrategy.decide(
                    methodName = name,
                    descriptor = descriptor,
                    isActivity = classType.isActivity,
                    isFragment = classType.isFragment,
                )

            if (decision is MethodInstrumentationStrategy.InstrumentationDecision.ShouldInstrument) {
                methodsToInstrument.add(name ?: "")
                logger.debug { "Will instrument method: $name (reason: ${decision.reason})" }

                return LifecycleInstrumentingMethodVisitor(
                    api = api,
                    delegate = methodVisitor,
                    methodName = name ?: "unknown",
                    internalClassName = internalClassName,
                    logger = logger,
                )
            }

            // Always wrap with TrackMethodVisitor for @Track support
            val methodTrackingEnabled = parameters.methodTrackingEnabled.getOrElse(true)
            val maxParametersPerMethod = parameters.maxParametersPerMethod.getOrElse(10)
            val excludeMethods = parameters.excludeMethods.getOrElse(emptyList()).toSet()

            return TrackMethodVisitor(
                api,
                methodVisitor,
                name ?: "unknown",
                descriptor ?: "",
                className,
                access,
                methodTrackingEnabled = methodTrackingEnabled,
                maxParametersPerMethod = maxParametersPerMethod,
                excludeMethods = excludeMethods,
            )
        }

        override fun visitEnd() {
            try {
                if (methodsToInstrument.isNotEmpty()) {
                    logger.debug { "Processing ${methodsToInstrument.size} instrumented methods" }
                    injectTrackingMethod(annotationMetadata)
                } else {
                    logger.debug { "No methods to instrument" }
                }
            } catch (e: Exception) {
                logger.error("Error processing class", e)
                ErrorReporter.reportError(
                    className = className,
                    errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
                    message = "Failed to process class",
                    throwable = e,
                )
            }

            super.visitEnd()
        }

        /**
         * Injects the __injectAnalyticsTracking() helper method.
         */
        private fun injectTrackingMethod(metadata: AnnotationMetadata) {
            val methodVisitor =
                cv.visitMethod(
                    Opcodes.ACC_PRIVATE,
                    AnalyticsConstants.InjectedMethod.NAME,
                    AnalyticsConstants.InjectedMethod.DESCRIPTOR,
                    null,
                    null,
                )

            methodVisitor.visitCode()

            // Load 'this' reference
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)

            // Push parameters
            methodVisitor.visitLdcInsn(metadata.screenName ?: extractScreenNameFromClassName(className))
            methodVisitor.visitLdcInsn(metadata.screenClass ?: extractSimpleClassName(className))

            // Call TrackScreenHelper.trackScreen()
            methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                AnalyticsConstants.TrackScreenHelper.CLASS,
                AnalyticsConstants.TrackScreenHelper.METHOD,
                AnalyticsConstants.TrackScreenHelper.DESCRIPTOR,
                false,
            )

            methodVisitor.visitInsn(Opcodes.RETURN)
            methodVisitor.visitMaxs(AnalyticsConstants.AsmConfig.STACK_SIZE, AnalyticsConstants.AsmConfig.LOCALS_SIZE)
            methodVisitor.visitEnd()

            logger.debug { "Injected tracking method" }
        }

        private fun extractScreenNameFromClassName(className: String): String {
            return className.substringAfterLast('.')
                .removeSuffix("Activity")
                .removeSuffix("Fragment")
                .removeSuffix("Screen")
        }

        private fun extractSimpleClassName(className: String): String {
            return className.substringAfterLast('.')
        }
    }
}
