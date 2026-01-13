package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.SearchOff: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "SearchOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(280f, 880f)
            quadToRelative(-83f, 0f, -141.5f, -58.5f)
            reflectiveQuadTo(80f, 680f)
            quadToRelative(0f, -83f, 58.5f, -141.5f)
            reflectiveQuadTo(280f, 480f)
            quadToRelative(83f, 0f, 141.5f, 58.5f)
            reflectiveQuadTo(480f, 680f)
            quadToRelative(0f, 83f, -58.5f, 141.5f)
            reflectiveQuadTo(280f, 880f)
            close()
            moveTo(824f, 840f)
            lineTo(568f, 584f)
            quadToRelative(-12f, -13f, -25.5f, -26.5f)
            reflectiveQuadTo(516f, 532f)
            quadToRelative(38f, -24f, 61f, -64f)
            reflectiveQuadToRelative(23f, -88f)
            quadToRelative(0f, -75f, -52.5f, -127.5f)
            reflectiveQuadTo(420f, 200f)
            quadToRelative(-75f, 0f, -127.5f, 52.5f)
            reflectiveQuadTo(240f, 380f)
            quadToRelative(0f, 6f, 0.5f, 11.5f)
            reflectiveQuadTo(242f, 403f)
            quadToRelative(-18f, 2f, -39.5f, 8f)
            reflectiveQuadTo(164f, 425f)
            quadToRelative(-2f, -11f, -3f, -22f)
            reflectiveQuadToRelative(-1f, -23f)
            quadToRelative(0f, -109f, 75.5f, -184.5f)
            reflectiveQuadTo(420f, 120f)
            quadToRelative(109f, 0f, 184.5f, 75.5f)
            reflectiveQuadTo(680f, 380f)
            quadToRelative(0f, 43f, -13.5f, 81.5f)
            reflectiveQuadTo(629f, 532f)
            lineToRelative(251f, 252f)
            lineToRelative(-56f, 56f)
            close()
            moveTo(209f, 779f)
            lineTo(280f, 708f)
            lineTo(350f, 779f)
            lineTo(379f, 751f)
            lineTo(308f, 680f)
            lineTo(379f, 609f)
            lineTo(351f, 581f)
            lineTo(280f, 652f)
            lineTo(209f, 581f)
            lineTo(181f, 609f)
            lineTo(252f, 680f)
            lineTo(181f, 751f)
            lineTo(209f, 779f)
            close()
        }
    }.build()
}
