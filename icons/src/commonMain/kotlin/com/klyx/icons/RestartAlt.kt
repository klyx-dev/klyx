package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.RestartAlt: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "RestartAlt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(440f, 838f)
            quadToRelative(-121f, -15f, -200.5f, -105.5f)
            reflectiveQuadTo(160f, 520f)
            quadToRelative(0f, -66f, 26f, -126.5f)
            reflectiveQuadTo(260f, 288f)
            lineToRelative(57f, 57f)
            quadToRelative(-38f, 34f, -57.5f, 79f)
            reflectiveQuadTo(240f, 520f)
            quadToRelative(0f, 88f, 56f, 155.5f)
            reflectiveQuadTo(440f, 758f)
            verticalLineToRelative(80f)
            close()
            moveTo(520f, 838f)
            verticalLineToRelative(-80f)
            quadToRelative(87f, -16f, 143.5f, -83f)
            reflectiveQuadTo(720f, 520f)
            quadToRelative(0f, -100f, -70f, -170f)
            reflectiveQuadToRelative(-170f, -70f)
            horizontalLineToRelative(-3f)
            lineToRelative(44f, 44f)
            lineToRelative(-56f, 56f)
            lineToRelative(-140f, -140f)
            lineToRelative(140f, -140f)
            lineToRelative(56f, 56f)
            lineToRelative(-44f, 44f)
            horizontalLineToRelative(3f)
            quadToRelative(134f, 0f, 227f, 93f)
            reflectiveQuadToRelative(93f, 227f)
            quadToRelative(0f, 121f, -79.5f, 211.5f)
            reflectiveQuadTo(520f, 838f)
            close()
        }
    }.build()
}
