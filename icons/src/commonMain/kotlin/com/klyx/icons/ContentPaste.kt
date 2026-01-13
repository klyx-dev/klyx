package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.ContentPaste: ImageVector
    get() {
        if (_ContentPaste != null) {
            return _ContentPaste!!
        }
        _ContentPaste = ImageVector.Builder(
            name = "ContentPaste",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(200f, 840f)
                quadTo(167f, 840f, 143.5f, 816.5f)
                quadTo(120f, 793f, 120f, 760f)
                lineTo(120f, 200f)
                quadTo(120f, 167f, 143.5f, 143.5f)
                quadTo(167f, 120f, 200f, 120f)
                lineTo(367f, 120f)
                quadTo(378f, 85f, 410f, 62.5f)
                quadTo(442f, 40f, 480f, 40f)
                quadTo(520f, 40f, 551.5f, 62.5f)
                quadTo(583f, 85f, 594f, 120f)
                lineTo(760f, 120f)
                quadTo(793f, 120f, 816.5f, 143.5f)
                quadTo(840f, 167f, 840f, 200f)
                lineTo(840f, 760f)
                quadTo(840f, 793f, 816.5f, 816.5f)
                quadTo(793f, 840f, 760f, 840f)
                lineTo(200f, 840f)
                close()
                moveTo(200f, 760f)
                lineTo(760f, 760f)
                quadTo(760f, 760f, 760f, 760f)
                quadTo(760f, 760f, 760f, 760f)
                lineTo(760f, 200f)
                quadTo(760f, 200f, 760f, 200f)
                quadTo(760f, 200f, 760f, 200f)
                lineTo(680f, 200f)
                lineTo(680f, 320f)
                lineTo(280f, 320f)
                lineTo(280f, 200f)
                lineTo(200f, 200f)
                quadTo(200f, 200f, 200f, 200f)
                quadTo(200f, 200f, 200f, 200f)
                lineTo(200f, 760f)
                quadTo(200f, 760f, 200f, 760f)
                quadTo(200f, 760f, 200f, 760f)
                close()
                moveTo(480f, 200f)
                quadTo(497f, 200f, 508.5f, 188.5f)
                quadTo(520f, 177f, 520f, 160f)
                quadTo(520f, 143f, 508.5f, 131.5f)
                quadTo(497f, 120f, 480f, 120f)
                quadTo(463f, 120f, 451.5f, 131.5f)
                quadTo(440f, 143f, 440f, 160f)
                quadTo(440f, 177f, 451.5f, 188.5f)
                quadTo(463f, 200f, 480f, 200f)
                close()
            }
        }.build()

        return _ContentPaste!!
    }

@Suppress("ObjectPropertyName")
private var _ContentPaste: ImageVector? = null
