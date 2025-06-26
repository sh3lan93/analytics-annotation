package com.shalan.analyticsannotation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen
import com.shalan.analytics.core.TrackedScreenParamsProvider

@TrackScreen(screenName = "Main Screen", additionalParams = ["user_id", "user_name"])
class MainActivity : AppCompatActivity(), TrackedScreenParamsProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun getTrackedScreenParams(): Map<String, Any> {
        // Provide dynamic parameters here
        return mapOf(
            "user_id" to "12345",
            "user_name" to "John Doe",
        )
    }
}
