package com.shalan.analyticsannotation.exclude

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen
import com.shalan.analyticsannotation.ExampleComposableScreen

@TrackScreen(screenName = "Excluded Activity")
class ExcludeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExampleComposableScreen()
        }
    }
}
