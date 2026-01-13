package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.KeyboardArrowDown: ImageVector
    get() {
        if (_KeyboardArrowDown != null) {
            return _KeyboardArrowDown!!
        }
        _KeyboardArrowDown = ImageVector.Builder(
            name = "KeyboardArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 616f)
                lineTo(240f, 376f)
                lineToRelative(56f, -56f)
                lineToRelative(184f, 184f)
                lineToRelative(184f, -184f)
                lineToRelative(56f, 56f)
                lineToRelative(-240f, 240f)
                close()
            }
        }.build()

        return _KeyboardArrowDown!!
    }

@Suppress("ObjectPropertyName")
private var _KeyboardArrowDown: ImageVector? = null
