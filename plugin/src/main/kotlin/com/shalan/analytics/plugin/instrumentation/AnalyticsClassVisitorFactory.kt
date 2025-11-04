package com.shalan.analytics.plugin.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.shalan.analytics.plugin.utils.ErrorReporter
import com.shalan.analytics.plugin.utils.PluginLogger
import com.shalan.analytics.plugin.utils.TrackingAnnotationInfo
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
 * This factory is the entry point for the Analytics Annotation Plugin's bytecode transformation.
 * It creates [AnalyticsClassVisitor] instances that scan for @TrackScreen and @Trackable
 * annotations, then injects tracking code at appropriate lifecycle points.
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
 *
 * @see AnalyticsClassVisitor
 * @see com.shalan.analytics.annotation.TrackScreen
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

        /** Whether to track Activities with @TrackScreen annotation. */
        @get:Input
        @get:Optional
        val trackActivities: Property<Boolean>

        /** Whether to track Fragments with @TrackScreen annotation. */
        @get:Input
        @get:Optional
        val trackFragments: Property<Boolean>

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

        /** Whether to validate annotation usage at build time. */
        @get:Input
        @get:Optional
        val validateAnnotations: Property<Boolean>

        /** List of method names to exclude from @Track instrumentation. */
        @get:Input
        @get:Optional
        val excludeMethods: ListProperty<String>

        /** List of regex patterns for class names to include for instrumentation. */
        @get:Input
        @get:Optional
        val includeClassPatterns: ListProperty<String>

        /** List of regex patterns for class names to exclude from instrumentation. */
        @get:Input
        @get:Optional
        val excludeClassPatterns: ListProperty<String>
    }

    /**
     * Creates an ASM ClassVisitor for transforming a single class file.
     *
     * This method is called by AGP for each class that passes the [isInstrumentable] check.
     * It creates an [AnalyticsClassVisitor] that will scan for tracking annotations and
     * inject appropriate bytecode.
     *
     * @param classContext Context information about the class being transformed
     * @param nextClassVisitor The next visitor in the transformation chain
     * @return An [AnalyticsClassVisitor] if the plugin is enabled, otherwise the original visitor
     */
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor {
        val params = parameters.get()

        // Initialize PluginLogger with debug mode setting
        PluginLogger.setDebugMode(params.debugMode.getOrElse(false))

        // If plugin is disabled, just pass through
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

    /**
     * Determines whether a class should be instrumented by this plugin.
     *
     * This method performs early filtering to skip classes that definitely don't need
     * instrumentation, improving build performance by reducing unnecessary class visits.
     *
     * ## Filtering Logic
     *
     * Classes are excluded if they:
     * - Are part of the Android SDK (android.*, androidx.*)
     * - Are part of the JDK (java.*)
     * - Are part of Kotlin stdlib (kotlin.*)
     * - Don't match includePackages patterns (if specified)
     * - Match excludePackages patterns
     *
     * @param classData Metadata about the class being considered for instrumentation
     * @return true if the class should be visited for potential transformation
     */
    override fun isInstrumentable(classData: ClassData): Boolean {
        val params = parameters.get()

        // Initialize PluginLogger with debug mode setting
        PluginLogger.setDebugMode(params.debugMode.getOrElse(false))

        // If plugin is disabled, don't instrument anything
        if (!params.enabled.getOrElse(true)) {
            PluginLogger.debug("Analytics Annotation Plugin is disabled")
            return false
        }

        val className = classData.className.replace('/', '.')
        PluginLogger.debug("Checking if class $className is instrumentable")

        // Skip system classes
        if (className.startsWith("android.") ||
            className.startsWith("androidx.") ||
            className.startsWith("java.") ||
            className.startsWith("kotlin.")
        ) {
            PluginLogger.debug("Skipping system class $className")
            return false
        }

        // Check include packages
        val includePackages = params.includePackages.getOrElse(emptyList())
        if (includePackages.isNotEmpty()) {
            val isIncluded =
                includePackages.any { packageName ->
                    className.startsWith(packageName)
                }
            if (!isIncluded) return false
        }

        // Check exclude packages
        val excludePackages = params.excludePackages.getOrElse(emptyList())
        if (excludePackages.isNotEmpty()) {
            val isExcluded =
                excludePackages.any { packageName ->
                    className.startsWith(packageName)
                }
            if (isExcluded) return false
        }

        PluginLogger.debug("Class $className is instrumentable")
        return true
    }

    /**
     * ASM ClassVisitor that performs the actual bytecode transformation for analytics tracking.
     *
     * This visitor implements a two-pass strategy:
     *
     * **Pass 1 (Scanning):**
     * - Visits class and method annotations to detect @TrackScreen, @Trackable
     * - Collects metadata about screen names, class types (Activity/Fragment)
     * - Identifies lifecycle methods that need instrumentation
     *
     * **Pass 2 (Transformation):**
     * - In [visitEnd], generates helper methods and injects tracking calls
     * - For Activities/Fragments: Injects `__injectAnalyticsTracking()` method and calls it after super lifecycle calls
     *
     * @property parameters Plugin configuration parameters
     * @property className The fully qualified name of the class being visited
     */
    private inner class AnalyticsClassVisitor(
        api: Int,
        nextClassVisitor: ClassVisitor,
        private val parameters: Parameters,
        private val className: String,
    ) : ClassVisitor(api, nextClassVisitor) {
        private var annotationInfo: TrackingAnnotationInfo? = null
        private var currentAnnotationInfo: TrackingAnnotationInfo? = null
        private var hasTrackScreenAnnotation = false
        private var hasTrackableAnnotation = false
        private var screenName: String? = null
        private var screenClass: String? = null // Used in TrackScreenAnnotationVisitor and createAnnotationInfo
        private val annotationParameters: MutableMap<String, Any> = mutableMapOf() // Used in annotation visitors
        private var internalClassName: String = ""
        private var isActivity: Boolean = false
        private var isFragment: Boolean = false
        private val methodsToInstrument = mutableListOf<String>()

        /**
         * Visits the class header to determine class type (Activity/Fragment/other).
         *
         * This is called first when processing a class file. It examines the superclass
         * to determine if this is an Activity or Fragment, which affects how tracking
         * will be injected.
         */
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?,
        ) {
            super.visit(version, access, name, signature, superName, interfaces)

            // Store class information for transformation decisions
            name?.let {
                internalClassName = it
                val dotClassName = it.replace('/', '.')
                logDebug("AnalyticsClassVisitor: Processing class $dotClassName")
            }

            // Determine class type for proper transformation
            isActivity = isActivityClass(superName)
            isFragment = isFragmentClass(superName)

            name?.let { internalName ->
                val dotClassName = internalName.replace('/', '.')
                logDebug(
                    "AnalyticsClassVisitor: Class $dotClassName - isActivity: $isActivity, isFragment: $isFragment, superName: $superName",
                )
                logDebug("AnalyticsClassVisitor: Raw superName: '$superName'")
            }
        }

        /**
         * Visits class-level annotations to detect @TrackScreen or @Trackable.
         *
         * When a tracking annotation is found, returns a specialized AnnotationVisitor that extracts
         * the annotation parameters (screenName, screenClass, etc.) for later use during transformation.
         *
         * @return A specialized AnnotationVisitor for tracking annotations, or the default visitor otherwise
         */
        override fun visitAnnotation(
            descriptor: String?,
            visible: Boolean,
        ): AnnotationVisitor? {
            when (descriptor) {
                "Lcom/shalan/analytics/annotation/TrackScreen;" -> {
                    hasTrackScreenAnnotation = true
                    logDebug("AnalyticsClassVisitor: Found @TrackScreen annotation on class $className")
                    return TrackScreenAnnotationVisitor(super.visitAnnotation(descriptor, visible))
                }

                "Lcom/shalan/analytics/annotation/Trackable;" -> {
                    hasTrackableAnnotation = true
                    logDebug("AnalyticsClassVisitor: Found @Trackable annotation on class $className")
                    return super.visitAnnotation(descriptor, visible)
                }
            }
            return super.visitAnnotation(descriptor, visible)
        }

        /**
         * Visits each method to determine if it needs instrumentation.
         *
         * This method creates specialized MethodVisitors for different method types:
         * - **Lifecycle methods** (onCreate/onViewCreated): Wrapped in [LifecycleInstrumentingMethodVisitor]
         * - **Other methods**: Wrapped in TrackMethodVisitor for @Track annotation support
         *
         * @return A specialized MethodVisitor that may inject tracking code
         */
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            logDebug("AnalyticsClassVisitor: Visiting method '$name' with descriptor '$descriptor' in class $className")
            // Check if this is a method we should instrument
            if (shouldCollectMethod(name, descriptor)) {
                methodsToInstrument.add(name ?: "")
                logDebug("AnalyticsClassVisitor: Collected method $name for potential instrumentation")
            }

            val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)

            // If this is a lifecycle method we want to instrument, wrap with our instrumenting visitor
            val isActivityOnCreate =
                name == "onCreate" && descriptor == "(Landroid/os/Bundle;)V" && isActivity
            val isFragmentOnViewCreated =
                name == "onViewCreated" && descriptor == "(Landroid/view/View;Landroid/os/Bundle;)V" && isFragment

            if (isActivityOnCreate || isFragmentOnViewCreated) {
                logDebug("AnalyticsClassVisitor: Wrapping $name method for potential instrumentation")
                return LifecycleInstrumentingMethodVisitor(api, methodVisitor, name ?: "unknown")
            }

            // Check if this method might have @Track annotation and wrap with TrackMethodVisitor
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

        /**
         * Called when class visiting is complete - performs deferred bytecode transformation.
         *
         * This is where the "second pass" happens. After collecting all annotation metadata
         * during the class scan, this method generates and injects the necessary tracking code:
         *
         * - For @TrackScreen: Generates `__injectAnalyticsTracking()` method
         * - For @Trackable: No class-level transformation needed (handled at method level)
         */
        override fun visitEnd() {
            try {
                // Check if we found any tracking annotations OR @Trackable annotation for method-level tracking
                val hasClassLevelAnnotations = hasTrackScreenAnnotation
                val hasTrackableMethods = hasTrackableAnnotation

                if (hasClassLevelAnnotations && methodsToInstrument.isNotEmpty()) {
                    // Process screen-level tracking annotations (@TrackScreen)
                    annotationInfo = createAnnotationInfo()

                    if (annotationInfo != null) {
                        logDebug("AnalyticsClassVisitor: Processing class with screen tracking annotations: $className")
                        logDebug("AnalyticsClassVisitor: Found ${methodsToInstrument.size} methods to instrument")

                        // Perform deferred transformation now that we have all annotation info
                        performDeferredTransformation(annotationInfo!!)
                    }
                } else if (hasTrackableMethods) {
                    // Process classes with @Trackable annotation for method-level tracking
                    logDebug("AnalyticsClassVisitor: Processing @Trackable class for method tracking: $className")
                    logDebug("AnalyticsClassVisitor: @Trackable class will have @Track methods processed by TrackMethodVisitor")

                    // For @Trackable classes, we don't need to do deferred transformation
                    // The TrackMethodVisitor handles @Track annotations individually
                } else {
                    logDebug("AnalyticsClassVisitor: No tracking annotations found in $className")
                }
            } catch (e: Exception) {
                logError("AnalyticsClassVisitor: Error processing class $className", e)
                ErrorReporter.reportError(
                    className = className,
                    errorType = ErrorReporter.ErrorType.TRANSFORMATION_ERROR,
                    message = "Failed to process class with modern instrumentation",
                    throwable = e,
                )
            }

            super.visitEnd()
        }

        /**
         * Creates a [TrackingAnnotationInfo] object from collected annotation metadata.
         *
         * This method is called during [visitEnd] to package all the annotation parameters
         * collected during the scanning phase into a data object for use during transformation.
         *
         * @return TrackingAnnotationInfo if a tracking annotation was found, null otherwise
         */
        private fun createAnnotationInfo(): TrackingAnnotationInfo? {
            return when {
                hasTrackScreenAnnotation ->
                    TrackingAnnotationInfo(
                        screenName = screenName ?: extractScreenNameFromClassName(className),
                        screenClass = screenClass?.takeIf { it.isNotEmpty() } ?: extractSimpleClassName(className),
                        annotationType = TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN,
                        className = className,
                    )

                else -> null
            }
        }

        /**
         * Executes the deferred bytecode transformation based on annotation type.
         *
         * Called from [visitEnd] after all class scanning is complete. Decides what
         * bytecode to generate based on the annotation type.
         *
         * @param annotationInfo Metadata about the tracking annotation found on this class
         */
        private fun performDeferredTransformation(annotationInfo: TrackingAnnotationInfo) {
            logDebug("AnalyticsClassVisitor: Performing deferred transformation for ${annotationInfo.annotationType}")

            // Store the annotation info for use by lifecycle visitors
            currentAnnotationInfo = annotationInfo

            when (annotationInfo.annotationType) {
                TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN -> {
                    // For Activities and Fragments, inject into lifecycle methods
                    injectTrackingCallIntoMethod(annotationInfo)
                    logDebug("AnalyticsClassVisitor: Injected tracking call for screen: ${annotationInfo.screenName}")
                }
            }
        }

        private fun injectTrackingCallIntoMethod(annotationInfo: TrackingAnnotationInfo) {
            // The actual injection happens in OnCreateInstrumentingMethodVisitor
            // We just need to make sure our tracking method exists
            injectTrackingMethod(annotationInfo)
        }

        /**
         * MethodVisitor that injects tracking calls after super.onCreate() or super.onViewCreated().
         *
         * This visitor monitors method instructions to detect super calls to lifecycle methods.
         * When it finds `super.onCreate()` or `super.onViewCreated()`, it injects a call to
         * the `__injectAnalyticsTracking()` method immediately after the super call.
         *
         * This ensures tracking happens at the right point in the lifecycle - after the
         * super implementation has run but before any user code executes.
         */
        private inner class LifecycleInstrumentingMethodVisitor(
            api: Int,
            private val delegate: MethodVisitor,
            private val methodName: String,
        ) : MethodVisitor(api, delegate) {
            private var hasSuperCall = false

            override fun visitMethodInsn(
                opcode: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean,
            ) {
                // First, call the original method instruction
                delegate.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

                // Check for super calls that we want to inject tracking after
                val shouldInject =
                    when {
                        // Activity onCreate: inject after super.onCreate()
                        methodName == "onCreate" &&
                            opcode == Opcodes.INVOKESPECIAL &&
                            name == "onCreate" &&
                            descriptor == "(Landroid/os/Bundle;)V" &&
                            !hasSuperCall -> {
                            logDebug("AnalyticsClassVisitor: Detected Activity onCreate super call - shouldInject=true")
                            true
                        }

                        // Fragment onViewCreated: inject after super.onViewCreated()
                        methodName == "onViewCreated" &&
                            opcode == Opcodes.INVOKESPECIAL &&
                            name == "onViewCreated" &&
                            descriptor == "(Landroid/view/View;Landroid/os/Bundle;)V" &&
                            !hasSuperCall -> {
                            logDebug("AnalyticsClassVisitor: Detected Fragment onViewCreated super call - shouldInject=true")
                            true
                        }

                        else -> false
                    }

                if (shouldInject && hasTrackScreenAnnotation && currentAnnotationInfo != null) {
                    hasSuperCall = true
                    logDebug("AnalyticsClassVisitor: Injecting tracking call after super.$name in $className")

                    // Call the injected __injectAnalyticsTracking method
                    delegate.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
                    delegate.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        internalClassName,
                        "__injectAnalyticsTracking",
                        "()V",
                        false,
                    )
                }
            }

            override fun visitInsn(opcode: Int) {
                // Call the original instruction
                delegate.visitInsn(opcode)
            }
        }

        /**
         * Generates the `__injectAnalyticsTracking()` helper method for Activities/Fragments.
         *
         * This method generates bytecode that creates a private method which calls
         * `TrackScreenHelper.trackScreen(this, screenName, screenClass)`. This approach
         * is much simpler than generating complex inline bytecode for parameter collection.
         *
         * The generated method is called from onCreate() or onViewCreated() after the super call.
         *
         * @param annotationInfo Metadata about the screen to track
         */
        private fun injectTrackingMethod(annotationInfo: TrackingAnnotationInfo) {
            // Inject a simple method that calls TrackScreenHelper.trackScreen()
            // This replaces complex ASM bytecode with a simple static method call
            val methodVisitor =
                cv.visitMethod(
                    Opcodes.ACC_PRIVATE,
                    "__injectAnalyticsTracking",
                    "()V",
                    null,
                    null,
                )

            methodVisitor.visitCode()

            // Generate: TrackScreenHelper.trackScreen(this, screenName, screenClass)
            // This is much simpler than the previous approach which generated complex bytecode
            // for parameter collection and ScreenTracking calls

            // Load 'this' as first parameter (the Activity/Fragment instance)
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)

            // Push screenName parameter
            methodVisitor.visitLdcInsn(annotationInfo.screenName)

            // Push screenClass parameter
            methodVisitor.visitLdcInsn(annotationInfo.screenClass ?: annotationInfo.className)

            // Call TrackScreenHelper.trackScreen(Object, String, String)
            methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/shalan/analytics/core/TrackScreenHelper",
                "trackScreen",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V",
                false,
            )

            methodVisitor.visitInsn(Opcodes.RETURN)
            methodVisitor.visitMaxs(3, 1) // Reduced stack size - much simpler now
            methodVisitor.visitEnd()

            logDebug("AnalyticsClassVisitor: Successfully injected tracking method")
        }

        /**
         * Determines if a method should be collected for potential instrumentation.
         *
         * This is called during the first pass (method scanning) to identify methods
         * that may need bytecode injection. Collected methods are stored in [methodsToInstrument].
         *
         * @param methodName The name of the method being visited
         * @param descriptor The JVM method descriptor (parameter and return types)
         * @return true if this method should be collected for instrumentation
         */
        private fun shouldCollectMethod(
            methodName: String?,
            descriptor: String?,
        ): Boolean {
            // Collect methods that we might want to instrument later
            return when {
                isActivity && methodName == "onCreate" && descriptor == "(Landroid/os/Bundle;)V" -> true
                isFragment && methodName == "onViewCreated" && descriptor == "(Landroid/view/View;Landroid/os/Bundle;)V" -> true
                else -> false
            }
        }

        /**
         * Checks if the given class is an Android Activity.
         *
         * @param superName The internal name of the superclass to check
         * @return True if the class extends from a known Activity class, false otherwise
         */
        private fun isActivityClass(superName: String?): Boolean {
            return when (superName) {
                "android/app/Activity",
                "androidx/appcompat/app/AppCompatActivity",
                "androidx/fragment/app/FragmentActivity",
                -> true

                else -> false
            }
        }

        /**
         * Checks if the given class is an Android Fragment.
         *
         * @param superName The internal name of the superclass to check
         * @return True if the class extends from a known Fragment class, false otherwise
         */
        private fun isFragmentClass(superName: String?): Boolean {
            return when (superName) {
                "androidx/fragment/app/Fragment",
                "android/app/Fragment",
                -> true

                else -> false
            }
        }

        /**
         * Extracts a readable screen name from a class name.
         *
         * This function takes a fully qualified class name and converts it to a screen name
         * by removing the package path and common Android class suffixes.
         *
         * @param className The fully qualified class name (e.g., "com.example.MainActivity")
         * @return A simplified screen name (e.g., "Main" for "MainActivity")
         */
        private fun extractScreenNameFromClassName(className: String): String {
            return className.substringAfterLast('.')
                .removeSuffix("Activity")
                .removeSuffix("Fragment")
                .removeSuffix("Screen")
        }

        /**
         * Extracts the simple class name from a fully qualified class name.
         *
         * @param className The fully qualified class name (e.g., "com.example.MainActivity")
         * @return The simple class name (e.g., "MainActivity")
         */
        private fun extractSimpleClassName(className: String): String {
            return className.substringAfterLast('.')
        }

        /**
         * AnnotationVisitor that extracts parameters from @TrackScreen annotations.
         *
         * Captures the screenName and screenClass parameters from @TrackScreen annotations
         * and stores them in the outer class's state for use during bytecode generation.
         */
        private inner class TrackScreenAnnotationVisitor(
            private val delegate: AnnotationVisitor?,
        ) : AnnotationVisitor(api, delegate) {
            override fun visit(
                name: String?,
                value: Any?,
            ) {
                when (name) {
                    "value", "screenName" -> screenName = value as? String
                    "screenClass" -> screenClass = value as? String
                    else -> value?.let { annotationParameters[name ?: ""] = it }
                }
                delegate?.visit(name, value)
            }

            override fun visitEnd() {
                // Create currentAnnotationInfo immediately when annotation processing is complete
                currentAnnotationInfo = createAnnotationInfo()
                logDebug("AnalyticsClassVisitor: Set currentAnnotationInfo: $currentAnnotationInfo")
                delegate?.visitEnd()
            }
        }

        private fun logDebug(message: String) {
            if (parameters.debugMode.getOrElse(false)) {
                PluginLogger.debug(message)
            }
        }

        private fun logError(
            message: String,
            throwable: Throwable? = null,
        ) {
            PluginLogger.error(message, throwable)
        }
    }
}
