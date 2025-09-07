package com.shalan.analytics.annotation

/**
 * Annotation to mark method parameters for inclusion in analytics events.
 * When applied to a parameter of a method annotated with [@Track], the parameter's
 * value will be automatically included as an event parameter.
 *
 * @property name The name for the parameter in the analytics event.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Param(
    val name: String,
)
