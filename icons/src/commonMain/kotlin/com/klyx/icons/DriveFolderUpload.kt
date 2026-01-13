package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.DriveFolderUpload: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "DriveFolderUpload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(440f, 680f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-168f)
            lineToRelative(64f, 64f)
            lineToRelative(56f, -56f)
            lineToRelative(-160f, -160f)
            lineToRelative(-160f, 160f)
            lineToRelative(56f, 56f)
            lineToRelative(64f, -64f)
            verticalLineToRelative(168f)
            close()
            moveTo(160f, 800f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(80f, 720f)
            verticalLineToRelative(-480f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(160f, 160f)
            horizontalLineToRelative(240f)
            lineToRelative(80f, 80f)
            horizontalLineToRelative(320f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(880f, 320f)
            verticalLineToRelative(400f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(800f, 800f)
            lineTo(160f, 800f)
            close()
            moveTo(160f, 720f)
            horizontalLineToRelative(640f)
            verticalLineToRelative(-400f)
            lineTo(447f, 320f)
            lineToRelative(-80f, -80f)
            lineTo(160f, 240f)
            verticalLineToRelative(480f)
            close()
            moveTo(160f, 720f)
            verticalLineToRelative(-480f)
            verticalLineToRelative(480f)
            close()
        }
    }.build()
}
