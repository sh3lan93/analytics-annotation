package com.shalan.analytics.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodTrackingConfigTest {
    @Test
    fun `MethodTrackingConfig has correct default values`() {
        val config = MethodTrackingConfig()

        assertTrue(config.enabled)
        assertTrue(config.customSerializers.isEmpty())
    }

    @Test
    fun `MethodTrackingConfig properties can be modified`() {
        val config = MethodTrackingConfig()

        config.enabled = false

        assertFalse(config.enabled)
    }

    @Test
    fun `MethodTrackingConfig customSerializers list is mutable`() {
        val config = MethodTrackingConfig()

        val mockSerializer =
            object : ParameterSerializer {
                override fun canSerialize(parameterType: Class<*>): Boolean = false

                override fun serialize(
                    value: Any?,
                    parameterType: Class<*>,
                ): Any? = null
            }

        config.customSerializers.add(mockSerializer)

        assertEquals(1, config.customSerializers.size)
        assertEquals(mockSerializer, config.customSerializers[0])
    }

    @Test
    fun `MethodTrackingConfig instances are independent`() {
        val config1 = MethodTrackingConfig()
        val config2 = MethodTrackingConfig()

        config1.enabled = false

        // config2 should maintain defaults
        assertTrue(config2.enabled)
    }

    @Test
    fun `AnalyticsConfig has MethodTrackingConfig property`() {
        val config = AnalyticsConfig()

        assertNotNull(config.methodTracking)
        assertTrue(config.methodTracking.enabled) // Should have default values
    }

    @Test
    fun `AnalyticsConfig methodTracking property can be modified`() {
        val config = AnalyticsConfig()

        config.methodTracking.enabled = false

        assertFalse(config.methodTracking.enabled)
    }

    @Test
    fun `AnalyticsConfig instances have independent MethodTrackingConfig`() {
        val config1 = AnalyticsConfig()
        val config2 = AnalyticsConfig()

        config1.methodTracking.enabled = false

        // config2 should maintain defaults
        assertTrue(config2.methodTracking.enabled)
    }
}
