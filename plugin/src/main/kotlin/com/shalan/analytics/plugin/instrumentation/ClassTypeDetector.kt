package com.shalan.analytics.plugin.instrumentation

import com.shalan.analytics.plugin.utils.PluginLogger

/**
 * Detects whether a class is an Activity, Fragment, or other type.
 *
 * This class encapsulates the logic for determining class types, making it:
 * - Easier to test
 * - Easier to extend (e.g., for custom base classes)
 * - Isolated from the main visitor logic
 *
 * Future enhancement: Use the ClassContext to check the full inheritance hierarchy
 * rather than just immediate superclass.
 */
class ClassTypeDetector(private val debugMode: Boolean = false) {
    /**
     * Represents the type of a class for analytics tracking purposes.
     */
    sealed class ClassType {
        object Activity : ClassType() {
            override fun toString() = "Activity"
        }

        object Fragment : ClassType() {
            override fun toString() = "Fragment"
        }

        object Other : ClassType() {
            override fun toString() = "Other"
        }

        val isActivity: Boolean get() = this is Activity
        val isFragment: Boolean get() = this is Fragment
    }

    /**
     * Detects the class type based on its superclass.
     *
     * @param superName The internal name of the superclass (e.g., "androidx/appcompat/app/AppCompatActivity")
     * @return The detected ClassType
     */
    fun detectClassType(superName: String?): ClassType {
        return when (superName) {
            in AnalyticsConstants.ActivityClasses.ALL -> {
                log("Detected Activity class with superclass: $superName")
                ClassType.Activity
            }
            in AnalyticsConstants.FragmentClasses.ALL -> {
                log("Detected Fragment class with superclass: $superName")
                ClassType.Fragment
            }
            else -> {
                if (superName != null && superName !in AnalyticsConstants.SystemPackages.ALL) {
                    log("Unknown superclass type: $superName (may be custom Activity/Fragment)")
                }
                ClassType.Other
            }
        }
    }

    /**
     * Checks if a class is an Activity based on its superclass.
     */
    fun isActivity(superName: String?): Boolean = detectClassType(superName).isActivity

    /**
     * Checks if a class is a Fragment based on its superclass.
     */
    fun isFragment(superName: String?): Boolean = detectClassType(superName).isFragment

    private fun log(message: String) {
        if (debugMode) {
            PluginLogger.debug("ClassTypeDetector: $message")
        }
    }
}
