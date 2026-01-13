package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Delete: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Delete",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(280f, 840f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(200f, 760f)
            verticalLineToRelative(-520f)
            horizontalLineToRelative(-40f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(200f)
            verticalLineToRelative(-40f)
            horizontalLineToRelative(240f)
            verticalLineToRelative(40f)
            horizontalLineToRelative(200f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-40f)
            verticalLineToRelative(520f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(680f, 840f)
            lineTo(280f, 840f)
            close()
            moveTo(680f, 240f)
            lineTo(280f, 240f)
            verticalLineToRelative(520f)
            horizontalLineToRelative(400f)
            verticalLineToRelative(-520f)
            close()
            moveTo(360f, 680f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-360f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(360f)
            close()
            moveTo(520f, 680f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-360f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(360f)
            close()
            moveTo(280f, 240f)
            verticalLineToRelative(520f)
            verticalLineToRelative(-520f)
            close()
        }
    }.build()
}
