package com.shalan.analytics.core

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

/**
 * Parameter serializer that converts complex objects to JSON format using kotlinx.serialization.
 * This serializer handles various object types by converting them to JSON strings suitable for analytics.
 *
 * It can handle:
 * - @Serializable data classes and objects (using kotlinx.serialization)
 * - Collections (Lists, Sets)
 * - Maps
 * - Arrays
 * - Complex nested objects
 * - Regular classes (fallback to toString() representation)
 */
@OptIn(InternalSerializationApi::class)
class JsonParameterSerializer : ParameterSerializer {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
        }

    override fun canSerialize(parameterType: Class<*>): Boolean {
        return when {
            // Primitives and their boxed versions - let PrimitiveParameterSerializer handle these
            parameterType.isPrimitive -> false
            parameterType == String::class.java -> false
            parameterType == Boolean::class.java -> false
            parameterType == Int::class.java -> false
            parameterType == Long::class.java -> false
            parameterType == Float::class.java -> false
            parameterType == Double::class.java -> false
            parameterType == Char::class.java -> false
            // Boxed primitives
            parameterType == java.lang.Boolean::class.java -> false
            parameterType == java.lang.Integer::class.java -> false
            parameterType == java.lang.Long::class.java -> false
            parameterType == java.lang.Float::class.java -> false
            parameterType == java.lang.Double::class.java -> false
            parameterType == java.lang.Character::class.java -> false

            // Collections and complex objects - we can handle these
            Collection::class.java.isAssignableFrom(parameterType) -> true
            Map::class.java.isAssignableFrom(parameterType) -> true
            parameterType.isArray -> true

            // Data classes and regular classes - we can handle these
            else -> true
        }
    }

    override fun serialize(
        value: Any?,
        parameterType: Class<*>,
    ): Any? {
        if (value == null) return null

        return try {
            when (value) {
                // Collections - try kotlinx serialization first
                is Collection<*> -> serializeCollection(value)
                is Map<*, *> -> serializeMap(value)
                is Array<*> -> serializeArray(value)

                // Try kotlinx serialization for @Serializable classes first
                else -> serializeWithKotlinx(value) ?: createFallbackJson(value)
            }
        } catch (e: Exception) {
            // Final fallback to quoted string if all serialization fails
            "\"${value.toString().replace("\"", "\\\"")}\""
        }
    }

    override fun getPriority(): Int = -10 // Lower priority than custom serializers (0) but used for data classes

    @Suppress("UNCHECKED_CAST")
    private fun serializeWithKotlinx(value: Any): String? {
        return try {
            val kClass = value::class as KClass<Any>

            // Check if the class is annotated with @Serializable
            if (kClass.hasAnnotation<kotlinx.serialization.Serializable>()) {
                json.encodeToString(kClass.serializer(), value)
            } else {
                // Not @Serializable, return null to use fallback
                null
            }
        } catch (e: SerializationException) {
            // Kotlinx serialization failed, return null to use fallback
            null
        } catch (e: Exception) {
            // Any other error, return null to use fallback
            null
        }
    }

    private fun serializeCollection(collection: Collection<*>): String {
        return try {
            // Try to serialize collection elements properly
            val jsonElements =
                collection.map { element ->
                    when (element) {
                        null -> "null"
                        is String -> json.encodeToString(String.serializer(), element)
                        is Number -> element.toString()
                        is Boolean -> element.toString()
                        else -> {
                            // Try kotlinx serialization first, fallback to quoted string
                            serializeWithKotlinx(element) ?: "\"${element.toString().replace("\"", "\\\"")}\""
                        }
                    }
                }
            "[${jsonElements.joinToString(",")}]"
        } catch (e: Exception) {
            // Fallback to simple serialization
            val elements =
                collection.map { element ->
                    when (element) {
                        null -> "null"
                        is String -> "\"${element.replace("\"", "\\\"")}\""
                        is Number, is Boolean -> element.toString()
                        else -> "\"${element.toString().replace("\"", "\\\"")}\""
                    }
                }
            "[${elements.joinToString(",")}]"
        }
    }

    private fun serializeMap(map: Map<*, *>): String {
        return try {
            // Try to serialize map entries properly
            val jsonEntries =
                map.entries.map { (key, value) ->
                    val keyStr = json.encodeToString(String.serializer(), key.toString())
                    val valueStr =
                        when (value) {
                            null -> "null"
                            is String -> json.encodeToString(String.serializer(), value)
                            is Number -> value.toString()
                            is Boolean -> value.toString()
                            else -> {
                                // Try kotlinx serialization first, fallback to quoted string
                                serializeWithKotlinx(value) ?: "\"${value.toString().replace("\"", "\\\"")}\""
                            }
                        }
                    "$keyStr:$valueStr"
                }
            "{${jsonEntries.joinToString(",")}}"
        } catch (e: Exception) {
            // Fallback to simple serialization
            val entries =
                map.entries.map { (key, value) ->
                    val keyStr = "\"${key.toString().replace("\"", "\\\"")}\""
                    val valueStr =
                        when (value) {
                            null -> "null"
                            is String -> "\"${value.replace("\"", "\\\"")}\""
                            is Number, is Boolean -> value.toString()
                            else -> "\"${value.toString().replace("\"", "\\\"")}\""
                        }
                    "$keyStr:$valueStr"
                }
            "{${entries.joinToString(",")}}"
        }
    }

    private fun serializeArray(array: Array<*>): String {
        return serializeCollection(array.toList())
    }

    private fun createFallbackJson(value: Any): String {
        return try {
            val kotlinClass = value::class
            if (kotlinClass.isData) {
                // For data classes, try to parse toString() format
                createDataClassJson(value)
            } else {
                // Regular class - create a simple JSON representation
                createObjectJson(value)
            }
        } catch (e: Exception) {
            createObjectJson(value)
        }
    }

    private fun createDataClassJson(value: Any): String {
        return try {
            val className = value.javaClass.simpleName
            val toString = value.toString()

            // For data classes, toString() returns: ClassName(prop1=value1, prop2=value2, ...)
            // Convert this to proper JSON format
            if (toString.startsWith("$className(") && toString.endsWith(")")) {
                val content = toString.substring(className.length + 1, toString.length - 1)
                val properties = parseDataClassProperties(content)

                val jsonProperties =
                    properties.joinToString(",") { (key, value) ->
                        "\"$key\":${formatJsonValue(value)}"
                    }

                "{$jsonProperties}"
            } else {
                // Fallback to quoted string
                "\"$toString\""
            }
        } catch (e: Exception) {
            "\"$value\""
        }
    }

    private fun parseDataClassProperties(content: String): List<Pair<String, String>> {
        val properties = mutableListOf<Pair<String, String>>()
        val current = StringBuilder()
        var key: String? = null
        var depth = 0
        var inString = false

        for (char in content) {
            when {
                char == '"' && (current.isEmpty() || current.last() != '\\') -> {
                    inString = !inString
                    current.append(char)
                }
                inString -> {
                    current.append(char)
                }
                char == '(' || char == '[' || char == '{' -> {
                    depth++
                    current.append(char)
                }
                char == ')' || char == ']' || char == '}' -> {
                    depth--
                    current.append(char)
                }
                char == '=' && depth == 0 && key == null -> {
                    key = current.toString().trim()
                    current.clear()
                }
                char == ',' && depth == 0 -> {
                    key?.let {
                        properties.add(it to current.toString().trim())
                        key = null
                        current.clear()
                    }
                }
                else -> {
                    current.append(char)
                }
            }
        }

        // Add the last property
        key?.let {
            properties.add(it to current.toString().trim())
        }

        return properties
    }

    private fun formatJsonValue(value: String): String {
        return when {
            value == "null" -> "null"
            value == "true" || value == "false" -> value
            value.toLongOrNull() != null -> value
            value.toDoubleOrNull() != null -> value
            value.startsWith("\"") && value.endsWith("\"") -> value
            else -> "\"$value\""
        }
    }

    private fun createObjectJson(value: Any): String {
        // For regular classes, create a simple JSON object with class name and toString
        val className = value.javaClass.simpleName
        val toString = value.toString().replace("\"", "\\\"")
        return "{\"_class\":\"$className\",\"_value\":\"$toString\"}"
    }
}
