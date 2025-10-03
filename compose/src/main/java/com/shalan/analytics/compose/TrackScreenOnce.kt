package com.shalan.analytics.compose

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.shalan.analytics.core.ScreenTracking

/**
 * Internal composable function that tracks a screen view exactly once when the composable
 * enters the composition, preventing duplicate tracking on recomposition.
 *
 * This function is called by ASM-injected code for @TrackScreenComposable annotations.
 * It should not be called directly by library users.
 *
 * @param screenName The name of the screen to track
 * @param screenClass The class name of the screen
 * @param parameters Additional parameters to include with the tracking event
 *
 * ## Implementation Details
 * - Uses LaunchedEffect(Unit) to ensure the tracking call happens only once
 * - The Unit key means the effect will only run when the composable first enters composition
 * - Subsequent recompositions will not trigger the tracking call
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Composable
fun TrackScreenOnce(
    screenName: String,
    screenClass: String,
    parameters: Map<String, Any> = emptyMap(),
) {
    LaunchedEffect(Unit) {
        try {
            ScreenTracking.getManager().logScreenView(
                screenName = screenName,
                screenClass = screenClass,
                parameters = parameters,
            )
        } catch (e: Exception) {
            // Analytics should never crash the app
            // Silently ignore errors in tracking
        }
    }
}
