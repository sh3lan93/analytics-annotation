package com.shalan.analytics.annotation

/**
 * Annotation to mark an Activity or Fragment for automatic screen tracking.
 * When applied, the [ScreenTrackingCallbacks] will automatically log a screen view event
 * when the annotated component is created.
 *
 * @property screenName The name of the screen to be logged in analytics. This is a required field.
 * @property screenClass An optional custom class name for the screen. If not provided,
 *   the simple name of the annotated class will be used.
 * @property additionalParams An optional array of strings representing keys for additional
 *   dynamic parameters to be included with the screen view event. The values for these keys
 *   should be provided by implementing the [TrackedScreenParamsProvider] interface on the
 *   annotated Activity or Fragment.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackScreen(
    val screenName: String,
    val screenClass: String = "",
    val additionalParams: Array<String> = [],
)
