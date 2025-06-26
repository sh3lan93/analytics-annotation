package com.shalan.analytics.core

/**
 * An optional interface that Activities or Fragments can implement to provide dynamic
 * additional parameters for screen tracking events.
 * The keys for these parameters should correspond to the `additionalParams` specified
 * in the [TrackScreen] annotation.
 */
interface TrackedScreenParamsProvider {
    /**
     * Returns a map of key-value pairs representing additional dynamic parameters
     * to be included with the screen tracking event.
     * The keys in this map should match the strings provided in the `additionalParams`
     * array of the [TrackScreen] annotation.
     *
     * @return A [Map] where keys are parameter names (String) and values are their corresponding data (Any).
     */
    fun getTrackedScreenParams(): Map<String, Any>
}
