package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.RoundedCorner: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "RoundedCorner",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(120f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(120f, 680f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(120f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(120f, 360f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(120f, 200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(280f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(280f, 200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(440f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(600f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(760f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(760f, 680f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(840f, 520f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(-200f)
            quadToRelative(0f, -50f, -35f, -85f)
            reflectiveQuadToRelative(-85f, -35f)
            lineTo(440f, 200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(200f)
            quadToRelative(83f, 0f, 141.5f, 58.5f)
            reflectiveQuadTo(840f, 320f)
            verticalLineToRelative(200f)
            close()
        }
    }.build()
}
