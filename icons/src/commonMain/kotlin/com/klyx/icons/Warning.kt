package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Warning: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Warning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveToRelative(40f, 840f)
            lineToRelative(440f, -760f)
            lineToRelative(440f, 760f)
            lineTo(40f, 840f)
            close()
            moveTo(178f, 760f)
            horizontalLineToRelative(604f)
            lineTo(480f, 240f)
            lineTo(178f, 760f)
            close()
            moveTo(480f, 720f)
            quadToRelative(17f, 0f, 28.5f, -11.5f)
            reflectiveQuadTo(520f, 680f)
            quadToRelative(0f, -17f, -11.5f, -28.5f)
            reflectiveQuadTo(480f, 640f)
            quadToRelative(-17f, 0f, -28.5f, 11.5f)
            reflectiveQuadTo(440f, 680f)
            quadToRelative(0f, 17f, 11.5f, 28.5f)
            reflectiveQuadTo(480f, 720f)
            close()
            moveTo(440f, 600f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-200f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(200f)
            close()
            moveTo(480f, 500f)
            close()
        }
    }.build()
}
