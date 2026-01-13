package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.ContentCopy: ImageVector
    get() {
        if (_ContentCopy != null) {
            return _ContentCopy!!
        }
        _ContentCopy = ImageVector.Builder(
            name = "ContentCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(360f, 720f)
                quadTo(327f, 720f, 303.5f, 696.5f)
                quadTo(280f, 673f, 280f, 640f)
                lineTo(280f, 160f)
                quadTo(280f, 127f, 303.5f, 103.5f)
                quadTo(327f, 80f, 360f, 80f)
                lineTo(720f, 80f)
                quadTo(753f, 80f, 776.5f, 103.5f)
                quadTo(800f, 127f, 800f, 160f)
                lineTo(800f, 640f)
                quadTo(800f, 673f, 776.5f, 696.5f)
                quadTo(753f, 720f, 720f, 720f)
                lineTo(360f, 720f)
                close()
                moveTo(360f, 640f)
                lineTo(720f, 640f)
                quadTo(720f, 640f, 720f, 640f)
                quadTo(720f, 640f, 720f, 640f)
                lineTo(720f, 160f)
                quadTo(720f, 160f, 720f, 160f)
                quadTo(720f, 160f, 720f, 160f)
                lineTo(360f, 160f)
                quadTo(360f, 160f, 360f, 160f)
                quadTo(360f, 160f, 360f, 160f)
                lineTo(360f, 640f)
                quadTo(360f, 640f, 360f, 640f)
                quadTo(360f, 640f, 360f, 640f)
                close()
                moveTo(200f, 880f)
                quadTo(167f, 880f, 143.5f, 856.5f)
                quadTo(120f, 833f, 120f, 800f)
                lineTo(120f, 240f)
                lineTo(200f, 240f)
                lineTo(200f, 800f)
                quadTo(200f, 800f, 200f, 800f)
                quadTo(200f, 800f, 200f, 800f)
                lineTo(640f, 800f)
                lineTo(640f, 880f)
                lineTo(200f, 880f)
                close()
                moveTo(360f, 640f)
                quadTo(360f, 640f, 360f, 640f)
                quadTo(360f, 640f, 360f, 640f)
                lineTo(360f, 160f)
                quadTo(360f, 160f, 360f, 160f)
                quadTo(360f, 160f, 360f, 160f)
                lineTo(360f, 160f)
                quadTo(360f, 160f, 360f, 160f)
                quadTo(360f, 160f, 360f, 160f)
                lineTo(360f, 640f)
                quadTo(360f, 640f, 360f, 640f)
                quadTo(360f, 640f, 360f, 640f)
                close()
            }
        }.build()

        return _ContentCopy!!
    }

@Suppress("ObjectPropertyName")
private var _ContentCopy: ImageVector? = null
