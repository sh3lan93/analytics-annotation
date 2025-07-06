package com.shalan.analytics.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.shalan.analytics.annotation.TrackScreen

/**
 * An implementation of [FragmentManager.FragmentLifecycleCallbacks] that automatically tracks
 * screen view events for Fragments annotated with [TrackScreen].
 * This class is responsible for checking for the [TrackScreen] annotation, extracting
 * screen information and additional parameters, and dispatching them to the [AnalyticsManager].
 *
 * @param analyticsManager The [AnalyticsManager] instance to which screen view events will be sent.
 */
class ScreenTrackingFragmentLifecycleCallbacks(
    private val analyticsManager: AnalyticsManager,
) : FragmentManager.FragmentLifecycleCallbacks() {
    /**
     * A cache to store [TrackScreen] annotations for Fragment classes.
     * This prevents redundant reflection calls for already processed Fragments.
     */
    companion object {
        private val annotationCache = mutableMapOf<Class<*>, TrackScreen?>()
    }

    /**
     * Called after a fragment's view has been created.
     * This is where the screen tracking logic is initiated for Fragments.
     * It checks for the [TrackScreen] annotation, retrieves screen details and optional
     * dynamic parameters from [TrackedScreenParamsProvider] (if implemented by the Fragment),
     * and logs the screen view event.
     *
     * @param fm The FragmentManager which this fragment is associated with.
     * @param f The Fragment whose view has been created.
     * @param v The view that was created.
     * @param savedInstanceState If the fragment is being re-created from a previous
     *     saved state, this is the state.
     */
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        val clazz = f::class.java
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
            if (f is TrackedScreenParamsProvider) {
                f.getTrackedScreenParams()
            } else {
                emptyMap()
            }

        analyticsManager.logScreenView(screenName, screenClass, providedParams)
    }
}
