package com.shalan.analytics.plugin.instrumentation

/**
 * Data class to hold method tracking information extracted from @Track annotation.
 */
data class MethodTrackInfo(
    val eventName: String,
    val includeGlobalParams: Boolean,
    val parameters: List<ParamInfo> = emptyList(),
)

/**
 * Data class to hold parameter tracking information extracted from @Param annotation.
 */
data class ParamInfo(
    val name: String,
    val index: Int,
)
