package com.klyx.nodegraph.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Icons.Duplicate: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Duplicate",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(320f, 880f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(240f, 800f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(-80f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(80f, 640f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-320f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(320f, 240f)
            horizontalLineToRelative(320f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(720f, 160f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(80f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(880f, 320f)
            verticalLineToRelative(480f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(800f, 880f)
            lineTo(320f, 880f)
            close()
            moveTo(320f, 800f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(-480f)
            lineTo(320f, 320f)
            verticalLineToRelative(480f)
            close()
            moveTo(80f, 480f)
            verticalLineToRelative(-160f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(160f)
            lineTo(80f, 480f)
            close()
            moveTo(80f, 240f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(160f, 80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(80f)
            lineTo(80f, 240f)
            close()
            moveTo(320f, 160f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(80f)
            lineTo(320f, 160f)
            close()
            moveTo(320f, 800f)
            verticalLineToRelative(-480f)
            verticalLineToRelative(480f)
            close()
        }
    }.build()
}
