package com.shalan.analytics.annotation

/**
 * Annotation to mark methods for automatic tracking in analytics.
 * When applied to a method, it will automatically log an event with the specified
 * event name and optional category when the method is invoked.
 *
 * Method parameters annotated with [@Param] will be automatically included
 * as event parameters.
 *
 * @property eventName The name of the event to be logged in analytics. This is a required field.
 * @property includeGlobalParams Whether to include global parameters (like user ID, session ID)
 *   in the event. Defaults to true.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Track(
    val eventName: String,
    val includeGlobalParams: Boolean = true,
)
