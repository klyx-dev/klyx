package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.TextFields: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "TextFields",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(280f, 800f)
            verticalLineToRelative(-520f)
            lineTo(80f, 280f)
            verticalLineToRelative(-120f)
            horizontalLineToRelative(520f)
            verticalLineToRelative(120f)
            lineTo(400f, 280f)
            verticalLineToRelative(520f)
            lineTo(280f, 800f)
            close()
            moveTo(640f, 800f)
            verticalLineToRelative(-320f)
            lineTo(520f, 480f)
            verticalLineToRelative(-120f)
            horizontalLineToRelative(360f)
            verticalLineToRelative(120f)
            lineTo(760f, 480f)
            verticalLineToRelative(320f)
            lineTo(640f, 800f)
            close()
        }
    }.build()
}
