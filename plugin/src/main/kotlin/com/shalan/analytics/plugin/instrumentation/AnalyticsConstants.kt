package com.shalan.analytics.plugin.instrumentation

/**
 * Central location for all constants used in bytecode instrumentation.
 *
 * This file eliminates magic strings and numbers scattered throughout the code,
 * making it easier to maintain and update descriptor formats or method names.
 */
object AnalyticsConstants {
    // ===== Class Type Detection =====
    object ActivityClasses {
        const val ANDROID_ACTIVITY = "android/app/Activity"
        const val ANDROIDX_APP_COMPAT = "androidx/appcompat/app/AppCompatActivity"
        const val ANDROIDX_FRAGMENT_ACTIVITY = "androidx/fragment/app/FragmentActivity"

        val ALL = listOf(ANDROID_ACTIVITY, ANDROIDX_APP_COMPAT, ANDROIDX_FRAGMENT_ACTIVITY)
    }

    object FragmentClasses {
        const val ANDROID_FRAGMENT = "android/app/Fragment"
        const val ANDROIDX_FRAGMENT = "androidx/fragment/app/Fragment"

        val ALL = listOf(ANDROID_FRAGMENT, ANDROIDX_FRAGMENT)
    }

    // ===== Annotation Descriptors =====
    object Annotations {
        const val TRACK_SCREEN = "Lcom/shalan/analytics/annotation/TrackScreen;"
        const val TRACKABLE = "Lcom/shalan/analytics/annotation/Trackable;"
    }

    // ===== Lifecycle Method Signatures =====
    object LifecycleMethods {
        object Activity {
            const val METHOD_NAME = "onCreate"
            const val DESCRIPTOR = "(Landroid/os/Bundle;)V"
        }

        object Fragment {
            const val METHOD_NAME = "onViewCreated"
            const val DESCRIPTOR = "(Landroid/view/View;Landroid/os/Bundle;)V"
        }
    }

    // ===== Injected Method =====
    object InjectedMethod {
        const val NAME = "__injectAnalyticsTracking"
        const val DESCRIPTOR = "()V"
    }

    // ===== Helper Method References =====
    object TrackScreenHelper {
        const val CLASS = "com/shalan/analytics/core/TrackScreenHelper"
        const val METHOD = "trackScreen"
        const val DESCRIPTOR = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V"
    }

    // ===== System Package Prefixes (for filtering) =====
    object SystemPackages {
        const val ANDROID = "android."
        const val ANDROIDX = "androidx."
        const val JAVA = "java."
        const val KOTLIN = "kotlin."

        val ALL = listOf(ANDROID, ANDROIDX, JAVA, KOTLIN)
    }

    // ===== Annotation Parameter Names =====
    object AnnotationParams {
        const val SCREEN_NAME_VALUE = "value"
        const val SCREEN_NAME = "screenName"
        const val SCREEN_CLASS = "screenClass"
    }

    // ===== ASM Stack Configuration =====
    object AsmConfig {
        const val STACK_SIZE = 3
        const val LOCALS_SIZE = 1
    }
}
