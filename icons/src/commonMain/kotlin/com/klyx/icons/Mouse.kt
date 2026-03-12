package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Mouse: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Mouse",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(480f, 880f)
            quadToRelative(-116f, 0f, -198f, -82f)
            reflectiveQuadToRelative(-82f, -198f)
            verticalLineToRelative(-240f)
            quadToRelative(0f, -116f, 82f, -198f)
            reflectiveQuadToRelative(198f, -82f)
            quadToRelative(116f, 0f, 198f, 82f)
            reflectiveQuadToRelative(82f, 198f)
            verticalLineToRelative(240f)
            quadToRelative(0f, 116f, -82f, 198f)
            reflectiveQuadTo(480f, 880f)
            close()
            moveTo(520f, 360f)
            horizontalLineToRelative(160f)
            quadToRelative(0f, -72f, -45.5f, -127f)
            reflectiveQuadTo(520f, 164f)
            verticalLineToRelative(196f)
            close()
            moveTo(280f, 360f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(-196f)
            quadToRelative(-69f, 14f, -114.5f, 69f)
            reflectiveQuadTo(280f, 360f)
            close()
            moveTo(480f, 800f)
            quadToRelative(83f, 0f, 141.5f, -58.5f)
            reflectiveQuadTo(680f, 600f)
            verticalLineToRelative(-160f)
            lineTo(280f, 440f)
            verticalLineToRelative(160f)
            quadToRelative(0f, 83f, 58.5f, 141.5f)
            reflectiveQuadTo(480f, 800f)
            close()
            moveTo(480f, 440f)
            close()
            moveTo(520f, 360f)
            close()
            moveTo(440f, 360f)
            close()
            moveTo(480f, 440f)
            close()
        }
    }.build()
}
