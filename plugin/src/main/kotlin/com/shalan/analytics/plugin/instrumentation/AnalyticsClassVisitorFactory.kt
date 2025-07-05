package com.shalan.analytics.plugin.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.shalan.analytics.plugin.utils.ErrorReporter
import com.shalan.analytics.plugin.utils.TrackingAnnotationInfo
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

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
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor {
        val params = parameters.get()

        // If plugin is disabled, just pass through
        if (!params.enabled.getOrElse(true)) {
            return nextClassVisitor
        }

        val visitor =
            AnalyticsClassVisitor(
                api = instrumentationContext.apiVersion.get(),
                nextClassVisitor = nextClassVisitor,
                parameters = params,
                className = classContext.currentClassData.className,
            )

        if (classContext.currentClassData.className.contains("ExampleComposableScreenKt")) {
            println("FORCE_DEBUG: Created AnalyticsClassVisitor for ${classContext.currentClassData.className}")
        }

        return visitor
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val params = parameters.get()

        // If plugin is disabled, don't instrument anything
        if (!params.enabled.getOrElse(true)) {
            println("DEBUG: Analytics Annotation Plugin is disabled")
            return false
        }

        val className = classData.className.replace('/', '.')
        println("DEBUG: Checking if class $className is instrumentable")

        if (className.contains("ExampleComposableScreenKt")) {
            println("FORCE_DEBUG: Found target class in isInstrumentable: $className")
        }

        // Skip system classes
        if (className.startsWith("android.") ||
            className.startsWith("androidx.") ||
            className.startsWith("java.") ||
            className.startsWith("kotlin.")
        ) {
            println("DEBUG: Skipping system class $className")
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

        println("DEBUG: Class $className is instrumentable")
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
        private var screenName: String? = null
        private var screenClass: String? = null
        private val additionalParams: MutableList<String> = mutableListOf()
        private val annotationParameters: MutableMap<String, Any> = mutableMapOf()
        private var internalClassName: String = ""
        private var superClassName: String? = null
        private var isActivity: Boolean = false
        private var isFragment: Boolean = false
        private val methodsToInstrument = mutableListOf<String>()
        private var hasOnCreateMethod = false
        private var onCreateMethodAccess = 0

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
            if (descriptor?.contains("Landroidx/compose/runtime/Composer;") == true) {
                logDebug("FORCE_DEBUG: Found Composer method $name with descriptor $descriptor")
            }
            logDebug("AnalyticsClassVisitor: [MODIFIED] Visiting method '$name' with descriptor '$descriptor' in class $className")
            logDebug("FORCE_DEBUG: About to call isComposableMethod for $name")

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

            if (isActivityOnCreate || isFragmentOnViewCreated) {
                logDebug("AnalyticsClassVisitor: Wrapping $name method for potential instrumentation")
                return LifecycleInstrumentingMethodVisitor(api, methodVisitor, name ?: "unknown")
            } else if (isComposableFunction) {
                println("FORCE_DEBUG: Creating ComposableMethodVisitor for $name")
                logDebug("AnalyticsClassVisitor: Wrapping Composable $name method for potential instrumentation")
                return ComposableMethodVisitor(api, methodVisitor, name ?: "unknown")
            }

            // Return a method visitor that can detect method-level annotations
            return object : MethodVisitor(api, methodVisitor) {
                override fun visitAnnotation(
                    descriptor: String?,
                    visible: Boolean,
                ): AnnotationVisitor? {
                    when (descriptor) {
                        "Lcom/shalan/analytics/annotation/TrackScreen;" -> {
                            hasTrackScreenAnnotation = true
                            logDebug("AnalyticsClassVisitor: Found @TrackScreen annotation on method $name in class $className")
                            return TrackScreenAnnotationVisitor(
                                super.visitAnnotation(
                                    descriptor,
                                    visible,
                                ),
                            )
                        }

                        "Lcom/shalan/analytics/compose/TrackScreenComposable;" -> {
                            hasTrackScreenComposableAnnotation = true
                            logDebug(
                                "AnalyticsClassVisitor: Found @TrackScreenComposable annotation on method $name in class $className",
                            )
                            return TrackScreenComposableAnnotationVisitor(
                                super.visitAnnotation(
                                    descriptor,
                                    visible,
                                ),
                            )
                        }
                    }
                    return super.visitAnnotation(descriptor, visible)
                }
            }
        }

        override fun visitEnd() {
            try {
                // Check if we found any tracking annotations AND have methods to instrument
                if ((hasTrackScreenAnnotation || hasTrackScreenComposableAnnotation) && methodsToInstrument.isNotEmpty()) {
                    annotationInfo = createAnnotationInfo()

                    if (annotationInfo != null) {
                        logDebug("AnalyticsClassVisitor: Processing class with tracking annotations: $className")
                        logDebug("AnalyticsClassVisitor: Found ${methodsToInstrument.size} methods to instrument")

                        // Perform deferred transformation now that we have all annotation info
                        performDeferredTransformation(annotationInfo!!)
                    }
                } else {
                    logDebug("AnalyticsClassVisitor: No tracking annotations found in $className or no instrumentable methods")
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
                        additionalParams = additionalParams.toList(),
                        annotationType = TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN,
                        className = className,
                    )

                hasTrackScreenComposableAnnotation ->
                    TrackingAnnotationInfo(
                        screenName = screenName ?: extractScreenNameFromClassName(className),
                        screenClass = screenClass?.takeIf { it.isNotEmpty() } ?: extractSimpleClassName(className),
                        additionalParams = additionalParams.toList(),
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
                    // For Composables, inject the tracking method
                    injectTrackingMethod(annotationInfo)
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
        ) : MethodVisitor(api, delegate) {
            private var methodHasTrackScreenComposableAnnotation = false
            private var methodScreenName: String? = null
            private var hasInjectedTracking = false

            override fun visitAnnotation(
                descriptor: String?,
                visible: Boolean,
            ): AnnotationVisitor? {
                when (descriptor) {
                    "Lcom/shalan/analytics/compose/TrackScreenComposable;" -> {
                        methodHasTrackScreenComposableAnnotation = true
                        hasTrackScreenComposableAnnotation = true
                        println("FORCE_DEBUG: Found @TrackScreenComposable annotation on method $methodName")
                        logDebug("AnalyticsClassVisitor: Found @TrackScreenComposable annotation on Composable method $methodName")
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

                // Inject tracking call at the beginning of the Composable function
                // but only if this method has @TrackScreenComposable annotation
                if (methodHasTrackScreenComposableAnnotation && methodScreenName != null && !hasInjectedTracking) {
                    hasInjectedTracking = true
                    logDebug(
                        "AnalyticsClassVisitor: Injecting tracking call at start of Composable $methodName " +
                            "with screenName: $methodScreenName",
                    )

                    // Call the injected __injectAnalyticsTracking method (static for composables)
                    delegate.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        internalClassName,
                        "__injectAnalyticsTracking",
                        "()V",
                        false,
                    )
                }
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
                            println("FORCE_DEBUG: Extracted screenName: $methodScreenName")
                        }

                        else -> value?.let { annotationParameters[name ?: ""] = it }
                    }
                    delegate?.visit(name, value)
                }
            }
        }

        private fun injectTrackingMethod(annotationInfo: TrackingAnnotationInfo) {
            // Inject a method that performs the tracking call
            // Make it static for Composables, private for Activities/Fragments
            val methodAccess =
                if (annotationInfo.annotationType == TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN_COMPOSABLE) {
                    Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC
                } else {
                    Opcodes.ACC_PRIVATE
                }

            val methodVisitor =
                cv.visitMethod(
                    methodAccess,
                    "__injectAnalyticsTracking",
                    "()V",
                    null,
                    null,
                )

            methodVisitor.visitCode()

            // Generate: ScreenTracking.getManager().logScreenView(screenName, screenClass, parameters)
            methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/shalan/analytics/core/ScreenTracking",
                "getManager",
                "()Lcom/shalan/analytics/core/AnalyticsManager;",
                false,
            )

            // Push screenName parameter
            methodVisitor.visitLdcInsn(annotationInfo.screenName)

            // Push screenClass parameter
            methodVisitor.visitLdcInsn(annotationInfo.screenClass ?: annotationInfo.className)

            // Create and push parameters Map
            generateParametersMap(methodVisitor, annotationInfo.additionalParams)

            methodVisitor.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/shalan/analytics/core/AnalyticsManager",
                "logScreenView",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;)V",
                true,
            )

            methodVisitor.visitInsn(Opcodes.RETURN)
            methodVisitor.visitMaxs(6, 1) // Increased stack for array operations
            methodVisitor.visitEnd()

            logDebug("AnalyticsClassVisitor: Successfully injected tracking method")
        }

        private fun generateParametersMap(
            methodVisitor: MethodVisitor,
            additionalParams: List<String>,
        ) {
            if (additionalParams.isEmpty()) {
                // Create empty map: Collections.emptyMap()
                methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/util/Collections",
                    "emptyMap",
                    "()Ljava/util/Map;",
                    false,
                )
            } else {
                // Generate code to call our helper method
                // Load 'this' for the helper method call
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)

                // Create array of parameter keys
                methodVisitor.visitLdcInsn(additionalParams.size)
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String")

                // Populate the array with parameter keys
                for (i in additionalParams.indices) {
                    methodVisitor.visitInsn(Opcodes.DUP)
                    methodVisitor.visitLdcInsn(i)
                    methodVisitor.visitLdcInsn(additionalParams[i])
                    methodVisitor.visitInsn(Opcodes.AASTORE)
                }

                // Call ScreenTracking.createParametersMap(this, paramKeys)
                methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/shalan/analytics/core/ScreenTracking",
                    "createParametersMap",
                    "(Ljava/lang/Object;[Ljava/lang/String;)Ljava/util/Map;",
                    false,
                )
            }
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

        private fun isActivityClass(superName: String?): Boolean {
            return when (superName) {
                "android/app/Activity",
                "androidx/appcompat/app/AppCompatActivity",
                "androidx/fragment/app/FragmentActivity",
                -> true

                else -> false
            }
        }

        private fun isFragmentClass(superName: String?): Boolean {
            return when (superName) {
                "androidx/fragment/app/Fragment",
                "android/app/Fragment",
                -> true

                else -> false
            }
        }

        private fun isComposableMethod(
            methodName: String?,
            descriptor: String?,
        ): Boolean {
            // Composable functions have specific signatures with Composer parameter
            val hasComposer = descriptor?.contains("Landroidx/compose/runtime/Composer;") == true

            logDebug("isComposableMethod($methodName, $descriptor) = $hasComposer")

            return hasComposer
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

            override fun visitArray(name: String?): AnnotationVisitor? {
                return if (name == "additionalParams") {
                    object : AnnotationVisitor(api, delegate?.visitArray(name)) {
                        override fun visit(
                            name: String?,
                            value: Any?,
                        ) {
                            if (value is String) {
                                additionalParams.add(value)
                            }
                            super.visit(name, value)
                        }
                    }
                } else {
                    delegate?.visitArray(name)
                }
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
                println("DEBUG: $message")
            }
        }

        private fun logError(
            message: String,
            throwable: Throwable? = null,
        ) {
            println("ERROR: $message")
            throwable?.printStackTrace()
        }
    }
}
