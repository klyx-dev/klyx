package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.MouseRegular: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "MouseRegular",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.975f, 22f)
            horizontalLineTo(12f)
            curveToRelative(3.859f, 0f, 7f, -3.14f, 7f, -7f)
            verticalLineTo(9f)
            curveToRelative(0f, -3.841f, -3.127f, -6.974f, -6.981f, -7f)
            horizontalLineToRelative(-0.06f)
            curveTo(8.119f, 2.022f, 5f, 5.157f, 5f, 9f)
            verticalLineToRelative(6f)
            curveToRelative(0f, 3.86f, 3.129f, 7f, 6.975f, 7f)
            close()
            moveTo(7f, 9f)
            arcToRelative(5.007f, 5.007f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4.985f, -5f)
            curveTo(14.75f, 4.006f, 17f, 6.249f, 17f, 9f)
            verticalLineToRelative(6f)
            curveToRelative(0f, 2.757f, -2.243f, 5f, -5f, 5f)
            horizontalLineToRelative(-0.025f)
            curveTo(9.186f, 20f, 7f, 17.804f, 7f, 15f)
            verticalLineTo(9f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 6f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(-2f)
            close()
        }
    }.build()
}
