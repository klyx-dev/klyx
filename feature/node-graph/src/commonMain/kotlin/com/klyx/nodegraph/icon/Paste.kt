package com.klyx.nodegraph.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Icons.Paste: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Paste",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveTo(200f, 840f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(120f, 760f)
            verticalLineToRelative(-560f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(200f, 120f)
            horizontalLineToRelative(167f)
            quadToRelative(11f, -35f, 43f, -57.5f)
            reflectiveQuadToRelative(70f, -22.5f)
            quadToRelative(40f, 0f, 71.5f, 22.5f)
            reflectiveQuadTo(594f, 120f)
            horizontalLineToRelative(166f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(840f, 200f)
            verticalLineToRelative(560f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(760f, 840f)
            lineTo(200f, 840f)
            close()
            moveTo(200f, 760f)
            horizontalLineToRelative(560f)
            verticalLineToRelative(-560f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(120f)
            lineTo(280f, 320f)
            verticalLineToRelative(-120f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(560f)
            close()
            moveTo(508.5f, 188.5f)
            quadTo(520f, 177f, 520f, 160f)
            reflectiveQuadToRelative(-11.5f, -28.5f)
            quadTo(497f, 120f, 480f, 120f)
            reflectiveQuadToRelative(-28.5f, 11.5f)
            quadTo(440f, 143f, 440f, 160f)
            reflectiveQuadToRelative(11.5f, 28.5f)
            quadTo(463f, 200f, 480f, 200f)
            reflectiveQuadToRelative(28.5f, -11.5f)
            close()
        }
    }.build()
}
