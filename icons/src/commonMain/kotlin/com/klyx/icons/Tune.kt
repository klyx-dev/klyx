package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Tune: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Tune",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(440f, 840f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(320f)
            verticalLineToRelative(80f)
            lineTo(520f, 760f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(120f, 760f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(240f)
            verticalLineToRelative(80f)
            lineTo(120f, 760f)
            close()
            moveTo(280f, 600f)
            verticalLineToRelative(-80f)
            lineTo(120f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(240f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(440f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(400f)
            verticalLineToRelative(80f)
            lineTo(440f, 520f)
            close()
            moveTo(600f, 360f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(80f)
            lineTo(680f, 280f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(120f, 280f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(400f)
            verticalLineToRelative(80f)
            lineTo(120f, 280f)
            close()
        }
    }.build()
}
