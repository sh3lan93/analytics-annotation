package com.shalan.analytics.plugin.utils

/**
 * Data class representing tracking annotation information extracted from bytecode.
 *
 * This class is used internally by the plugin to store metadata about @TrackScreen
 * annotations found during bytecode transformation.
 *
 * @property screenName The display name of the screen (e.g., "Home Screen", "Product Details")
 * @property screenClass Optional custom class name for grouping analytics (defaults to actual class name)
 * @property annotationType The type of tracking annotation found
 * @property className The fully qualified class name where the annotation was found
 */
data class TrackingAnnotationInfo(
    val screenName: String,
    val screenClass: String? = null,
    val annotationType: AnnotationType,
    val className: String,
) {
    /**
     * Enum representing the different types of screen tracking annotations.
     */
    enum class AnnotationType {
        /** Used for Activities and Fragments with @TrackScreen annotation */
        TRACK_SCREEN,
    }
}
