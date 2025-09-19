package com.shalan.analytics.compose

/**
 * Annotation to mark Composable functions for automatic screen tracking in analytics.
 *
 * This annotation should ONLY be applied to functions that are also annotated with @Composable.
 * When applied to a Composable function, it will automatically log a screen view event
 * with the specified screen name when the Composable is first composed.
 *
 * @property screenName The name of the screen to be logged in analytics. This is a required field.
 *
 * ## Usage
 * ```kotlin
 * @TrackScreenComposable(screenName = "User Profile Screen")
 * @Composable
 * fun UserProfileScreen() {
 *     // Composable content
 *     // Analytics screen tracking is automatically injected
 * }
 * ```
 *
 * ## Important Notes
 * - This annotation is designed exclusively for @Composable functions
 * - Do not use this annotation on regular functions - use @Track instead
 * - The analytics plugin will only process this annotation on Composable functions
 * - This is a compile-time annotation that injects tracking code automatically
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackScreenComposable(val screenName: String)
