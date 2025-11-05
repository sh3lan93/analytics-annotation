package com.shalan.analytics.plugin.instrumentation

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes

/**
 * Extracts metadata from @TrackScreen and @Trackable annotations.
 *
 * This class handles the visitor pattern for annotations, extracting parameters
 * in a type-safe manner and returning structured metadata.
 */
class AnnotationExtractor(
    private val logger: TrackingLogger,
) {
    /**
     * Creates an AnnotationVisitor that extracts @TrackScreen parameters.
     *
     * @param delegate The next visitor in the chain
     * @return An AnnotationVisitor that extracts tracking parameters
     */
    fun createTrackScreenVisitor(delegate: AnnotationVisitor?): AnnotationVisitor {
        return TrackScreenAnnotationVisitor(delegate)
    }

    /**
     * AnnotationVisitor for @TrackScreen annotations.
     *
     * Extracts the screenName and screenClass parameters.
     */
    private inner class TrackScreenAnnotationVisitor(
        private val delegate: AnnotationVisitor?,
    ) : AnnotationVisitor(Opcodes.ASM9, delegate) {
        private var extractedMetadata = AnnotationMetadata()

        override fun visit(
            name: String?,
            value: Any?,
        ) {
            when (name) {
                AnalyticsConstants.AnnotationParams.SCREEN_NAME_VALUE,
                AnalyticsConstants.AnnotationParams.SCREEN_NAME,
                -> {
                    extractedMetadata =
                        extractedMetadata.copy(
                            screenName = value as? String,
                        )
                    logger.debug { "Extracted screenName: $value" }
                }

                AnalyticsConstants.AnnotationParams.SCREEN_CLASS -> {
                    extractedMetadata =
                        extractedMetadata.copy(
                            screenClass = value as? String,
                        )
                    logger.debug { "Extracted screenClass: $value" }
                }

                else -> {
                    if (value != null && name != null) {
                        val newParams = extractedMetadata.annotationParameters.toMutableMap()
                        newParams[name] = value
                        extractedMetadata =
                            extractedMetadata.copy(
                                annotationParameters = newParams,
                            )
                    }
                }
            }
            delegate?.visit(name, value)
        }

        override fun visitEnd() {
            logger.debug { "Completed extracting @TrackScreen: $extractedMetadata" }
            delegate?.visitEnd()
        }

        fun getMetadata(): AnnotationMetadata = extractedMetadata
    }
}
