package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Klyx: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Klyx",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 100f,
        viewportHeight = 100f,
        autoMirror = false
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(90f, 10f)
            lineTo(30f, 45f)
            lineTo(30f, 10f)
            lineTo(10f, 10f)
            lineTo(10f, 90f)
            lineTo(40f, 65f)
            lineTo(65f, 90f)
            lineTo(90f, 90f)
            lineTo(55f, 52f)
            lineTo(90f, 10f)
            close()
        }
    }.build()
}
