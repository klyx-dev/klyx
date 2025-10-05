package com.klyx.core.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KlyxIcons.BackToTab: ImageVector
    get() {
        if (_BackToTab != null) return _BackToTab!!

        _BackToTab = ImageVector.Builder(
            name = "BackToTab",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(331f, 468f)
                lineTo(160f, 297f)
                verticalLineToRelative(143f)
                horizontalLineTo(80f)
                verticalLineToRelative(-280f)
                horizontalLineToRelative(280f)
                verticalLineToRelative(80f)
                horizontalLineTo(216f)
                lineToRelative(172f, 171f)
                close()
                moveTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-200f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(200f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(80f)
                close()
                moveToRelative(640f, -280f)
                verticalLineToRelative(-280f)
                horizontalLineTo(440f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(360f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 240f)
                verticalLineToRelative(280f)
                close()
                moveToRelative(80f, 80f)
                verticalLineToRelative(200f)
                horizontalLineTo(560f)
                verticalLineToRelative(-200f)
                close()
            }
        }.build()

        return _BackToTab!!
    }

private var _BackToTab: ImageVector? = null

