package com.klyx.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Translate: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Translate",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveToRelative(476f, 880f)
            lineToRelative(182f, -480f)
            horizontalLineToRelative(84f)
            lineTo(924f, 880f)
            horizontalLineToRelative(-84f)
            lineToRelative(-43f, -122f)
            lineTo(603f, 758f)
            lineTo(560f, 880f)
            horizontalLineToRelative(-84f)
            close()
            moveTo(160f, 760f)
            lineToRelative(-56f, -56f)
            lineToRelative(202f, -202f)
            quadToRelative(-35f, -35f, -63.5f, -80f)
            reflectiveQuadTo(190f, 320f)
            horizontalLineToRelative(84f)
            quadToRelative(20f, 39f, 40f, 68f)
            reflectiveQuadToRelative(48f, 58f)
            quadToRelative(33f, -33f, 68.5f, -92.5f)
            reflectiveQuadTo(484f, 240f)
            lineTo(40f, 240f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(280f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(280f)
            verticalLineToRelative(80f)
            lineTo(564f, 240f)
            quadToRelative(-21f, 72f, -63f, 148f)
            reflectiveQuadToRelative(-83f, 116f)
            lineToRelative(96f, 98f)
            lineToRelative(-30f, 82f)
            lineToRelative(-122f, -125f)
            lineToRelative(-202f, 201f)
            close()
            moveTo(628f, 688f)
            horizontalLineToRelative(144f)
            lineToRelative(-72f, -204f)
            lineToRelative(-72f, 204f)
            close()
        }
    }.build()
}
