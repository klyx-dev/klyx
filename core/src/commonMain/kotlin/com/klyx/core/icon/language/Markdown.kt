package com.klyx.core.icon.language

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.KlyxIcons

val KlyxIcons.Language.Markdown: ImageVector
    get() {
        if (_Markdown != null) {
            return _Markdown!!
        }
        _Markdown = ImageVector.Builder(
            name = "Markdown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(640f, 600f)
                lineToRelative(120f, -120f)
                lineToRelative(-42f, -43f)
                lineToRelative(-48f, 48f)
                verticalLineToRelative(-125f)
                horizontalLineToRelative(-60f)
                verticalLineToRelative(125f)
                lineToRelative(-48f, -48f)
                lineToRelative(-42f, 43f)
                close()
                moveTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-480f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 160f)
                horizontalLineToRelative(640f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 240f)
                verticalLineToRelative(480f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(800f, 800f)
                close()
                moveTo(160f, 720f)
                horizontalLineToRelative(640f)
                verticalLineToRelative(-480f)
                lineTo(160f, 240f)
                close()
                moveTo(160f, 720f)
                verticalLineToRelative(-480f)
                close()
                moveTo(220f, 600f)
                horizontalLineToRelative(60f)
                verticalLineToRelative(-180f)
                horizontalLineToRelative(40f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(60f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(40f)
                verticalLineToRelative(180f)
                horizontalLineToRelative(60f)
                verticalLineToRelative(-200f)
                quadToRelative(0f, -17f, -11.5f, -28.5f)
                reflectiveQuadTo(440f, 360f)
                lineTo(260f, 360f)
                quadToRelative(-17f, 0f, -28.5f, 11.5f)
                reflectiveQuadTo(220f, 400f)
                close()
            }
        }.build()

        return _Markdown!!
    }

@Suppress("ObjectPropertyName")
private var _Markdown: ImageVector? = null
