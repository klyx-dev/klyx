package com.klyx.nodegraph.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Icons.ElectricBolt: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "ElectricBolt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color(0xFFE3E3E3))) {
            moveToRelative(280f, 880f)
            lineToRelative(160f, -300f)
            lineToRelative(-320f, -40f)
            lineToRelative(480f, -460f)
            horizontalLineToRelative(80f)
            lineTo(520f, 380f)
            lineToRelative(320f, 40f)
            lineTo(360f, 880f)
            horizontalLineToRelative(-80f)
            close()
            moveTo(502f, 633f)
            lineTo(663f, 479f)
            lineTo(394f, 445f)
            lineTo(457f, 328f)
            lineTo(297f, 482f)
            lineTo(565f, 515f)
            lineTo(502f, 633f)
            close()
            moveTo(480f, 480f)
            close()
        }
    }.build()
}
