package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.InsertDriveFile: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "InsertDriveFile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = true
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
            verticalLineToRelative(480f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(720f, 880f)
            lineTo(240f, 880f)
            close()
            moveTo(520f, 360f)
            verticalLineToRelative(-200f)
            lineTo(240f, 160f)
            verticalLineToRelative(640f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(-440f)
            lineTo(520f, 360f)
            close()
            moveTo(240f, 160f)
            verticalLineToRelative(200f)
            verticalLineToRelative(-200f)
            verticalLineToRelative(640f)
            verticalLineToRelative(-640f)
            close()
        }
    }.build()
}
