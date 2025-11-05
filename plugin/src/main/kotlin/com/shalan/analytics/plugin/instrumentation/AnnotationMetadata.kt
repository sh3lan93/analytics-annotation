package com.shalan.analytics.plugin.instrumentation

/**
 * Encapsulates metadata extracted from tracking annotations.
 *
 * This data class provides a type-safe way to pass annotation parameters around,
 * replacing the previous mutable map approach which was error-prone.
 *
 * @property screenName The name of the screen being tracked
 * @property screenClass Optional custom class name for analytics grouping
 * @property annotationParameters Additional parameters from the annotation
 */
data class AnnotationMetadata(
    val screenName: String? = null,
    val screenClass: String? = null,
    val annotationParameters: Map<String, Any> = emptyMap(),
) {
    /**
     * Returns the effective screen name.
     *
     * This can be used to apply defaults or transformations consistently.
     */
    fun effectiveScreenName(fallback: String): String = screenName ?: fallback

    /**
     * Returns the effective screen class.
     */
    fun effectiveScreenClass(fallback: String): String = screenClass?.takeIf { it.isNotEmpty() } ?: fallback

    /**
     * Checks if this metadata has sufficient information for instrumentation.
     */
    fun isComplete(): Boolean = screenName != null

    /**
     * Merges this metadata with another, with other values taking precedence.
     */
    fun mergeWith(other: AnnotationMetadata): AnnotationMetadata {
        return copy(
            screenName = other.screenName ?: this.screenName,
            screenClass = other.screenClass ?: this.screenClass,
            annotationParameters = this.annotationParameters + other.annotationParameters,
        )
    }

    companion object {
        /**
         * Creates empty metadata.
         */
        fun empty() = AnnotationMetadata()
    }
}
