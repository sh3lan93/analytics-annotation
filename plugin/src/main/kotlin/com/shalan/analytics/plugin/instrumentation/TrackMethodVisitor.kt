package com.shalan.analytics.plugin.instrumentation

import com.shalan.analytics.plugin.utils.PluginLogger
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

data class PendingTrackInfo(
    val eventName: String,
    val includeGlobalParams: Boolean,
)

/**
 * ASM MethodVisitor that handles @Track annotation instrumentation.
 * This visitor injects calls to MethodTrackingManager.track() at method entry points.
 */
class TrackMethodVisitor(
    api: Int,
    private val delegate: MethodVisitor,
    private val methodName: String,
    private val methodDescriptor: String,
    private val className: String,
    // Method access flags to determine if method is static
    private val access: Int = 0,
    // Method tracking configuration from Gradle plugin
    private val methodTrackingEnabled: Boolean = true,
    private val maxParametersPerMethod: Int = 10,
    private val excludeMethods: Set<String> = emptySet(),
) : MethodVisitor(api, delegate) {
    private var trackAnnotationInfo: MethodTrackInfo? = null
    private var pendingTrackInfo: PendingTrackInfo? = null
    private var parameterAnnotations =
        mutableMapOf<Int, String>() // parameter index -> parameter name
    private var hasInjectedTracking = false

    init {
        PluginLogger.debug(
            "TrackMethodVisitor created for: $methodName, enabled: $methodTrackingEnabled",
        )
    }

    override fun visitAnnotation(
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor? {
        PluginLogger.debug(
            "TrackMethodVisitor.visitAnnotation - method: " +
                "$methodName, descriptor: $descriptor, visible: $visible",
        )
        return when (descriptor) {
            "Lcom/shalan/analytics/annotation/Track;" -> {
                PluginLogger.debug(
                    "Found @Track on $methodName, enabled: $methodTrackingEnabled, " +
                        "excluded: ${
                            excludeMethods.contains(
                                methodName,
                            )
                        }",
                )
                // Only process @Track annotations if method tracking is enabled
                if (methodTrackingEnabled && !excludeMethods.contains(methodName)) {
                    PluginLogger.debug("Creating TrackAnnotationVisitor for method: $methodName")
                    TrackAnnotationVisitor(delegate.visitAnnotation(descriptor, visible))
                } else {
                    PluginLogger.debug("Skipping @Track annotation for method: $methodName (tracking disabled or excluded)")
                    delegate.visitAnnotation(descriptor, visible)
                }
            }

            else -> delegate.visitAnnotation(descriptor, visible)
        }
    }

    override fun visitParameterAnnotation(
        parameter: Int,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor? {
        PluginLogger.debug(
            "visitParameterAnnotation - method: $methodName, param: $parameter",
        )
        return when (descriptor) {
            "Lcom/shalan/analytics/annotation/Param;" -> {
                PluginLogger.debug("Found @Param annotation on parameter $parameter for method: $methodName")
                ParamAnnotationVisitor(
                    parameter,
                    delegate.visitParameterAnnotation(parameter, descriptor, visible),
                )
            }

            else -> delegate.visitParameterAnnotation(parameter, descriptor, visible)
        }
    }

    override fun visitCode() {
        delegate.visitCode()

        // Create MethodTrackInfo from pendingTrackInfo if we have a @Track annotation
        if (pendingTrackInfo != null && trackAnnotationInfo == null) {
            PluginLogger.debug(
                "visitCode() - method: $methodName, creating MethodTrackInfo with " +
                    "parameterAnnotations.size: ${parameterAnnotations.size}",
            )
            parameterAnnotations.forEach { (index, name) ->
                PluginLogger.debug("  Parameter: index=$index, name='$name'")
            }

            val paramInfos =
                parameterAnnotations.map { (index, paramName) ->
                    ParamInfo(name = paramName, index = index)
                }

            trackAnnotationInfo =
                MethodTrackInfo(
                    eventName = pendingTrackInfo!!.eventName,
                    includeGlobalParams = pendingTrackInfo!!.includeGlobalParams,
                    parameters = paramInfos,
                )

            PluginLogger.debug(
                "Created final MethodTrackInfo - eventName: '${pendingTrackInfo!!.eventName}', " +
                    "parameters: ${paramInfos.size}",
            )
        }

        // Inject tracking call at method entry if @Track annotation is present
        if (trackAnnotationInfo != null && !hasInjectedTracking) {
            hasInjectedTracking = true
            injectTrackingCall(trackAnnotationInfo!!)
        }
    }

    private fun injectTrackingCall(trackInfo: MethodTrackInfo) {
        // Generate call: MethodTrackingManager.track(eventName, parameters, includeGlobalParams)

        // Push event name
        delegate.visitLdcInsn(trackInfo.eventName)

        // Create parameters map
        generateParametersMap(trackInfo)

        // Push includeGlobalParams
        if (trackInfo.includeGlobalParams) {
            delegate.visitInsn(Opcodes.ICONST_1)
        } else {
            delegate.visitInsn(Opcodes.ICONST_0)
        }

        // Call MethodTrackingManager.track(String, Map, boolean)
        delegate.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "com/shalan/analytics/core/MethodTrackingManager",
            "track",
            "(Ljava/lang/String;Ljava/util/Map;Z)V",
            false,
        )
    }

    private fun generateParametersMap(trackInfo: MethodTrackInfo) {
        // Apply parameter limit from plugin configuration
        val limitedParams =
            trackInfo.parameters
                .filter { it.index < getParameterCount() }
                .take(maxParametersPerMethod)

        PluginLogger.debug(
            "generateParametersMap - method: $methodName, trackInfo.parameters: " +
                "${trackInfo.parameters.size}, limitedParams: ${limitedParams.size}",
        )
        trackInfo.parameters.forEach { param ->
            PluginLogger.debug("  Parameter: index=${param.index}, name='${param.name}'")
        }

        if (limitedParams.isEmpty()) {
            // Create empty map: Collections.emptyMap()
            PluginLogger.debug("Creating empty map for method: $methodName")
            delegate.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/Collections",
                "emptyMap",
                "()Ljava/util/Map;",
                false,
            )
            return
        }

        // Calculate map size
        val mapSize = limitedParams.size

        // Create HashMap with initial capacity
        delegate.visitTypeInsn(Opcodes.NEW, "java/util/HashMap")
        delegate.visitInsn(Opcodes.DUP)
        delegate.visitLdcInsn(mapSize)
        delegate.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/util/HashMap",
            "<init>",
            "(I)V",
            false,
        )

        // Add each tracked parameter to the map
        limitedParams.forEach { param ->
            // Duplicate map reference for put() call
            delegate.visitInsn(Opcodes.DUP)

            // Push parameter name as key
            delegate.visitLdcInsn(param.name)

            // Load parameter value from local variables
            loadParameterValue(param.index)

            // Box primitive types if needed
            boxPrimitiveIfNeeded(param.index)

            // Call map.put(key, value)
            delegate.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "java/util/Map",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                true,
            )

            // Pop the return value from put()
            delegate.visitInsn(Opcodes.POP)
        }
    }

    private fun loadParameterValue(parameterIndex: Int) {
        val paramTypes = getParameterTypes()
        if (parameterIndex >= paramTypes.size) return

        val paramType = paramTypes[parameterIndex]
        val localVarIndex = calculateLocalVariableIndex(parameterIndex)

        when (paramType.sort) {
            Type.INT -> delegate.visitVarInsn(Opcodes.ILOAD, localVarIndex) // int
            Type.LONG -> delegate.visitVarInsn(Opcodes.LLOAD, localVarIndex) // long
            Type.FLOAT -> delegate.visitVarInsn(Opcodes.FLOAD, localVarIndex) // float
            Type.DOUBLE -> delegate.visitVarInsn(Opcodes.DLOAD, localVarIndex) // double
            Type.BOOLEAN -> delegate.visitVarInsn(Opcodes.ILOAD, localVarIndex) // boolean
            Type.BYTE -> delegate.visitVarInsn(Opcodes.ILOAD, localVarIndex) // byte
            Type.SHORT -> delegate.visitVarInsn(Opcodes.ILOAD, localVarIndex) // short
            Type.CHAR -> delegate.visitVarInsn(Opcodes.ILOAD, localVarIndex) // char
            else -> delegate.visitVarInsn(Opcodes.ALOAD, localVarIndex) // objects/arrays
        }
    }

    private fun boxPrimitiveIfNeeded(parameterIndex: Int) {
        val paramTypes = getParameterTypes()
        if (parameterIndex >= paramTypes.size) return

        val paramType = paramTypes[parameterIndex]

        when (paramType.sort) {
            Type.INT -> { // int -> Integer
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false,
                )
            }

            Type.LONG -> { // long -> Long
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false,
                )
            }

            Type.FLOAT -> { // float -> Float
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false,
                )
            }

            Type.DOUBLE -> { // double -> Double
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false,
                )
            }

            Type.BOOLEAN -> { // boolean -> Boolean
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false,
                )
            }

            Type.BYTE -> { // byte -> Byte
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false,
                )
            }

            Type.SHORT -> { // short -> Short
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false,
                )
            }

            Type.CHAR -> { // char -> Character
                delegate.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false,
                )
            }
            // Objects and arrays don't need boxing
        }
    }

    /**
     * Parses method descriptor using ASM's Type class for better accuracy.
     */
    private fun getParameterTypes(): Array<Type> {
        return Type.getArgumentTypes(methodDescriptor)
    }

    /**
     * Calculates the local variable index for a parameter, accounting for 'this' in instance methods.
     */
    private fun calculateLocalVariableIndex(parameterIndex: Int): Int {
        val isStatic = (access and Opcodes.ACC_STATIC) != 0
        var localVarIndex = if (isStatic) 0 else 1 // Account for 'this' in instance methods

        val paramTypes = getParameterTypes()

        // Add up the sizes of all parameters before this one
        for (i in 0 until parameterIndex.coerceAtMost(paramTypes.size)) {
            localVarIndex += paramTypes[i].size // Type.size() returns 2 for long/double, 1 for others
        }

        return localVarIndex
    }

    private fun getParameterCount(): Int {
        return getParameterTypes().size
    }

    private inner class TrackAnnotationVisitor(
        private val delegate: AnnotationVisitor?,
    ) : AnnotationVisitor(api, delegate) {
        private var eventName: String = ""
        private var includeGlobalParams: Boolean = true

        override fun visit(
            name: String?,
            value: Any?,
        ) {
            when (name) {
                "eventName" -> eventName = value as? String ?: ""
                "includeGlobalParams" -> includeGlobalParams = value as? Boolean ?: true
            }
            delegate?.visit(name, value)
        }

        override fun visitEnd() {
            PluginLogger.debug(
                "TrackAnnotationVisitor.visitEnd() - method: $methodName, " +
                    "storing pending track info for later processing",
            )

            // Store pending track info - will create MethodTrackInfo in visitCode()
            pendingTrackInfo =
                PendingTrackInfo(
                    eventName = eventName,
                    includeGlobalParams = includeGlobalParams,
                )

            PluginLogger.debug(
                "Stored PendingTrackInfo - eventName: '$eventName', includeGlobalParams: $includeGlobalParams",
            )

            delegate?.visitEnd()
        }
    }

    private inner class ParamAnnotationVisitor(
        private val parameterIndex: Int,
        private val delegate: AnnotationVisitor?,
    ) : AnnotationVisitor(api, delegate) {
        override fun visit(
            name: String?,
            value: Any?,
        ) {
            // Debug logging to understand annotation property names
            PluginLogger.debug(
                "ParamAnnotationVisitor - parameterIndex: $parameterIndex," +
                    " name: '$name', value: '$value'",
            )

            when (name) {
                "name" -> {
                    val paramName = value as? String ?: "param$parameterIndex"
                    parameterAnnotations[parameterIndex] = paramName
                    PluginLogger.debug(
                        "Added parameter annotation - index: " +
                            "$parameterIndex, name: '$paramName'",
                    )
                }

                "value" -> {
                    val paramName = value as? String ?: "param$parameterIndex"
                    parameterAnnotations[parameterIndex] = paramName
                    PluginLogger.debug(
                        "Added parameter annotation via 'value' - index: " +
                            "$parameterIndex, name: '$paramName'",
                    )
                }
            }
            delegate?.visit(name, value)
        }
    }
}
