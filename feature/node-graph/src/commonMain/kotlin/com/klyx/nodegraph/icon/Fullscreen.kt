package com.klyx.nodegraph.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Icons.Fullscreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Fullscreen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(120f, 840f)
            verticalLineToRelative(-200f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(120f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(80f)
            lineTo(120f, 840f)
            close()
            moveTo(640f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(-120f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(200f)
            lineTo(640f, 840f)
            close()
            moveTo(120f, 320f)
            verticalLineToRelative(-200f)
            horizontalLineToRelative(200f)
            verticalLineToRelative(80f)
            lineTo(200f, 200f)
            verticalLineToRelative(120f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(760f, 320f)
            verticalLineToRelative(-120f)
            lineTo(640f, 200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(200f)
            verticalLineToRelative(200f)
            horizontalLineToRelative(-80f)
            close()
        }
    }.build()
}
