package com.shalan.analytics.plugin.instrumentation

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * MethodVisitor that injects tracking calls after lifecycle method super calls.
 *
 * This visitor monitors method bytecode to detect when Activity.onCreate() or
 * Fragment.onViewCreated() calls their super implementations. Immediately after
 * the super call, it injects a call to __injectAnalyticsTracking().
 *
 * ## Why This Approach?
 *
 * - Injection happens AFTER super, ensuring parent initialization is complete
 * - Injection happens BEFORE user code in the method
 * - Non-invasive: doesn't modify existing instructions
 * - Works with any method implementation
 *
 * ## Bytecode Transformation Example
 *
 * **Before:**
 * ```
 * super.onCreate(bundle)
 * setContentView(R.layout.main)
 * ```
 *
 * **After:**
 * ```
 * super.onCreate(bundle)
 * __injectAnalyticsTracking()  ← Injected here
 * setContentView(R.layout.main)
 * ```
 */
class LifecycleInstrumentingMethodVisitor(
    api: Int,
    private val delegate: MethodVisitor,
    private val methodName: String,
    private val internalClassName: String,
    private val logger: TrackingLogger,
) : MethodVisitor(api, delegate) {
    private var superCallInjected = false

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean,
    ) {
        // Delegate the original method call
        delegate.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

        // After the super call, inject our tracking call
        if (!superCallInjected && shouldInjectAfterThisCall(opcode, name, descriptor)) {
            superCallInjected = true
            logger.debug { "Detected lifecycle super call in $methodName - injecting tracking" }
            injectTrackingCall()
        }
    }

    override fun visitInsn(opcode: Int) {
        delegate.visitInsn(opcode)
    }

    /**
     * Determines if we should inject tracking after this specific method call.
     */
    private fun shouldInjectAfterThisCall(
        opcode: Int,
        methodName: String?,
        descriptor: String?,
    ): Boolean {
        // Only inject after INVOKESPECIAL (super calls)
        if (opcode != Opcodes.INVOKESPECIAL) return false

        // Activity onCreate: inject after super.onCreate()
        if (this.methodName == AnalyticsConstants.LifecycleMethods.Activity.METHOD_NAME &&
            methodName == AnalyticsConstants.LifecycleMethods.Activity.METHOD_NAME &&
            descriptor == AnalyticsConstants.LifecycleMethods.Activity.DESCRIPTOR
        ) {
            return true
        }

        // Fragment onViewCreated: inject after super.onViewCreated()
        if (this.methodName == AnalyticsConstants.LifecycleMethods.Fragment.METHOD_NAME &&
            methodName == AnalyticsConstants.LifecycleMethods.Fragment.METHOD_NAME &&
            descriptor == AnalyticsConstants.LifecycleMethods.Fragment.DESCRIPTOR
        ) {
            return true
        }

        return false
    }

    /**
     * Injects bytecode to call __injectAnalyticsTracking().
     *
     * Generates:
     * ```
     * this.__injectAnalyticsTracking()
     * ```
     */
    private fun injectTrackingCall() {
        // Load 'this' reference (local variable 0)
        delegate.visitVarInsn(Opcodes.ALOAD, 0)

        // Call __injectAnalyticsTracking() on this
        delegate.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            internalClassName,
            AnalyticsConstants.InjectedMethod.NAME,
            AnalyticsConstants.InjectedMethod.DESCRIPTOR,
            false,
        )

        logger.debug { "Injected call to ${AnalyticsConstants.InjectedMethod.NAME} in $methodName" }
    }
}
