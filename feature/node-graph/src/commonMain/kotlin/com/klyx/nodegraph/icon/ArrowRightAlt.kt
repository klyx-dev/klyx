package com.klyx.nodegraph.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Icons.ArrowRightAlt: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ArrowRightAlt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveToRelative(560f, 720f)
            lineToRelative(-56f, -58f)
            lineToRelative(142f, -142f)
            lineTo(160f, 520f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(486f)
            lineTo(504f, 298f)
            lineToRelative(56f, -58f)
            lineToRelative(240f, 240f)
            lineToRelative(-240f, 240f)
            close()
        }
    }.build()
}
