package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FormatBold: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "FormatBold",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(272f, 760f)
            verticalLineToRelative(-560f)
            horizontalLineToRelative(221f)
            quadToRelative(65f, 0f, 120f, 40f)
            reflectiveQuadToRelative(55f, 111f)
            quadToRelative(0f, 51f, -23f, 78.5f)
            reflectiveQuadTo(602f, 469f)
            quadToRelative(25f, 11f, 55.5f, 41f)
            reflectiveQuadToRelative(30.5f, 90f)
            quadToRelative(0f, 89f, -65f, 124.5f)
            reflectiveQuadTo(501f, 760f)
            lineTo(272f, 760f)
            close()
            moveTo(393f, 648f)
            horizontalLineToRelative(104f)
            quadToRelative(48f, 0f, 58.5f, -24.5f)
            reflectiveQuadTo(566f, 588f)
            quadToRelative(0f, -11f, -10.5f, -35.5f)
            reflectiveQuadTo(494f, 528f)
            lineTo(393f, 528f)
            verticalLineToRelative(120f)
            close()
            moveTo(393f, 420f)
            horizontalLineToRelative(93f)
            quadToRelative(33f, 0f, 48f, -17f)
            reflectiveQuadToRelative(15f, -38f)
            quadToRelative(0f, -24f, -17f, -39f)
            reflectiveQuadToRelative(-44f, -15f)
            horizontalLineToRelative(-95f)
            verticalLineToRelative(109f)
            close()
        }
    }.build()
}
