package com.klyx.core.icon.language

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.KlyxIcons

val KlyxIcons.Language.Ruby: ImageVector
    get() {
        if (_Ruby != null) {
            return _Ruby!!
        }
        _Ruby = ImageVector.Builder(
            name = "Ruby",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.156f, 0.083f)
                curveToRelative(3.033f, 0.525f, 3.893f, 2.598f, 3.829f, 4.77f)
                lineTo(24f, 4.822f)
                lineTo(22.635f, 22.71f)
                lineTo(4.89f, 23.926f)
                horizontalLineToRelative(0.016f)
                curveTo(3.433f, 23.864f, 0.15f, 23.729f, 0f, 19.139f)
                lineToRelative(1.645f, -3f)
                lineToRelative(2.819f, 6.586f)
                lineToRelative(0.503f, 1.172f)
                lineToRelative(2.805f, -9.144f)
                lineToRelative(-0.03f, 0.007f)
                lineToRelative(0.016f, -0.03f)
                lineToRelative(9.255f, 2.956f)
                lineToRelative(-1.396f, -5.431f)
                lineToRelative(-0.99f, -3.9f)
                lineToRelative(8.82f, -0.569f)
                lineToRelative(-0.615f, -0.51f)
                lineTo(16.5f, 2.114f)
                lineTo(20.159f, 0.073f)
                lineToRelative(-0.003f, 0.01f)
                close()
                moveTo(0f, 19.089f)
                close()
                moveTo(5.13f, 5.073f)
                curveToRelative(3.561f, -3.533f, 8.157f, -5.621f, 9.922f, -3.84f)
                curveToRelative(1.762f, 1.777f, -0.105f, 6.105f, -3.673f, 9.636f)
                curveToRelative(-3.563f, 3.532f, -8.103f, 5.734f, -9.864f, 3.957f)
                curveToRelative(-1.766f, -1.777f, 0.045f, -6.217f, 3.612f, -9.75f)
                lineToRelative(0.003f, -0.003f)
                close()
            }
        }.build()

        return _Ruby!!
    }

@Suppress("ObjectPropertyName")
private var _Ruby: ImageVector? = null
