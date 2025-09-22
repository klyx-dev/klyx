package com.klyx.core.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KlyxIcons.Klyx: ImageVector
    get() {
        if (_Klyx != null) return _Klyx!!

        _Klyx = ImageVector.Builder(
            name = "Klyx",
            defaultWidth = 200.dp,
            defaultHeight = 200.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(753.5f, 247.6f)
                curveToRelative(-8.3f, 4.2f, -24.3f, 12.8f, -60f, 32.4f)
                curveToRelative(-22.8f, 12.5f, -157.9f, 87.3f, -258.5f, 143.1f)
                lineToRelative(-46.5f, 25.8f)
                lineToRelative(-0.3f, -96.5f)
                lineToRelative(-0.2f, -96.4f)
                lineToRelative(-59f, -0f)
                lineToRelative(-59f, -0f)
                lineToRelative(0f, 243.7f)
                lineToRelative(0f, 243.8f)
                lineToRelative(9.2f, -9.5f)
                curveToRelative(5.1f, -5.2f, 28.4f, -28.9f, 51.8f, -52.5f)
                curveToRelative(23.4f, -23.7f, 56.6f, -57.3f, 73.7f, -74.7f)
                lineToRelative(31.2f, -31.7f)
                lineToRelative(16.8f, 17.7f)
                curveToRelative(9.2f, 9.8f, 31f, 33f, 48.3f, 51.7f)
                curveToRelative(32.7f, 35.3f, 68f, 73.2f, 81f, 86.7f)
                lineToRelative(7.5f, 7.9f)
                lineToRelative(71.7f, -0.3f)
                lineToRelative(71.6f, -0.3f)
                lineToRelative(-46.5f, -49f)
                curveToRelative(-25.6f, -27f, -57.9f, -60.9f, -71.7f, -75.5f)
                curveToRelative(-13.9f, -14.6f, -36.9f, -38.8f, -51.2f, -53.8f)
                curveToRelative(-14.2f, -15.1f, -30.9f, -32.7f, -37.1f, -39.2f)
                lineToRelative(-11.2f, -11.8f)
                lineToRelative(2.1f, -1.8f)
                curveToRelative(4.9f, -4.4f, 120.7f, -127.9f, 174.7f, -186.4f)
                curveToRelative(60f, -65f, 70.9f, -77f, 69.5f, -77f)
                curveToRelative(-0.5f, 0.1f, -4f, 1.7f, -7.9f, 3.6f)
                close()
            }
        }.build()

        return _Klyx!!
    }

@Suppress("ObjectPropertyName")
private var _Klyx: ImageVector? = null
