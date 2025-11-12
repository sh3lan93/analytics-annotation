package com.shalan.analyticsannotation

import android.app.Application
import android.util.Log
import com.shalan.analytics.core.InMemoryDebugAnalyticsProvider
import com.shalan.analytics.core.ScreenTracking
import com.shalan.analytics.core.analyticsConfig
import com.shalan.analytics.core.methodTracking

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        ScreenTracking.initialize(
            config =
                analyticsConfig {
                    debugMode = true
                    providers.add(InMemoryDebugAnalyticsProvider())

                    errorHandler = { throwable ->
                        Log.e("Analytics", "Method tracking error", throwable)
                    }
                    methodTracking {
                        enabled = true
                        // Register custom parameter serializers
                        customSerializers.add(UserProfileAnalyticsSerializer())
                        customSerializers.add(LimitedCollectionSerializer())
                        customSerializers.add(EnumAnalyticsSerializer())
                        customSerializers.add(PrivacyAwareSerializer())
                    }
                },
        )

        // Set global parameters that will be included in method tracking when includeGlobalParams=true
        ScreenTracking.setGlobalParameters(
            mapOf(
                "app_version" to "1.0.0",
                "user_id" to "demo_user_123",
                "session_id" to "session_${System.currentTimeMillis()}",
                "platform" to "Android",
            ),
        )
    }
}
