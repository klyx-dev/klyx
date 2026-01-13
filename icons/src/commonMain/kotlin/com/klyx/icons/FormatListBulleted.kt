package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FormatListBulleted: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "FormatListBulleted",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = true
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(360f, 760f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(80f)
            lineTo(360f, 760f)
            close()
            moveTo(360f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(80f)
            lineTo(360f, 520f)
            close()
            moveTo(360f, 280f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(80f)
            lineTo(360f, 280f)
            close()
            moveTo(200f, 800f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(120f, 720f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(200f, 640f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(280f, 720f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(200f, 800f)
            close()
            moveTo(200f, 560f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(120f, 480f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(200f, 400f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(280f, 480f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(200f, 560f)
            close()
            moveTo(200f, 320f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(120f, 240f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(200f, 160f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(280f, 240f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(200f, 320f)
            close()
        }
    }.build()
}
