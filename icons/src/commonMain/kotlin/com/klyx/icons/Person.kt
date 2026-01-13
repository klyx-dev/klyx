package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Person: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Person",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(480f, 480f)
            quadToRelative(-66f, 0f, -113f, -47f)
            reflectiveQuadToRelative(-47f, -113f)
            quadToRelative(0f, -66f, 47f, -113f)
            reflectiveQuadToRelative(113f, -47f)
            quadToRelative(66f, 0f, 113f, 47f)
            reflectiveQuadToRelative(47f, 113f)
            quadToRelative(0f, 66f, -47f, 113f)
            reflectiveQuadToRelative(-113f, 47f)
            close()
            moveTo(160f, 800f)
            verticalLineToRelative(-112f)
            quadToRelative(0f, -34f, 17.5f, -62.5f)
            reflectiveQuadTo(224f, 582f)
            quadToRelative(62f, -31f, 126f, -46.5f)
            reflectiveQuadTo(480f, 520f)
            quadToRelative(66f, 0f, 130f, 15.5f)
            reflectiveQuadTo(736f, 582f)
            quadToRelative(29f, 15f, 46.5f, 43.5f)
            reflectiveQuadTo(800f, 688f)
            verticalLineToRelative(112f)
            lineTo(160f, 800f)
            close()
            moveTo(240f, 720f)
            horizontalLineToRelative(480f)
            verticalLineToRelative(-32f)
            quadToRelative(0f, -11f, -5.5f, -20f)
            reflectiveQuadTo(700f, 654f)
            quadToRelative(-54f, -27f, -109f, -40.5f)
            reflectiveQuadTo(480f, 600f)
            quadToRelative(-56f, 0f, -111f, 13.5f)
            reflectiveQuadTo(260f, 654f)
            quadToRelative(-9f, 5f, -14.5f, 14f)
            reflectiveQuadToRelative(-5.5f, 20f)
            verticalLineToRelative(32f)
            close()
            moveTo(480f, 400f)
            quadToRelative(33f, 0f, 56.5f, -23.5f)
            reflectiveQuadTo(560f, 320f)
            quadToRelative(0f, -33f, -23.5f, -56.5f)
            reflectiveQuadTo(480f, 240f)
            quadToRelative(-33f, 0f, -56.5f, 23.5f)
            reflectiveQuadTo(400f, 320f)
            quadToRelative(0f, 33f, 23.5f, 56.5f)
            reflectiveQuadTo(480f, 400f)
            close()
            moveTo(480f, 320f)
            close()
            moveTo(480f, 720f)
            close()
        }
    }.build()
}
