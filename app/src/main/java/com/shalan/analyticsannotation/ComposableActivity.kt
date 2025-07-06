package com.shalan.analyticsannotation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen

@TrackScreen(screenName = "ComposableScreen")
class ComposableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExampleComposableScreen()
        }
    }
}
