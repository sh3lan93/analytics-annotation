package com.shalan.analytics.plugin.utils

import org.junit.Test
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnnotationScannerTest {
    
    @Test
    fun `scanClass returns null for class without annotations`() {
        val classBytes = createClassBytecode("com/test/NoAnnotationClass")
        val result = AnnotationScanner.scanClass(classBytes)
        
        assertNull(result)
    }
    
    @Test
    fun `scanClass detects TrackScreen annotation with default values`() {
        val classBytes = createClassBytecodeWithTrackScreen(
            className = "com/test/TestActivity",
            screenName = null // Use default
        )
        val result = AnnotationScanner.scanClass(classBytes)
        
        assertNotNull(result)
        assertEquals(TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN, result.annotationType) 
        assertEquals("TestActivity", result.screenName) // Should extract from class name
        assertEquals("TestActivity", result.screenClass)
        assertEquals("com.test.TestActivity", result.className)
        assertTrue(result.additionalParams.isEmpty())
    }
    
    @Test
    fun `scanClass detects TrackScreen annotation with custom screenName`() {
        val classBytes = createClassBytecodeWithTrackScreen(
            className = "com/test/MainActivity",
            screenName = "Home Screen"
        )
        val result = AnnotationScanner.scanClass(classBytes)
        
        assertNotNull(result)
        assertEquals("Home Screen", result.screenName)
        assertEquals("MainActivity", result.screenClass)
        assertEquals("com.test.MainActivity", result.className)
    }
    
    @Test
    fun `scanClass detects TrackScreen annotation with custom screenClass`() {
        val classBytes = createClassBytecodeWithTrackScreen(
            className = "com/test/DetailActivity", 
            screenName = "Details",
            screenClass = "ProductDetails"
        )
        val result = AnnotationScanner.scanClass(classBytes)
        
        assertNotNull(result)
        assertEquals("Details", result.screenName)
        assertEquals("ProductDetails", result.screenClass)
        assertEquals("com.test.DetailActivity", result.className)
    }
    
    @Test
    fun `scanClass detects TrackScreen annotation with additional parameters`() {
        val classBytes = createClassBytecodeWithTrackScreen(
            className = "com/test/ParameterActivity",
            screenName = "Params",
            additionalParams = arrayOf("userId", "sessionId", "timestamp")
        )
        val result = AnnotationScanner.scanClass(classBytes)
        
        assertNotNull(result)
        assertEquals("Params", result.screenName)
        assertEquals(3, result.additionalParams.size)
        assertTrue(result.additionalParams.contains("userId"))
        assertTrue(result.additionalParams.contains("sessionId"))
        assertTrue(result.additionalParams.contains("timestamp"))
    }
    
    @Test
    fun `scanClass detects TrackScreenComposable annotation`() {
        val classBytes = createClassBytecodeWithTrackScreenComposable(
            className = "com/test/ComposableScreenKt",
            screenName = "Composable Screen"
        )
        val result = AnnotationScanner.scanClass(classBytes)
        
        assertNotNull(result)
        assertEquals(TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN_COMPOSABLE, result.annotationType)
        assertEquals("Composable Screen", result.screenName)
        assertEquals("ComposableScreenKt", result.screenClass)
        assertEquals("com.test.ComposableScreenKt", result.className)
    }
    
    @Test
    fun `scanClass handles class name extraction from different formats`() {
        val testCases = listOf(
            "com/test/SimpleActivity" to "SimpleActivity", 
            "com/app/ui/MainActivity" to "MainActivity",
            "MainActivity" to "MainActivity", // No package
            "com/test/DetailFragment" to "DetailFragment"
        )
        
        testCases.forEach { (className, expectedSimpleName) ->
            val classBytes = createClassBytecodeWithTrackScreen(className)
            val result = AnnotationScanner.scanClass(classBytes)
            
            assertNotNull(result, "Failed for className: $className")
            assertEquals(expectedSimpleName, result.screenClass)
            assertEquals(className.replace('/', '.'), result.className)
        }
    }
    
    @Test
    fun `scanClass handles invalid bytecode gracefully`() {
        val invalidBytes = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()) // Invalid class file
        val result = AnnotationScanner.scanClass(invalidBytes)
        
        assertNull(result) // Should handle gracefully
    }
    
    @Test
    fun `scanClass handles empty bytecode gracefully`() {
        val emptyBytes = byteArrayOf()
        val result = AnnotationScanner.scanClass(emptyBytes)
        
        assertNull(result)
    }
    
    private fun createClassBytecode(className: String): ByteArray {
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
    
    private fun createClassBytecodeWithTrackScreen(
        className: String,
        screenName: String? = null,
        screenClass: String? = null,
        additionalParams: Array<String>? = null
    ): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        )
        
        // Add @TrackScreen annotation
        val annotationVisitor = classWriter.visitAnnotation(
            "Lcom/shalan/analytics/annotation/TrackScreen;",
            true
        )
        
        screenName?.let { annotationVisitor.visit("value", it) }
        screenClass?.let { annotationVisitor.visit("screenClass", it) }
        additionalParams?.let { params ->
            val arrayVisitor = annotationVisitor.visitArray("additionalParams")
            params.forEach { param ->
                arrayVisitor.visit(null, param)
            }
            arrayVisitor.visitEnd()
        }
        
        annotationVisitor.visitEnd()
        
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
    
    private fun createClassBytecodeWithTrackScreenComposable(
        className: String,
        screenName: String? = null
    ): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        )
        
        // Add @TrackScreenComposable annotation
        val annotationVisitor = classWriter.visitAnnotation(
            "Lcom/shalan/analytics/compose/TrackScreenComposable;",
            true
        )
        
        screenName?.let { annotationVisitor.visit("value", it) }
        annotationVisitor.visitEnd()
        
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