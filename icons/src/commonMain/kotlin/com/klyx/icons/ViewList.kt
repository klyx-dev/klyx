package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.ViewList: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ViewList",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(360f, 720f)
            horizontalLineToRelative(440f)
            verticalLineToRelative(-107f)
            lineTo(360f, 613f)
            verticalLineToRelative(107f)
            close()
            moveTo(160f, 347f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(-107f)
            lineTo(160f, 240f)
            verticalLineToRelative(107f)
            close()
            moveTo(160f, 534f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(-107f)
            lineTo(160f, 427f)
            verticalLineToRelative(107f)
            close()
            moveTo(160f, 720f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(-107f)
            lineTo(160f, 613f)
            verticalLineToRelative(107f)
            close()
            moveTo(360f, 534f)
            horizontalLineToRelative(440f)
            verticalLineToRelative(-107f)
            lineTo(360f, 427f)
            verticalLineToRelative(107f)
            close()
            moveTo(360f, 347f)
            horizontalLineToRelative(440f)
            verticalLineToRelative(-107f)
            lineTo(360f, 240f)
            verticalLineToRelative(107f)
            close()
            moveTo(160f, 800f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(80f, 720f)
            verticalLineToRelative(-480f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(160f, 160f)
            horizontalLineToRelative(640f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(880f, 240f)
            verticalLineToRelative(480f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(800f, 800f)
            lineTo(160f, 800f)
            close()
        }
    }.build()
}
