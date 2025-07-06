package com.shalan.analyticsannotation

import android.app.Application
import com.shalan.analytics.core.InMemoryDebugAnalyticsProvider
import com.shalan.analytics.core.ScreenTracking
import com.shalan.analytics.core.analyticsConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config =
                analyticsConfig {
                    debugMode = true
                    providers.add(InMemoryDebugAnalyticsProvider())
                },
        )
    }
}
