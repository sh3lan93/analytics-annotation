package com.shalan.analyticsannotation

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Custom IdlingResource for waiting on analytics events in instrumented tests.
 *
 * This allows Espresso to synchronize with analytics operations instead of using Thread.sleep().
 * Usage:
 * ```
 * val idlingResource = AnalyticsIdlingResource("EventLogged") { testProvider.getLoggedEvents().isNotEmpty() }
 * IdlingRegistry.getInstance().register(idlingResource)
 * // ... perform test operations ...
 * IdlingRegistry.getInstance().unregister(idlingResource)
 * ```
 */
class AnalyticsIdlingResource(
    private val name: String,
    private val isIdleCondition: () -> Boolean,
) : IdlingResource {
    private var callback: ResourceCallback? = null
    private val isIdle = AtomicBoolean(false)

    override fun getName(): String = name

    override fun isIdleNow(): Boolean {
        val idle = isIdleCondition()
        if (idle && !isIdle.getAndSet(true)) {
            callback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback?) {
        this.callback = callback
    }

    /**
     * Reset the idle state for reuse in multiple test operations.
     */
    fun reset() {
        isIdle.set(false)
    }
}
