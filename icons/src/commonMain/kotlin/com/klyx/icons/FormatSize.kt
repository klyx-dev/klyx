package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FormatSize: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "FormatSize",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(560f, 800f)
            verticalLineToRelative(-520f)
            lineTo(360f, 280f)
            verticalLineToRelative(-120f)
            horizontalLineToRelative(520f)
            verticalLineToRelative(120f)
            lineTo(680f, 280f)
            verticalLineToRelative(520f)
            lineTo(560f, 800f)
            close()
            moveTo(200f, 800f)
            verticalLineToRelative(-320f)
            lineTo(80f, 480f)
            verticalLineToRelative(-120f)
            horizontalLineToRelative(360f)
            verticalLineToRelative(120f)
            lineTo(320f, 480f)
            verticalLineToRelative(320f)
            lineTo(200f, 800f)
            close()
        }
    }.build()
}
