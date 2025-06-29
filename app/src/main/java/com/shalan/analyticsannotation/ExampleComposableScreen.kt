package com.shalan.analyticsannotation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shalan.analytics.compose.TrackScreenComposable
import com.shalan.analytics.compose.TrackScreenView

@TrackScreenComposable(screenName = "Example Composable Screen")
@Composable
fun ExampleComposableScreen() {
    TrackScreenView(screenName = "Example Composable Screen") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "This is a composable screen")
        }
    }
}
