package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.KeyboardTab: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "KeyboardTab",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = true
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(800f, 720f)
            verticalLineToRelative(-480f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(480f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(480f, 720f)
            lineTo(423f, 664f)
            lineTo(567f, 520f)
            lineTo(80f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(487f)
            lineTo(424f, 296f)
            lineToRelative(56f, -56f)
            lineToRelative(240f, 240f)
            lineToRelative(-240f, 240f)
            close()
        }
    }.build()
}
