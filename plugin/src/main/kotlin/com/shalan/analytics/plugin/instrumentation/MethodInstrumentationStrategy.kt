package com.shalan.analytics.plugin.instrumentation

/**
 * Decides which methods should be instrumented for analytics tracking.
 *
 * This class encapsulates the business logic for determining which methods need
 * tracking code injection, eliminating duplication and making it easy to extend
 * to support additional lifecycle methods.
 */
class MethodInstrumentationStrategy {
    /**
     * Represents a decision about whether and how to instrument a method.
     */
    sealed class InstrumentationDecision {
        /**
         * This method should be instrumented (e.g., onCreate, onViewCreated).
         */
        data class ShouldInstrument(val reason: String) : InstrumentationDecision()

        /**
         * This method should be collected but not instrumented yet.
         */
        data class ShouldCollect(val reason: String) : InstrumentationDecision()

        /**
         * This method doesn't need instrumentation.
         */
        object Skip : InstrumentationDecision() {
            override fun toString() = "Skip"
        }

        val shouldInstrument: Boolean get() = this is ShouldInstrument
        val shouldCollect: Boolean get() = this is ShouldCollect || this is ShouldInstrument
    }

    /**
     * Decides whether a method should be instrumented.
     *
     * @param methodName The name of the method
     * @param descriptor The JVM descriptor of the method
     * @param isActivity Whether the containing class is an Activity
     * @param isFragment Whether the containing class is a Fragment
     * @return A decision indicating what to do with this method
     */
    fun decide(
        methodName: String?,
        descriptor: String?,
        isActivity: Boolean,
        isFragment: Boolean,
    ): InstrumentationDecision {
        // Check for Activity lifecycle methods
        if (isActivity && methodName == AnalyticsConstants.LifecycleMethods.Activity.METHOD_NAME &&
            descriptor == AnalyticsConstants.LifecycleMethods.Activity.DESCRIPTOR
        ) {
            return InstrumentationDecision.ShouldInstrument("Activity.onCreate lifecycle method")
        }

        // Check for Fragment lifecycle methods
        if (isFragment && methodName == AnalyticsConstants.LifecycleMethods.Fragment.METHOD_NAME &&
            descriptor == AnalyticsConstants.LifecycleMethods.Fragment.DESCRIPTOR
        ) {
            return InstrumentationDecision.ShouldInstrument("Fragment.onViewCreated lifecycle method")
        }

        return InstrumentationDecision.Skip
    }

    /**
     * Gets a human-readable description of which methods will be instrumented.
     */
    fun describeSupportedMethods(): String {
        return """
            Supported lifecycle methods for instrumentation:
            - Activity: ${AnalyticsConstants.LifecycleMethods.Activity.METHOD_NAME}${AnalyticsConstants.LifecycleMethods.Activity.DESCRIPTOR}
            - Fragment: ${AnalyticsConstants.LifecycleMethods.Fragment.METHOD_NAME}${AnalyticsConstants.LifecycleMethods.Fragment.DESCRIPTOR}
            """.trimIndent()
    }
}
