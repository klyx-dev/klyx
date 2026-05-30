package com.klyx.ui.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
@JvmInline
value class ImageVectorOrPainter private constructor(
    @PublishedApi
    internal val value: Any
) {

    init {
        require(value is ImageVector || value is Painter) {
            "Value must be either an ImageVector or a Painter, but was ${value::class.simpleName}"
        }
    }

    val imageVector: ImageVector? get() = value as? ImageVector
    val painter: Painter? get() = value as? Painter

    @IgnorableReturnValue
    inline fun <T> fold(
        onVector: (ImageVector) -> T,
        onPainter: (Painter) -> T
    ): T {
        return when (value) {
            is ImageVector -> onVector(value)
            is Painter -> onPainter(value)
            else -> error("Unexpected type")
        }
    }

    companion object {
        fun from(imageVector: ImageVector): ImageVectorOrPainter = ImageVectorOrPainter(imageVector)
        fun from(painter: Painter): ImageVectorOrPainter = ImageVectorOrPainter(painter)
    }
}

val ImageVector.asImageVectorOrPainter get() = ImageVectorOrPainter.from(this)
val Painter.asImageVectorOrPainter get() = ImageVectorOrPainter.from(this)
