package com.shalan.analytics.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shalan.analytics.annotation.TrackScreen

class ScreenTrackingLifecycleObserver(
    private val target: Any,
    private val analyticsManager: AnalyticsManager,
) : DefaultLifecycleObserver {
    companion object {
        private val annotationCache = mutableMapOf<Class<*>, TrackScreen?>()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        trackScreen()
    }

    private fun trackScreen() {
        val clazz = target::class.java
        val annotation =
            annotationCache.getOrPut(clazz) {
                clazz.getAnnotation(TrackScreen::class.java)
            } ?: return

        val screenName = annotation.screenName
        val screenClass =
            if (annotation.screenClass.isNotEmpty()) {
                annotation.screenClass
            } else {
                clazz.simpleName
            }
        analyticsManager.logScreenView(screenName, screenClass)
    }
}
