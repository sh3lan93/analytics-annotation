package com.shalan.analytics.compose

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackScreenComposable(val screenName: String)
