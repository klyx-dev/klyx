package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.DataObject: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "DataObject",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(560f, 800f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(120f)
            quadToRelative(17f, 0f, 28.5f, -11.5f)
            reflectiveQuadTo(720f, 680f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -38f, 22f, -69f)
            reflectiveQuadToRelative(58f, -44f)
            verticalLineToRelative(-14f)
            quadToRelative(-36f, -13f, -58f, -44f)
            reflectiveQuadToRelative(-22f, -69f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -17f, -11.5f, -28.5f)
            reflectiveQuadTo(680f, 240f)
            lineTo(560f, 240f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(120f)
            quadToRelative(50f, 0f, 85f, 35f)
            reflectiveQuadToRelative(35f, 85f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 17f, 11.5f, 28.5f)
            reflectiveQuadTo(840f, 400f)
            horizontalLineToRelative(40f)
            verticalLineToRelative(160f)
            horizontalLineToRelative(-40f)
            quadToRelative(-17f, 0f, -28.5f, 11.5f)
            reflectiveQuadTo(800f, 600f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 50f, -35f, 85f)
            reflectiveQuadToRelative(-85f, 35f)
            lineTo(560f, 800f)
            close()
            moveTo(280f, 800f)
            quadToRelative(-50f, 0f, -85f, -35f)
            reflectiveQuadToRelative(-35f, -85f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -17f, -11.5f, -28.5f)
            reflectiveQuadTo(120f, 560f)
            lineTo(80f, 560f)
            verticalLineToRelative(-160f)
            horizontalLineToRelative(40f)
            quadToRelative(17f, 0f, 28.5f, -11.5f)
            reflectiveQuadTo(160f, 360f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -50f, 35f, -85f)
            reflectiveQuadToRelative(85f, -35f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(80f)
            lineTo(280f, 240f)
            quadToRelative(-17f, 0f, -28.5f, 11.5f)
            reflectiveQuadTo(240f, 280f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 38f, -22f, 69f)
            reflectiveQuadToRelative(-58f, 44f)
            verticalLineToRelative(14f)
            quadToRelative(36f, 13f, 58f, 44f)
            reflectiveQuadToRelative(22f, 69f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 17f, 11.5f, 28.5f)
            reflectiveQuadTo(280f, 720f)
            horizontalLineToRelative(120f)
            verticalLineToRelative(80f)
            lineTo(280f, 800f)
            close()
        }
    }.build()
}
