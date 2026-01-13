package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Redo: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Redo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = true
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(396f, 760f)
            quadToRelative(-97f, 0f, -166.5f, -63f)
            reflectiveQuadTo(160f, 540f)
            quadToRelative(0f, -94f, 69.5f, -157f)
            reflectiveQuadTo(396f, 320f)
            horizontalLineToRelative(252f)
            lineTo(544f, 216f)
            lineToRelative(56f, -56f)
            lineToRelative(200f, 200f)
            lineToRelative(-200f, 200f)
            lineToRelative(-56f, -56f)
            lineToRelative(104f, -104f)
            lineTo(396f, 400f)
            quadToRelative(-63f, 0f, -109.5f, 40f)
            reflectiveQuadTo(240f, 540f)
            quadToRelative(0f, 60f, 46.5f, 100f)
            reflectiveQuadTo(396f, 680f)
            horizontalLineToRelative(284f)
            verticalLineToRelative(80f)
            lineTo(396f, 760f)
            close()
        }
    }.build()
}
