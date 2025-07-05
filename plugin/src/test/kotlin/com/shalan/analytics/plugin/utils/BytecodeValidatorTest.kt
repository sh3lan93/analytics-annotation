package com.shalan.analytics.plugin.utils

import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BytecodeValidatorTest {
    
    @Test
    fun `validateBytecode accepts valid bytecode`() {
        val validBytes = createValidClassBytecode("com/test/ValidClass")
        val result = BytecodeValidator.validateBytecode(validBytes, "ValidClass")
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `validateBytecode rejects invalid bytecode`() {
        val invalidBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03) // Not a valid class file
        val result = BytecodeValidator.validateBytecode(invalidBytes, "InvalidClass")
        
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
    }
    
    @Test
    fun `validateBytecode rejects empty bytecode`() {
        val emptyBytes = byteArrayOf()
        val result = BytecodeValidator.validateBytecode(emptyBytes, "EmptyClass")
        
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
    }
    
    @Test
    fun `validateBytecode warns about old Java versions`() {
        val oldVersionBytes = createClassBytecodeWithVersion("com/test/OldClass", Opcodes.V1_7)
        val result = BytecodeValidator.validateBytecode(oldVersionBytes, "OldClass")
        
        assertTrue(result.isValid) // Still valid, but should warn
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("Java version") })
    }
    
    @Test
    fun `validateBytecode accepts modern Java versions`() {
        val modernVersionBytes = createClassBytecodeWithVersion("com/test/ModernClass", Opcodes.V11)
        val result = BytecodeValidator.validateBytecode(modernVersionBytes, "ModernClass")
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test 
    fun `validateTransformation validates both original and transformed bytecode`() {
        val originalBytes = createValidClassBytecode("com/test/OriginalClass")
        val transformedBytes = createClassWithTrackingCall("com/test/OriginalClass")
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = originalBytes,
            transformedBytes = transformedBytes,
            className = "OriginalClass",
            expectedTrackingCalls = 1
        )
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `validateTransformation detects invalid original bytecode`() {
        val invalidOriginal = byteArrayOf(0x00, 0x01)
        val validTransformed = createValidClassBytecode("com/test/TestClass")
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = invalidOriginal,
            transformedBytes = validTransformed,
            className = "TestClass",
            expectedTrackingCalls = 0
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("original") })
    }
    
    @Test
    fun `validateTransformation detects invalid transformed bytecode`() {
        val validOriginal = createValidClassBytecode("com/test/TestClass")
        val invalidTransformed = byteArrayOf(0x00, 0x01)
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = validOriginal,
            transformedBytes = invalidTransformed,
            className = "TestClass",
            expectedTrackingCalls = 0
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("transformed") })
    }
    
    @Test
    fun `validateTransformation counts tracking calls correctly`() {
        val originalBytes = createValidClassBytecode("com/test/TestClass")
        val transformedWithCalls = createClassWithMultipleTrackingCalls("com/test/TestClass", 3)
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = originalBytes,
            transformedBytes = transformedWithCalls,
            className = "TestClass",
            expectedTrackingCalls = 3
        )
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `validateTransformation warns when tracking call count doesn't match expected`() {
        val originalBytes = createValidClassBytecode("com/test/TestClass")
        val transformedWithCalls = createClassWithMultipleTrackingCalls("com/test/TestClass", 1)
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = originalBytes,
            transformedBytes = transformedWithCalls,
            className = "TestClass",
            expectedTrackingCalls = 2 // Expect 2 but only have 1
        )
        
        assertTrue(result.isValid) // Still valid
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("Expected 2 tracking calls but found 1") })
    }
    
    @Test
    fun `validateTransformation detects missing tracking calls`() {
        val originalBytes = createValidClassBytecode("com/test/TestClass")
        val transformedWithoutCalls = createValidClassBytecode("com/test/TestClass") // No transformation
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = originalBytes,
            transformedBytes = transformedWithoutCalls,
            className = "TestClass",
            expectedTrackingCalls = 1
        )
        
        assertTrue(result.isValid) // Still valid bytecode
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("Expected 1 tracking calls but found 0") })
    }
    
    @Test
    fun `validateTransformation handles zero expected tracking calls`() {
        val originalBytes = createValidClassBytecode("com/test/TestClass")
        val transformedBytes = createValidClassBytecode("com/test/TestClass")
        
        val result = BytecodeValidator.validateTransformation(
            originalBytes = originalBytes,
            transformedBytes = transformedBytes,
            className = "TestClass",
            expectedTrackingCalls = 0
        )
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `ValidationResult provides correct information`() {
        val result = BytecodeValidator.ValidationResult(
            isValid = false,
            errors = listOf("Error 1", "Error 2"),
            warnings = listOf("Warning 1")
        )

        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(1, result.warnings.size)
    }
    
    private fun createValidClassBytecode(className: String): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        )
        
        // Add default constructor
        val constructorVisitor = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        constructorVisitor.visitCode()
        constructorVisitor.visitVarInsn(Opcodes.ALOAD, 0)
        constructorVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        )
        constructorVisitor.visitInsn(Opcodes.RETURN)
        constructorVisitor.visitMaxs(1, 1)
        constructorVisitor.visitEnd()
        
        classWriter.visitEnd()
        return classWriter.toByteArray()
    }
    
    private fun createClassBytecodeWithVersion(className: String, version: Int): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classWriter.visit(
            version,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        )
        
        // Add default constructor
        val constructorVisitor = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        constructorVisitor.visitCode()
        constructorVisitor.visitVarInsn(Opcodes.ALOAD, 0)
        constructorVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        )
        constructorVisitor.visitInsn(Opcodes.RETURN)
        constructorVisitor.visitMaxs(1, 1)
        constructorVisitor.visitEnd()
        
        classWriter.visitEnd()
        return classWriter.toByteArray()
    }
    
    private fun createClassWithTrackingCall(className: String): ByteArray {
        return createClassWithMultipleTrackingCalls(className, 1)
    }
    
    private fun createClassWithMultipleTrackingCalls(className: String, callCount: Int): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        )
        
        // Add method with tracking calls
        val methodVisitor = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "testMethod",
            "()V",
            null,
            null
        )
        methodVisitor.visitCode()
        
        // Add multiple tracking calls
        repeat(callCount) {
            // Call ScreenTracking.getManager()
            methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/shalan/analytics/core/ScreenTracking",
                "getManager",
                "()Lcom/shalan/analytics/core/AnalyticsManager;",
                false
            )
            methodVisitor.visitInsn(Opcodes.POP) // Pop the result
        }
        
        methodVisitor.visitInsn(Opcodes.RETURN)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()
        
        // Add default constructor  
        val constructorVisitor = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        constructorVisitor.visitCode()
        constructorVisitor.visitVarInsn(Opcodes.ALOAD, 0)
        constructorVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        )
        constructorVisitor.visitInsn(Opcodes.RETURN)
        constructorVisitor.visitMaxs(1, 1)
        constructorVisitor.visitEnd()
        
        classWriter.visitEnd()
        return classWriter.toByteArray()
    }
}