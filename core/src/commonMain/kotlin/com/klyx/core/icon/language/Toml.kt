package com.klyx.core.icon.language

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.KlyxIcons

val KlyxIcons.Language.Toml: ImageVector
    get() {
        if (_Toml != null) {
            return _Toml!!
        }
        _Toml = ImageVector.Builder(
            name = "Toml",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(0.014f, 0f)
                horizontalLineToRelative(5.34f)
                verticalLineToRelative(2.652f)
                lineTo(2.888f, 2.652f)
                verticalLineToRelative(18.681f)
                horizontalLineToRelative(2.468f)
                lineTo(5.356f, 24f)
                lineTo(0.015f, 24f)
                lineTo(0.015f, 0f)
                close()
                moveTo(17.636f, 5.049f)
                verticalLineToRelative(2.78f)
                horizontalLineToRelative(-4.274f)
                verticalLineToRelative(12.935f)
                horizontalLineToRelative(-3.008f)
                lineTo(10.354f, 7.83f)
                lineTo(6.059f, 7.83f)
                lineTo(6.059f, 5.05f)
                horizontalLineToRelative(11.577f)
                close()
                moveTo(23.986f, 24f)
                horizontalLineToRelative(-5.34f)
                verticalLineToRelative(-2.652f)
                horizontalLineToRelative(2.467f)
                lineTo(21.113f, 2.667f)
                horizontalLineToRelative(-2.468f)
                lineTo(18.645f, 0f)
                horizontalLineToRelative(5.34f)
                verticalLineToRelative(24f)
                close()
            }
        }.build()

        return _Toml!!
    }

@Suppress("ObjectPropertyName")
private var _Toml: ImageVector? = null
