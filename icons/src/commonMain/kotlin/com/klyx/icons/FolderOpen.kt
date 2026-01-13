package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FolderOpen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "FolderOpen",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
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
            lineTo(447f, 320f)
            lineToRelative(-80f, -80f)
            lineTo(160f, 240f)
            verticalLineToRelative(480f)
            lineToRelative(96f, -320f)
            horizontalLineToRelative(684f)
            lineTo(837f, 743f)
            quadToRelative(-8f, 26f, -29.5f, 41.5f)
            reflectiveQuadTo(760f, 800f)
            lineTo(160f, 800f)
            close()
            moveTo(244f, 720f)
            horizontalLineToRelative(516f)
            lineToRelative(72f, -240f)
            lineTo(316f, 480f)
            lineToRelative(-72f, 240f)
            close()
            moveTo(244f, 720f)
            lineTo(316f, 480f)
            lineTo(244f, 720f)
            close()
            moveTo(160f, 320f)
            verticalLineToRelative(-80f)
            verticalLineToRelative(80f)
            close()
        }
    }.build()
}
