package com.shalan.analyticsannotation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen
import com.shalan.analytics.core.TrackedScreenParamsProvider

@TrackScreen(screenName = "Main Screen")
class MainActivity : AppCompatActivity(), TrackedScreenParamsProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.example_button).setOnClickListener {
            startActivity(Intent(this, ExampleActivity::class.java))
        }

        findViewById<Button>(R.id.composable_button).setOnClickListener {
            startActivity(Intent(this, ComposableActivity::class.java))
        }
    }

    override fun getTrackedScreenParams(): Map<String, Any> {
        // Provide dynamic parameters here
        return mapOf(
            "user_id" to "12345",
            "user_name" to "John Doe",
        )
    }
}
