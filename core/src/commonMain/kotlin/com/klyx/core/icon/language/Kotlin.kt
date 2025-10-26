package com.klyx.core.icon.language

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.klyx.core.icon.KlyxIcons

val KlyxIcons.Language.Kotlin: ImageVector
    get() {
        if (_Kotlin != null) {
            return _Kotlin!!
        }
        _Kotlin = ImageVector.Builder(
            name = "Kotlin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(24f, 24f)
                horizontalLineTo(0f)
                verticalLineTo(0f)
                horizontalLineToRelative(24f)
                lineTo(12f, 12f)
                close()
            }
        }.build()

        return _Kotlin!!
    }

@Suppress("ObjectPropertyName")
private var _Kotlin: ImageVector? = null
