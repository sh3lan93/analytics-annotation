package com.shalan.analytics.annotation

/**
 * Marks a class as containing methods with @Track annotations.
 *
 * This annotation provides explicit opt-in behavior for method-level tracking,
 * allowing the analytics plugin to efficiently identify which classes contain
 * @Track annotated methods without scanning all classes in the project.
 *
 * Classes marked with @Trackable will be processed by the analytics plugin
 * to instrument their @Track annotated methods.
 *
 * ## Usage
 * ```kotlin
 * @Trackable
 * class MyViewModel : ViewModel() {
 *     @Track(eventName = "user_action")
 *     fun performAction(@Param("action_id") actionId: String) {
 *         // Method implementation
 *     }
 * }
 * ```
 *
 * ## Note
 * - This annotation is only needed for classes that contain @Track methods
 * - Activities and Fragments with @TrackScreen don't need @Trackable
 * - Composable functions with @TrackScreenComposable don't need @Trackable
 * - This is a compile-time annotation and has no runtime overhead
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Trackable
