package com.shalan.analytics.annotation

/**
 * Annotation to mark an Activity or Fragment for automatic screen tracking.
 * When applied, it will automatically log a screen view event with the provided screen name,
 * screen class, and additional parameters.
 *
 * @property screenName The name of the screen to be logged in analytics. This is a required field.
 * @property screenClass An optional custom class name for the screen. If not provided,
 *   the simple name of the annotated class will be used.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TrackScreen(
    val screenName: String,
    val screenClass: String = "",
)
