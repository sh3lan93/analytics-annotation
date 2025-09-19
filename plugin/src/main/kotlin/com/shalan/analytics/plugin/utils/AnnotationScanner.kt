package com.shalan.analytics.plugin.utils

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

data class TrackingAnnotationInfo(
    val screenName: String,
    val screenClass: String? = null,
    val annotationType: AnnotationType,
    val className: String,
) {
    enum class AnnotationType {
        TRACK_SCREEN, // For Activities and Fragments
        TRACK_SCREEN_COMPOSABLE, // For Composables
    }
}

object AnnotationScanner {
    private const val TRACK_SCREEN_ANNOTATION = "Lcom/shalan/analytics/annotation/TrackScreen;"
    private const val TRACK_SCREEN_COMPOSABLE_ANNOTATION = "Lcom/shalan/analytics/compose/TrackScreenComposable;"

    fun scanClass(classBytes: ByteArray): TrackingAnnotationInfo? {
        return try {
            val classReader = ClassReader(classBytes)
            val visitor = AnnotationScannerVisitor()
            classReader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            visitor.annotationInfo
        } catch (e: Exception) {
            // Handle invalid bytecode gracefully
            null
        }
    }

    private class AnnotationScannerVisitor : ClassVisitor(Opcodes.ASM9) {
        var annotationInfo: TrackingAnnotationInfo? = null
        private var className: String = ""

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?,
        ) {
            className = name.replace('/', '.')
        }

        override fun visitAnnotation(
            descriptor: String,
            visible: Boolean,
        ): AnnotationVisitor? {
            return when (descriptor) {
                TRACK_SCREEN_ANNOTATION -> {
                    TrackScreenAnnotationVisitor(
                        TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN,
                    )
                }
                TRACK_SCREEN_COMPOSABLE_ANNOTATION -> {
                    TrackScreenAnnotationVisitor(
                        TrackingAnnotationInfo.AnnotationType.TRACK_SCREEN_COMPOSABLE,
                    )
                }
                else -> null
            }
        }

        private inner class TrackScreenAnnotationVisitor(
            private val annotationType: TrackingAnnotationInfo.AnnotationType,
        ) : AnnotationVisitor(Opcodes.ASM9) {
            private var screenName: String = ""
            private var screenClass: String? = null

            override fun visit(
                name: String?,
                value: Any?,
            ) {
                when (name) {
                    "value", "screenName" -> screenName = value as String
                    "screenClass" -> screenClass = value as String
                }
            }

            override fun visitEnd() {
                // Extract screen name from class name if not provided
                val finalScreenName =
                    screenName.ifEmpty {
                        className.substringAfterLast('.')
                    }

                val finalScreenClass = screenClass ?: className.substringAfterLast('.')

                if (finalScreenName.isNotEmpty()) {
                    annotationInfo =
                        TrackingAnnotationInfo(
                            screenName = finalScreenName,
                            screenClass = finalScreenClass,
                            annotationType = annotationType,
                            className = className,
                        )
                }
            }
        }
    }
}
