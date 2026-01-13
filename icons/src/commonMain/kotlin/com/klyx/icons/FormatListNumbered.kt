package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FormatListNumbered: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "FormatListNumbered",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(120f, 880f)
            verticalLineToRelative(-60f)
            horizontalLineToRelative(100f)
            verticalLineToRelative(-30f)
            horizontalLineToRelative(-60f)
            verticalLineToRelative(-60f)
            horizontalLineToRelative(60f)
            verticalLineToRelative(-30f)
            lineTo(120f, 700f)
            verticalLineToRelative(-60f)
            horizontalLineToRelative(120f)
            quadToRelative(17f, 0f, 28.5f, 11.5f)
            reflectiveQuadTo(280f, 680f)
            verticalLineToRelative(40f)
            quadToRelative(0f, 17f, -11.5f, 28.5f)
            reflectiveQuadTo(240f, 760f)
            quadToRelative(17f, 0f, 28.5f, 11.5f)
            reflectiveQuadTo(280f, 800f)
            verticalLineToRelative(40f)
            quadToRelative(0f, 17f, -11.5f, 28.5f)
            reflectiveQuadTo(240f, 880f)
            lineTo(120f, 880f)
            close()
            moveTo(120f, 600f)
            verticalLineToRelative(-110f)
            quadToRelative(0f, -17f, 11.5f, -28.5f)
            reflectiveQuadTo(160f, 450f)
            horizontalLineToRelative(60f)
            verticalLineToRelative(-30f)
            lineTo(120f, 420f)
            verticalLineToRelative(-60f)
            horizontalLineToRelative(120f)
            quadToRelative(17f, 0f, 28.5f, 11.5f)
            reflectiveQuadTo(280f, 400f)
            verticalLineToRelative(70f)
            quadToRelative(0f, 17f, -11.5f, 28.5f)
            reflectiveQuadTo(240f, 510f)
            horizontalLineToRelative(-60f)
            verticalLineToRelative(30f)
            horizontalLineToRelative(100f)
            verticalLineToRelative(60f)
            lineTo(120f, 600f)
            close()
            moveTo(180f, 320f)
            verticalLineToRelative(-180f)
            horizontalLineToRelative(-60f)
            verticalLineToRelative(-60f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(240f)
            horizontalLineToRelative(-60f)
            close()
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
        }
    }.build()
}
