package com.shalan.analytics.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.shalan.analytics.annotation.TrackScreen
import java.util.Stack

/**
 * An implementation of [Application.ActivityLifecycleCallbacks] that automatically tracks
 * screen view events for Activities annotated with [TrackScreen].
 * This class is responsible for checking for the [TrackScreen] annotation, extracting
 * screen information and additional parameters, and dispatching them to the [AnalyticsManager].
 * It also tracks the screen flow to provide context about navigation paths.
 *
 * @param analyticsManager The [AnalyticsManager] instance to which screen view events will be sent.
 */
class ScreenTrackingCallbacks(
    private val analyticsManager: AnalyticsManager,
) : Application.ActivityLifecycleCallbacks {
    /**
     * A cache to store [TrackScreen] annotations for Activity classes.
     * This prevents redundant reflection calls for already processed Activities.
     */
    companion object {
        private val annotationCache = mutableMapOf<Class<*>, TrackScreen?>()
        internal val activityStack = Stack<Activity>()
        private var activityCount = 0
    }

    /**
     * Called when an Activity is created. This is where the screen tracking logic is initiated.
     * It checks for the [TrackScreen] annotation, retrieves screen details and optional
     * dynamic parameters from [TrackedScreenParamsProvider] (if implemented by the Activity),
     * and logs the screen view event.
     *
     * @param activity The Activity that was created.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most recently
     *     supplied in [onActivitySaveInstanceState].
     */
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        activityCount++
        val clazz = activity::class.java
        val annotation =
            annotationCache.getOrPut(clazz) {
                clazz.getAnnotation(TrackScreen::class.java)
            } ?: return

        val screenName = annotation.screenName
        val screenClass =
            annotation.screenClass.ifEmpty {
                clazz.simpleName
            }

        val providedParams =
            if (activity is TrackedScreenParamsProvider) {
                activity.getTrackedScreenParams()
            } else {
                emptyMap()
            }

        val filteredParams = mutableMapOf<String, Any>()
        for (key in annotation.additionalParams) {
            providedParams[key]?.let { value ->
                filteredParams[key] = value
            }
        }

        val previousScreenName = if (activityStack.isNotEmpty()) activityStack.peek().javaClass.simpleName else ""
        val previousScreenClass = if (activityStack.isNotEmpty()) activityStack.peek().javaClass.name else ""
        val navigationType = if (activityStack.isNotEmpty()) "forward" else "initial"

        val screenFlowParams =
            mapOf(
                "previous_screen_name" to previousScreenName,
                "previous_screen_class" to previousScreenClass,
                "navigation_type" to navigationType,
            )

        analyticsManager.logScreenView(screenName, screenClass, filteredParams + screenFlowParams)
    }

    /**
     * Called when an Activity is started. Adds the activity to the stack.
     * @param activity The Activity that was started.
     */
    override fun onActivityStarted(activity: Activity) {
        activityStack.push(activity)
    }

    /**
     * Called when an Activity is resumed.
     * @param activity The Activity that was resumed.
     */
    override fun onActivityResumed(activity: Activity) {}

    /**
     * Called when an Activity is paused.
     * @param activity The Activity that was paused.
     */
    override fun onActivityPaused(activity: Activity) {}

    /**
     * Called when an Activity is stopped. Removes the activity from the stack.
     * @param activity The Activity that was stopped.
     */
    override fun onActivityStopped(activity: Activity) {
        if (activityStack.isNotEmpty() && activityStack.peek() == activity) {
            activityStack.pop()
        } else {
            // Handle cases where activity might be stopped out of order (e.g., due to system killing process)
            activityStack.remove(activity)
        }
    }

    /**
     * Called when an Activity is about to be saved.
     * @param activity The Activity that was saved.
     * @param outState Bundle in which to place your saved state.
     */
    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    /**
     * Called when an Activity is destroyed.
     * @param activity The Activity that was destroyed.
     */
    override fun onActivityDestroyed(activity: Activity) {
        activityCount--
        if (activityCount == 0 && activity.isFinishing) {
            analyticsManager.release()
        }
    }
}
