package com.klyx.core.icon.language

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.KlyxIcons

val KlyxIcons.Language.Html: ImageVector
    get() {
        if (_Html != null) {
            return _Html!!
        }
        _Html = ImageVector.Builder(
            name = "Html",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(1.5f, 0f)
                horizontalLineToRelative(21f)
                lineToRelative(-1.91f, 21.563f)
                lineTo(11.977f, 24f)
                lineToRelative(-8.564f, -2.438f)
                lineTo(1.5f, 0f)
                close()
                moveTo(8.531f, 9.75f)
                lineToRelative(-0.232f, -2.718f)
                lineToRelative(10.059f, 0.003f)
                lineToRelative(0.23f, -2.622f)
                lineTo(5.412f, 4.41f)
                lineToRelative(0.698f, 8.01f)
                horizontalLineToRelative(9.126f)
                lineToRelative(-0.326f, 3.426f)
                lineToRelative(-2.91f, 0.804f)
                lineToRelative(-2.955f, -0.81f)
                lineToRelative(-0.188f, -2.11f)
                lineTo(6.248f, 13.73f)
                lineToRelative(0.33f, 4.171f)
                lineTo(12f, 19.351f)
                lineToRelative(5.379f, -1.443f)
                lineToRelative(0.744f, -8.157f)
                lineTo(8.531f, 9.751f)
                close()
            }
        }.build()

        return _Html!!
    }

@Suppress("ObjectPropertyName")
private var _Html: ImageVector? = null
