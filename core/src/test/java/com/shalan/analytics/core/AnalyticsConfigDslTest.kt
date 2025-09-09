package com.shalan.analytics.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsConfigDslTest {
    @Test
    fun `analyticsConfig DSL creates AnalyticsConfig with defaults`() {
        val config =
            analyticsConfig {
                // Empty block should use defaults
            }

        assertNotNull(config)
        assertTrue(config.providers.isEmpty())
        assertTrue(config.methodTracking.enabled)
    }

    @Test
    fun `analyticsConfig DSL allows provider configuration`() {
        val provider = InMemoryDebugAnalyticsProvider()

        val config =
            analyticsConfig {
                providers.add(provider)
            }

        assertEquals(1, config.providers.size)
        assertEquals(provider, config.providers[0])
    }

    @Test
    fun `analyticsConfig DSL allows multiple providers`() {
        val provider1 = InMemoryDebugAnalyticsProvider()
        val provider2 = InMemoryDebugAnalyticsProvider()

        val config =
            analyticsConfig {
                providers.add(provider1)
                providers.add(provider2)
            }

        assertEquals(2, config.providers.size)
        assertTrue(config.providers.contains(provider1))
        assertTrue(config.providers.contains(provider2))
    }

    @Test
    fun `methodTracking DSL extension works with AnalyticsConfig`() {
        val config =
            analyticsConfig {
                methodTracking {
                    enabled = false
                }
            }

        assertFalse(config.methodTracking.enabled)
    }

    @Test
    fun `methodTracking DSL extension allows error handler configuration`() {
        var capturedError: Throwable? = null
        val errorHandler: (Throwable) -> Unit = { capturedError = it }

        val config =
            analyticsConfig {
                methodTracking {
                    this.errorHandler = errorHandler
                }
            }

        assertEquals(errorHandler, config.methodTracking.errorHandler)

        // Test that the error handler works
        val testException = RuntimeException("test error")
        config.methodTracking.errorHandler?.invoke(testException)
        assertEquals(testException, capturedError)
    }

    @Test
    fun `methodTracking DSL extension allows custom serializer configuration`() {
        val mockSerializer =
            object : ParameterSerializer {
                override fun canSerialize(parameterType: Class<*>): Boolean = parameterType == String::class.java

                override fun serialize(
                    value: Any?,
                    parameterType: Class<*>,
                ): Any? = value?.toString()
            }

        val config =
            analyticsConfig {
                methodTracking {
                    customSerializers.add(mockSerializer)
                }
            }

        assertEquals(1, config.methodTracking.customSerializers.size)
        assertEquals(mockSerializer, config.methodTracking.customSerializers[0])
        assertTrue(config.methodTracking.customSerializers[0].canSerialize(String::class.java))
        assertEquals("test", config.methodTracking.customSerializers[0].serialize("test", String::class.java))
    }

    @Test
    fun `methodTracking DSL extension can be called multiple times`() {
        val config =
            analyticsConfig {
                methodTracking {
                    enabled = false
                }

                methodTracking {
                    // Second call should modify the same instance
                    // enabled should remain from first call
                }
            }

        assertFalse(config.methodTracking.enabled) // From first call
    }

    @Test
    fun `combined DSL configuration works properly`() {
        val provider = InMemoryDebugAnalyticsProvider()
        val errorHandler: (Throwable) -> Unit = { }
        val customSerializer =
            object : ParameterSerializer {
                override fun canSerialize(parameterType: Class<*>): Boolean = true

                override fun serialize(
                    value: Any?,
                    parameterType: Class<*>,
                ): Any? = "serialized"
            }

        val config =
            analyticsConfig {
                providers.add(provider)

                methodTracking {
                    enabled = true
                    this.errorHandler = errorHandler
                    customSerializers.add(customSerializer)
                }
            }

        // Verify provider configuration
        assertEquals(1, config.providers.size)
        assertEquals(provider, config.providers[0])

        // Verify method tracking configuration
        assertTrue(config.methodTracking.enabled)
        assertEquals(errorHandler, config.methodTracking.errorHandler)
        assertEquals(1, config.methodTracking.customSerializers.size)
        assertEquals(customSerializer, config.methodTracking.customSerializers[0])
    }

    @Test
    fun `methodTracking extension preserves existing config when not modified`() {
        val config = AnalyticsConfig()

        // Modify directly first
        config.methodTracking.enabled = false

        // Use DSL extension to modify only some properties
        config.methodTracking {
            // Don't change enabled
        }

        assertFalse(config.methodTracking.enabled) // Should remain false
    }
}
