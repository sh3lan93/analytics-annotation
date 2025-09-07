package com.shalan.analytics.core

/**
 * Interface for serializing method parameters to analytics-compatible values.
 * Implementations should handle conversion of various parameter types to formats
 * suitable for analytics providers.
 */
interface ParameterSerializer {
    /**
     * Determines if this serializer can handle the given parameter type.
     *
     * @param parameterType The class type of the parameter to serialize
     * @return true if this serializer can handle the type, false otherwise
     */
    fun canSerialize(parameterType: Class<*>): Boolean

    /**
     * Serializes a parameter value to an analytics-compatible format.
     *
     * @param value The parameter value to serialize
     * @param parameterType The class type of the parameter
     * @return The serialized value, or null if serialization fails
     */
    fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any?

    /**
     * Gets the priority of this serializer. Higher priority serializers
     * are checked first when determining which serializer to use.
     *
     * @return The priority value (higher = more priority)
     */
    fun getPriority(): Int = 0
}
