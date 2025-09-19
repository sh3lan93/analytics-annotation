package com.shalan.analyticsannotation

import com.shalan.analytics.core.ParameterSerializer

/**
 * Custom parameter serializer for UserProfile objects.
 * This demonstrates how to create specialized serializers for specific types
 * that provide more targeted analytics data than generic JSON serialization.
 */
class UserProfileAnalyticsSerializer : ParameterSerializer {
    override fun canSerialize(parameterType: Class<*>): Boolean {
        return UserProfile::class.java.isAssignableFrom(parameterType)
    }

    override fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any? {
        if (value !is UserProfile) return null

        // Create analytics-focused representation instead of full object serialization
        return mapOf(
            "user_id" to value.id,
            "user_type" to if (value.isVerified) "verified" else "unverified",
            "age_group" to getAgeGroup(value.age),
            "email_domain" to value.email.substringAfter("@"),
            "has_metadata" to value.metadata.isNotEmpty(),
        )
    }

    override fun getPriority(): Int = 100 // High priority to override default JSON serialization

    private fun getAgeGroup(age: Int): String =
        when {
            age < 18 -> "under_18"
            age < 25 -> "18_24"
            age < 35 -> "25_34"
            age < 45 -> "35_44"
            age < 55 -> "45_54"
            else -> "55_plus"
        }
}

/**
 * Custom serializer for handling collections with size limits.
 * This prevents analytics events from becoming too large with big collections.
 */
class LimitedCollectionSerializer : ParameterSerializer {
    companion object {
        private const val MAX_COLLECTION_SIZE = 10
        private const val MAX_STRING_LENGTH = 100
    }

    override fun canSerialize(parameterType: Class<*>): Boolean {
        return Collection::class.java.isAssignableFrom(parameterType) ||
            parameterType.isArray
    }

    override fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any? {
        if (value == null) return null

        return when {
            value is Collection<*> -> serializeCollection(value)
            value.javaClass.isArray -> serializeArray(value)
            else -> value.toString()
        }
    }

    override fun getPriority(): Int = 50 // Medium priority

    private fun serializeCollection(collection: Collection<*>): Map<String, Any> {
        val items =
            collection.take(MAX_COLLECTION_SIZE).map { item ->
                when (item) {
                    is String ->
                        if (item.length > MAX_STRING_LENGTH) {
                            item.take(MAX_STRING_LENGTH) + "..."
                        } else {
                            item
                        }
                    else ->
                        item?.toString()?.let { str ->
                            if (str.length > MAX_STRING_LENGTH) {
                                str.take(MAX_STRING_LENGTH) + "..."
                            } else {
                                str
                            }
                        } ?: "null"
                }
            }

        return mapOf(
            "size" to collection.size,
            "items" to items,
            "truncated" to (collection.size > MAX_COLLECTION_SIZE),
        )
    }

    private fun serializeArray(array: Any): Map<String, Any> {
        val arrayList =
            when (array) {
                is Array<*> -> array.toList()
                is IntArray -> array.toList()
                is LongArray -> array.toList()
                is DoubleArray -> array.toList()
                is FloatArray -> array.toList()
                is BooleanArray -> array.toList()
                is ByteArray -> array.toList()
                is ShortArray -> array.toList()
                is CharArray -> array.toList()
                else -> listOf(array.toString())
            }

        return serializeCollection(arrayList)
    }
}

/**
 * Custom serializer for enum values that provides both the enum name and ordinal.
 * This is useful for analytics to track both human-readable values and numeric codes.
 */
class EnumAnalyticsSerializer : ParameterSerializer {
    override fun canSerialize(parameterType: Class<*>): Boolean {
        return parameterType.isEnum
    }

    override fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any? {
        if (value !is Enum<*>) return value?.toString()

        return mapOf(
            "name" to value.name,
            "ordinal" to value.ordinal,
            "type" to parameterType.simpleName,
        )
    }

    override fun getPriority(): Int = 75 // High priority for enums
}

/**
 * Custom serializer that redacts sensitive information from maps and objects.
 * This demonstrates privacy-conscious analytics implementation.
 */
class PrivacyAwareSerializer : ParameterSerializer {
    companion object {
        private val SENSITIVE_KEYS =
            setOf(
                "password", "token", "secret", "key", "pin", "ssn",
                "credit_card", "phone", "address", "email",
            )
        private val REDACTED_VALUE = "[REDACTED]"
    }

    override fun canSerialize(parameterType: Class<*>): Boolean {
        return Map::class.java.isAssignableFrom(parameterType)
    }

    override fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any? {
        if (value !is Map<*, *>) return value

        return value.mapValues { (key, mapValue) ->
            val keyStr = key?.toString()?.lowercase() ?: ""
            if (SENSITIVE_KEYS.any { sensitiveKey -> keyStr.contains(sensitiveKey) }) {
                REDACTED_VALUE
            } else {
                mapValue
            }
        }
    }

    override fun getPriority(): Int = 200 // Very high priority for privacy
}
