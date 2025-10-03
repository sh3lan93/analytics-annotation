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
import org.objectweb.asm.Type

abstract class AnalyticsClassVisitorFactory :
    AsmClassVisitorFactory<AnalyticsClassVisitorFactory.Parameters> {
    interface Parameters : InstrumentationParameters {
        @get:Input
        val enabled: Property<Boolean>

        @get:Input
        @get:Optional
        val debugMode: Property<Boolean>

        @get:Input
        @get:Optional
        val trackActivities: Property<Boolean>

        @get:Input
        @get:Optional
        val trackFragments: Property<Boolean>

        @get:Input
        @get:Optional
        val trackComposables: Property<Boolean>

        @get:Input
        @get:Optional
        val includePackages: ListProperty<String>

        @get:Input
        @get:Optional
        val excludePackages: ListProperty<String>

        // Method tracking configuration parameters
        @get:Input
        @get:Optional
        val methodTrackingEnabled: Property<Boolean>

        @get:Input
        @get:Optional
        val maxParametersPerMethod: Property<Int>

        @get:Input
        @get:Optional
        val validateAnnotations: Property<Boolean>

        @get:Input
        @get:Optional
        val excludeMethods: ListProperty<String>

        @get:Input
        @get:Optional
        val includeClassPatterns: ListProperty<String>

        @get:Input
        @get:Optional
        val excludeClassPatterns: ListProperty<String>
    }

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

    private inner class AnalyticsClassVisitor(
        api: Int,
        nextClassVisitor: ClassVisitor,
        private val parameters: Parameters,
        private val className: String,
    ) : ClassVisitor(api, nextClassVisitor) {
        private var annotationInfo: TrackingAnnotationInfo? = null
        private var currentAnnotationInfo: TrackingAnnotationInfo? = null
        private var hasTrackScreenAnnotation = false
        private var hasTrackScreenComposableAnnotation = false
        private var hasTrackableAnnotation = false
        private val methodsWithTrackAnnotation = mutableMapOf<String, MethodTrackInfo>()
        private var screenName: String? = null
        private var screenClass: String? = null
        private val annotationParameters: MutableMap<String, Any> = mutableMapOf()
        private var internalClassName: String = ""
        private var superClassName: String? = null
        private var isActivity: Boolean = false
        private var isFragment: Boolean = false
        private val methodsToInstrument = mutableListOf<String>()
        private var hasOnCreateMethod = false
        private var onCreateMethodAccess = 0
        private var implementsTrackedScreenParamsProvider: Boolean = false

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
            superClassName = superName

            // Check if the class implements TrackedScreenParamsProvider
            implementsTrackedScreenParamsProvider = interfaces?.contains("com/shalan/analytics/core/TrackedScreenParamsProvider") == true

            // Determine class type for proper transformation
            isActivity = isActivityClass(superName)
            isFragment = isFragmentClass(superName)

            name?.let { internalName ->
                val dotClassName = internalName.replace('/', '.')
                logDebug(
                    "AnalyticsClassVisitor: Class $dotClassName - isActivity: $isActivity, isFragment: $isFragment, superName: $superName",
                )
                logDebug("AnalyticsClassVisitor: Raw superName: '$superName'")
                logDebug("AnalyticsClassVisitor: Implements TrackedScreenParamsProvider: $implementsTrackedScreenParamsProvider")
            }
        }

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

                "Lcom/shalan/analytics/compose/TrackScreenComposable;" -> {
                    hasTrackScreenComposableAnnotation = true
                    logDebug("AnalyticsClassVisitor: Found @TrackScreenComposable annotation on class $className")
                    return TrackScreenComposableAnnotationVisitor(
                        super.visitAnnotation(
                            descriptor,
                            visible,
                        ),
                    )
                }

                "Lcom/shalan/analytics/annotation/Trackable;" -> {
                    hasTrackableAnnotation = true
                    logDebug("AnalyticsClassVisitor: Found @Trackable annotation on class $className")
                    return super.visitAnnotation(descriptor, visible)
                }
            }
            return super.visitAnnotation(descriptor, visible)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            try {
                if (descriptor?.contains("Landroidx/compose/runtime/Composer;") == true) {
                    logDebug("FORCE_DEBUG: Found Composer method $name with descriptor $descriptor")
                }
                logDebug("AnalyticsClassVisitor: [MODIFIED] Visiting method '$name' with descriptor '$descriptor' in class $className")
                // Check if this is a method we should instrument
                if (shouldCollectMethod(name, descriptor)) {
                    methodsToInstrument.add(name ?: "")
                    logDebug("AnalyticsClassVisitor: Collected method $name for potential instrumentation")

                    // Store onCreate method info for later modification
                    if (name == "onCreate" && descriptor == "(Landroid/os/Bundle;)V") {
                        hasOnCreateMethod = true
                        onCreateMethodAccess = access
                    }
                }

                val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)

                // If this is a lifecycle method we want to instrument, wrap with our instrumenting visitor
                val isActivityOnCreate =
                    name == "onCreate" && descriptor == "(Landroid/os/Bundle;)V" && isActivity
                val isFragmentOnViewCreated =
                    name == "onViewCreated" && descriptor == "(Landroid/view/View;Landroid/os/Bundle;)V" && isFragment
                val isComposableFunction = isComposableMethod(name, descriptor)

                logDebug("FORCE_DEBUG: isComposableFunction result for $name = $isComposableFunction")

                logDebug(
                    "AnalyticsClassVisitor: Method $name - isActivity: $isActivity, " +
                        "isFragment: $isFragment, isComposable: $isComposableFunction",
                )

                logDebug("FORCE_DEBUG: About to check method path for $name")
                PluginLogger.forceDebug(
                    "FORCE_DEBUG: Method $name paths - onCreate: $isActivityOnCreate, onViewCreated: $isFragmentOnViewCreated",
                )

                if (isActivityOnCreate || isFragmentOnViewCreated) {
                    PluginLogger.forceDebug("FORCE_DEBUG: Taking lifecycle path for method: $name")
                    logDebug("AnalyticsClassVisitor: Wrapping $name method for potential instrumentation")
                    return LifecycleInstrumentingMethodVisitor(api, methodVisitor, name ?: "unknown")
                } else if (isComposableFunction) {
                    PluginLogger.forceDebug("FORCE_DEBUG: Taking composable path for method: $name")
                    PluginLogger.forceDebug("Creating ComposableMethodVisitor for $name")
                    logDebug("AnalyticsClassVisitor: Wrapping Composable $name method for potential instrumentation")
                    return ComposableMethodVisitor(api, methodVisitor, name ?: "unknown", descriptor ?: "")
                }

                PluginLogger.forceDebug("FORCE_DEBUG: Taking TrackMethodVisitor path for method: $name")
                PluginLogger.forceDebug("FORCE_DEBUG: About to create TrackMethodVisitor for method: $name")

                // Check if this method might have @Track annotation and wrap with TrackMethodVisitor
                val methodTrackingEnabled = parameters.methodTrackingEnabled.getOrElse(true)
                val maxParametersPerMethod = parameters.maxParametersPerMethod.getOrElse(10)
                val excludeMethods = parameters.excludeMethods.getOrElse(emptyList()).toSet()

                PluginLogger.forceDebug(
                    "Creating TrackMethodVisitor for method: $name, enabled: $methodTrackingEnabled",
                )

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
            } catch (e: Exception) {
                PluginLogger.forceDebug("FORCE_DEBUG: Exception in visitMethod for $name: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }

        override fun visitEnd() {
            try {
                // Check if we found any tracking annotations OR @Trackable annotation for method-level tracking
                val hasClassLevelAnnotations = hasTrackScreenAnnotation || hasTrackScreenComposableAnnotation
                val hasTrackableMethods = hasTrackableAnnotation

                if (hasClassLevelAnnotations && methodsToInstrument.isNotEmpty()) {
                    // Process screen-level tracking annotations (@TrackScreen, @TrackScreenComposable)
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

        private fun createAnnotationInfo(): TrackingAnnotationInfo? {
            return when {
                hasTrackScreenAnnotation ->
                    TrackingAnnotationInfo(
                        screenName = screenName ?: extractScreenNameFromClassName(className),
                        screenClass = screenClass?.takeIf { it.isNotEmpty() } ?: extractSimpleClassName(className),
                        annotationType = TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN,
                        className = className,
                    )

                hasTrackScreenComposableAnnotation ->
                    TrackingAnnotationInfo(
                        screenName = screenName ?: extractScreenNameFromClassName(className),
                        screenClass = screenClass?.takeIf { it.isNotEmpty() } ?: extractSimpleClassName(className),
                        annotationType = TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN_COMPOSABLE,
                        className = className,
                    )

                else -> null
            }
        }

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

                TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN_COMPOSABLE -> {
                    // For Composables, no need to inject __injectAnalyticsTracking method
                    // The TrackScreenOnce composable is called directly in ComposableMethodVisitor.visitCode()
                    logDebug("AnalyticsClassVisitor: Composable tracking injection completed for ${annotationInfo.screenName}")
                }
            }
        }

        private fun injectTrackingCallIntoMethod(annotationInfo: TrackingAnnotationInfo) {
            // The actual injection happens in OnCreateInstrumentingMethodVisitor
            // We just need to make sure our tracking method exists
            injectTrackingMethod(annotationInfo)
        }

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

        private inner class ComposableMethodVisitor(
            api: Int,
            private val delegate: MethodVisitor,
            private val methodName: String,
            private val methodDescriptor: String,
        ) : MethodVisitor(api, delegate) {
            private var methodHasTrackScreenComposableAnnotation = false
            private var methodHasComposableAnnotation = false
            private var methodScreenName: String? = null
            private var hasInjectedTracking = false

            override fun visitAnnotation(
                descriptor: String?,
                visible: Boolean,
            ): AnnotationVisitor? {
                when (descriptor) {
                    "Landroidx/compose/runtime/Composable;" -> {
                        methodHasComposableAnnotation = true
                        logDebug("AnalyticsClassVisitor: Found @Composable annotation on method $methodName")
                        return delegate.visitAnnotation(descriptor, visible)
                    }
                    "Lcom/shalan/analytics/compose/TrackScreenComposable;" -> {
                        methodHasTrackScreenComposableAnnotation = true
                        hasTrackScreenComposableAnnotation = true
                        PluginLogger.forceDebug("Found @TrackScreenComposable annotation on method $methodName")
                        logDebug("AnalyticsClassVisitor: Found @TrackScreenComposable annotation on method $methodName")
                        return ComposableAnnotationVisitor(
                            delegate.visitAnnotation(
                                descriptor,
                                visible,
                            ),
                        )
                    }
                }
                return delegate.visitAnnotation(descriptor, visible)
            }

            override fun visitCode() {
                // Call the original visitCode
                delegate.visitCode()

                // Validate that @TrackScreenComposable is only used with @Composable functions
                if (methodHasTrackScreenComposableAnnotation && !methodHasComposableAnnotation) {
                    PluginLogger.error(
                        "WARNING: @TrackScreenComposable annotation found on method '$methodName' " +
                            "which is not annotated with @Composable. This annotation should only be used " +
                            "on Composable functions. Please either:\n" +
                            "1. Add @Composable annotation to the function, or\n" +
                            "2. Use @Track annotation instead for regular functions.",
                    )
                }

                // Inject tracking call at the beginning of the Composable function
                // but only if this method has @TrackScreenComposable annotation AND @Composable annotation
                if (methodHasTrackScreenComposableAnnotation && methodHasComposableAnnotation &&
                    methodScreenName != null && !hasInjectedTracking
                ) {
                    hasInjectedTracking = true
                    logDebug(
                        "AnalyticsClassVisitor: Injecting TrackScreenOnce call at start of Composable $methodName " +
                            "with screenName: $methodScreenName",
                    )

                    // Instead of calling __injectAnalyticsTracking, call the TrackScreenOnce composable
                    // This ensures the tracking is wrapped in a LaunchedEffect to prevent duplicate events on recomposition

                    // Push screenName parameter
                    delegate.visitLdcInsn(methodScreenName)

                    // Push screenClass parameter (use method name or extracted class name)
                    val screenClass = extractSimpleClassName(className)
                    delegate.visitLdcInsn(screenClass)

                    // Push empty map for parameters
                    delegate.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "java/util/Collections",
                        "emptyMap",
                        "()Ljava/util/Map;",
                        false,
                    )

                    // Call TrackScreenOnce(screenName, screenClass, parameters, $composer, $changed)
                    // Note: Composable functions have additional Composer and changed parameters
                    // We need to pass through the Composer parameter from the current method
                    delegate.visitVarInsn(Opcodes.ALOAD, getComposerParameterIndex())
                    delegate.visitInsn(Opcodes.ICONST_0) // $changed parameter = 0

                    delegate.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/shalan/analytics/compose/TrackScreenOnceKt",
                        "TrackScreenOnce",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Landroidx/compose/runtime/Composer;I)V",
                        false,
                    )
                }
            }

            private fun getComposerParameterIndex(): Int {
                // The Composer parameter is typically near the end of the parameter list
                // We need to calculate the local variable index based on method descriptor
                val paramTypes = Type.getArgumentTypes(methodDescriptor)
                var localVarIndex = 0

                // Composable functions are typically static (top-level functions)
                // If this were an instance method, slot 0 would be 'this'
                // For now we assume static methods for top-level composables

                // Find the Composer parameter by scanning through the parameters
                paramTypes.forEach { type ->
                    if (type.descriptor.contains("Landroidx/compose/runtime/Composer;")) {
                        return localVarIndex
                    }
                    localVarIndex += type.size // Type.size() returns 2 for long/double, 1 for others
                }

                // Fallback: if we didn't find the Composer, return the current index
                // This should not happen for valid composable functions
                logDebug("WARNING: Could not find Composer parameter in method $methodName")
                return localVarIndex
            }

            private inner class ComposableAnnotationVisitor(
                private val delegate: AnnotationVisitor?,
            ) : AnnotationVisitor(api, delegate) {
                override fun visit(
                    name: String?,
                    value: Any?,
                ) {
                    when (name) {
                        "value", "screenName" -> {
                            methodScreenName = value as? String
                            screenName = methodScreenName // Also set the class-level screenName
                            PluginLogger.forceDebug("Extracted screenName: $methodScreenName")
                        }

                        else -> value?.let { annotationParameters[name ?: ""] = it }
                    }
                    delegate?.visit(name, value)
                }
            }
        }

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

        private fun shouldCollectMethod(
            methodName: String?,
            descriptor: String?,
        ): Boolean {
            // Collect methods that we might want to instrument later
            return when {
                isActivity && methodName == "onCreate" && descriptor == "(Landroid/os/Bundle;)V" -> true
                isFragment && methodName == "onViewCreated" && descriptor == "(Landroid/view/View;Landroid/os/Bundle;)V" -> true
                // Composable functions typically have descriptors with Composer parameter
                isComposableMethod(methodName, descriptor) -> true
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
         * Checks if the given method is a Jetpack Compose Composable function.
         *
         * Composable functions are identified by having a Composer parameter in their descriptor.
         *
         * @param methodName The name of the method to check
         * @param descriptor The method descriptor containing parameter and return types
         * @return True if the method is a Composable function, false otherwise
         */
        private fun isComposableMethod(
            methodName: String?,
            descriptor: String?,
        ): Boolean {
            // Composable functions have specific signatures with Composer parameter
            val hasComposer = descriptor?.contains("Landroidx/compose/runtime/Composer;") == true

            logDebug("isComposableMethod($methodName, $descriptor) = $hasComposer")

            return hasComposer
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

        private fun extractSimpleClassName(className: String): String {
            return className.substringAfterLast('.')
        }

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

        private inner class TrackScreenComposableAnnotationVisitor(
            private val delegate: AnnotationVisitor?,
        ) : AnnotationVisitor(api, delegate) {
            override fun visit(
                name: String?,
                value: Any?,
            ) {
                when (name) {
                    "value", "screenName" -> screenName = value as? String
                    else -> value?.let { annotationParameters[name ?: ""] = it }
                }
                delegate?.visit(name, value)
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
