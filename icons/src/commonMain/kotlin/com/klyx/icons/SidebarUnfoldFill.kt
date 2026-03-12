package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.SidebarUnfoldFill: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "SidebarUnfoldFill",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(13f, 3f)
            horizontalLineTo(4f)
            curveTo(3.448f, 3f, 3f, 3.448f, 3f, 4f)
            verticalLineTo(20f)
            curveTo(3f, 20.552f, 3.448f, 21f, 4f, 21f)
            horizontalLineTo(13f)
            verticalLineTo(3f)
            close()
            moveTo(15f, 21f)
            verticalLineTo(3f)
            horizontalLineTo(20f)
            curveTo(20.552f, 3f, 21f, 3.448f, 21f, 4f)
            verticalLineTo(20f)
            curveTo(21f, 20.552f, 20.552f, 21f, 20f, 21f)
            horizontalLineTo(15f)
            close()
            moveTo(7f, 8.5f)
            lineTo(11f, 12f)
            lineTo(7f, 15.5f)
            verticalLineTo(8.5f)
            close()
        }
    }.build()
}
