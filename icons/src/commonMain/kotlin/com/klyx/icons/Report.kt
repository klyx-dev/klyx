package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Report: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Report",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(480f, 680f)
            quadToRelative(17f, 0f, 28.5f, -11.5f)
            reflectiveQuadTo(520f, 640f)
            quadToRelative(0f, -17f, -11.5f, -28.5f)
            reflectiveQuadTo(480f, 600f)
            quadToRelative(-17f, 0f, -28.5f, 11.5f)
            reflectiveQuadTo(440f, 640f)
            quadToRelative(0f, 17f, 11.5f, 28.5f)
            reflectiveQuadTo(480f, 680f)
            close()
            moveTo(440f, 520f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(240f)
            close()
            moveTo(330f, 840f)
            lineTo(120f, 630f)
            verticalLineToRelative(-300f)
            lineToRelative(210f, -210f)
            horizontalLineToRelative(300f)
            lineToRelative(210f, 210f)
            verticalLineToRelative(300f)
            lineTo(630f, 840f)
            lineTo(330f, 840f)
            close()
            moveTo(364f, 760f)
            horizontalLineToRelative(232f)
            lineToRelative(164f, -164f)
            verticalLineToRelative(-232f)
            lineTo(596f, 200f)
            lineTo(364f, 200f)
            lineTo(200f, 364f)
            verticalLineToRelative(232f)
            lineToRelative(164f, 164f)
            close()
            moveTo(480f, 480f)
            close()
        }
    }.build()
}
