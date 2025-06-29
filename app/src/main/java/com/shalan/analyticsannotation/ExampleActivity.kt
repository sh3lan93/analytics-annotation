package com.shalan.analyticsannotation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shalan.analytics.annotation.TrackScreen

@TrackScreen(screenName = "Example Activity")
class ExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment())
                .commit()
        }
    }
}
