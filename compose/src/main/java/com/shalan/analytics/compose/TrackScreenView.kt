package com.shalan.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.shalan.analytics.core.ScreenTracking

@Composable
fun TrackScreenView(
    screenName: String,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) {
        ScreenTracking.getManager().logScreenView(screenName, screenName)
    }
    content()
}
