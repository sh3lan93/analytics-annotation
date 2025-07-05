package com.shalan.analytics.plugin.utils

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Bytecode validation utility for ensuring transformed classes are valid.
 * Performs basic structural validation and detects common transformation issues.
 */
object BytecodeValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
    )

    /**
     * Validates that the provided bytecode is structurally valid and can be loaded by the JVM.
     */
    fun validateBytecode(
        classBytes: ByteArray,
        className: String,
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            // Basic structural validation using ASM
            val classReader = ClassReader(classBytes)
            val validationVisitor = ValidationVisitor(className, errors, warnings)
            classReader.accept(validationVisitor, ClassReader.SKIP_DEBUG)

            // Attempt to write the bytecode to catch any structural issues
            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
            classReader.accept(classWriter, 0)
            val rewritten = classWriter.toByteArray()

            // Verify the rewritten bytecode is readable
            ClassReader(rewritten)
        } catch (e: Exception) {
            errors.add("Bytecode validation failed: ${e.message}")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }

    /**
     * Validates that a transformation preserved the original class structure
     * while adding the expected tracking code.
     */
    fun validateTransformation(
        originalBytes: ByteArray,
        transformedBytes: ByteArray,
        className: String,
        expectedTrackingCalls: Int = 1,
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            // First validate both bytecode arrays are structurally valid
            val originalValidation = validateBytecode(originalBytes, className)
            val transformedValidation = validateBytecode(transformedBytes, className)

            if (!originalValidation.isValid) {
                errors.add("Original bytecode is invalid: ${originalValidation.errors}")
            }

            if (!transformedValidation.isValid) {
                errors.add("Transformed bytecode is invalid: ${transformedValidation.errors}")
                return ValidationResult(false, errors, warnings)
            }

            // Count tracking method calls in transformed bytecode
            val trackingCallCount = countTrackingCalls(transformedBytes)

            if (trackingCallCount == 0) {
                warnings.add("No tracking calls found in transformed bytecode")
            } else if (trackingCallCount != expectedTrackingCalls) {
                warnings.add("Expected $expectedTrackingCalls tracking calls, found $trackingCallCount")
            }

            // Verify class metadata is preserved
            val originalReader = ClassReader(originalBytes)
            val transformedReader = ClassReader(transformedBytes)

            if (originalReader.className != transformedReader.className) {
                errors.add("Class name changed during transformation")
            }

            if (originalReader.superName != transformedReader.superName) {
                errors.add("Superclass changed during transformation")
            }
        } catch (e: Exception) {
            errors.add("Transformation validation failed: ${e.message}")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }

    private fun countTrackingCalls(classBytes: ByteArray): Int {
        var callCount = 0
        val classReader = ClassReader(classBytes)

        classReader.accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor? {
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitMethodInsn(
                            opcode: Int,
                            owner: String,
                            name: String,
                            descriptor: String,
                            isInterface: Boolean,
                        ) {
                            if (owner == "com/shalan/analytics/core/ScreenTracking" &&
                                (name == "trackScreen" || name == "getManager")
                            ) {
                                callCount++
                            }
                        }
                    }
                }
            },
            ClassReader.SKIP_DEBUG,
        )

        return callCount
    }

    private class ValidationVisitor(
        private val className: String,
        private val errors: MutableList<String>,
        private val warnings: MutableList<String>,
    ) : ClassVisitor(Opcodes.ASM9) {
        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?,
        ) {
            // Validate class version
            if (version < Opcodes.V1_8) {
                warnings.add("Class compiled with Java version < 8")
            }

            // Validate class name matches expected
            if (name.replace('/', '.') != className) {
                errors.add("Class name mismatch: expected $className, found ${name.replace('/', '.')}")
            }
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor? {
            return object : MethodVisitor(Opcodes.ASM9) {
                private var maxStack = 0
                private var maxLocals = 0

                override fun visitMaxs(
                    maxStack: Int,
                    maxLocals: Int,
                ) {
                    this.maxStack = maxStack
                    this.maxLocals = maxLocals

                    // Basic sanity checks for method limits
                    if (maxStack > 65535) {
                        errors.add("Method $name has excessive stack size: $maxStack")
                    }

                    if (maxLocals > 65535) {
                        errors.add("Method $name has excessive local variables: $maxLocals")
                    }
                }
            }
        }
    }
}
