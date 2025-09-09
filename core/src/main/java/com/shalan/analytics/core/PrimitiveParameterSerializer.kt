package com.shalan.analytics.core

/**
 * Parameter serializer for primitive types and common data types.
 * Handles String, Number types (Int, Long, Float, Double), Boolean, and Char.
 */
class PrimitiveParameterSerializer : ParameterSerializer {
    private val supportedTypes =
        setOf(
            String::class.java,
            Boolean::class.java,
            Int::class.java,
            Long::class.java,
            Float::class.java,
            Double::class.java,
            Char::class.java,
            // Boxed primitives
            java.lang.Boolean::class.java,
            java.lang.Integer::class.java,
            java.lang.Long::class.java,
            java.lang.Float::class.java,
            java.lang.Double::class.java,
            java.lang.Character::class.java,
        )

    override fun canSerialize(parameterType: Class<*>): Boolean {
        return supportedTypes.contains(parameterType) || parameterType.isPrimitive
    }

    override fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any? {
        if (value == null) return null

        return when (value) {
            is String -> value
            is Number -> value
            is Boolean -> value
            is Char -> value.toString()
            else -> value.toString() // Fallback for other primitive types
        }
    }

    override fun getPriority(): Int = 100 // High priority for primitive types
}
