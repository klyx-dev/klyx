package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FileOpen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "FileOpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(240f, 880f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(160f, 800f)
            verticalLineToRelative(-640f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(240f, 80f)
            horizontalLineToRelative(320f)
            lineToRelative(240f, 240f)
            verticalLineToRelative(240f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-200f)
            lineTo(520f, 360f)
            verticalLineToRelative(-200f)
            lineTo(240f, 160f)
            verticalLineToRelative(640f)
            horizontalLineToRelative(360f)
            verticalLineToRelative(80f)
            lineTo(240f, 880f)
            close()
            moveTo(878f, 895f)
            lineTo(760f, 777f)
            verticalLineToRelative(89f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-226f)
            horizontalLineToRelative(226f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-90f)
            lineToRelative(118f, 118f)
            lineToRelative(-56f, 57f)
            close()
            moveTo(240f, 800f)
            verticalLineToRelative(-640f)
            verticalLineToRelative(640f)
            close()
        }
    }.build()
}
