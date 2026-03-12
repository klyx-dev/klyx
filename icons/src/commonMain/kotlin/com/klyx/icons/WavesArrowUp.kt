package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.WavesArrowUp: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "WavesArrowUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            verticalLineToRelative(8f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 15f)
            curveToRelative(0.6f, 0.5f, 1.2f, 1f, 2.5f, 1f)
            curveToRelative(2.5f, 0f, 2.5f, -2f, 5f, -2f)
            curveToRelative(2.6f, 0f, 2.4f, 2f, 5f, 2f)
            curveToRelative(2.5f, 0f, 2.5f, -2f, 5f, -2f)
            curveToRelative(1.3f, 0f, 1.9f, 0.5f, 2.5f, 1f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 21f)
            curveToRelative(0.6f, 0.5f, 1.2f, 1f, 2.5f, 1f)
            curveToRelative(2.5f, 0f, 2.5f, -2f, 5f, -2f)
            curveToRelative(2.6f, 0f, 2.4f, 2f, 5f, 2f)
            curveToRelative(2.5f, 0f, 2.5f, -2f, 5f, -2f)
            curveToRelative(1.3f, 0f, 1.9f, 0.5f, 2.5f, 1f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveToRelative(8f, 6f)
            lineToRelative(4f, -4f)
            lineToRelative(4f, 4f)
        }
    }.build()
}
