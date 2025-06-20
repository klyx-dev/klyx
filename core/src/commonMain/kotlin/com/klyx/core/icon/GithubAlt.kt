package com.klyx.core.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KlyxIcons.GithubAlt: ImageVector
    get() {
        if (_GithubAlt != null) return _GithubAlt!!

        _GithubAlt = ImageVector.Builder(
            name = "GithubAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(21.035f, 5.257f)
                curveToRelative(0.91f, 1.092f, 1.364f, 2.366f, 1.364f, 3.822f)
                curveToRelative(0f, 5.277f, -3.002f, 6.824f, -5.823f, 7.279f)
                curveToRelative(0.364f, 0.637f, 0.455f, 1.365f, 0.455f, 2.093f)
                verticalLineToRelative(3.73f)
                curveToRelative(0f, 0.455f, -0.273f, 0.728f, -0.637f, 0.728f)
                arcToRelative(0.718f, 0.718f, 0f, false, true, -0.728f, -0.728f)
                verticalLineToRelative(-3.73f)
                arcToRelative(2.497f, 2.497f, 0f, false, false, -0.728f, -2.093f)
                lineToRelative(0.455f, -1.183f)
                curveToRelative(2.821f, -0.364f, 5.733f, -1.274f, 5.733f, -6.187f)
                curveToRelative(0f, -1.183f, -0.455f, -2.275f, -1.274f, -3.185f)
                lineToRelative(-0.182f, -0.727f)
                arcToRelative(4.04f, 4.04f, 0f, false, false, 0.09f, -2.73f)
                curveToRelative(-0.454f, 0.09f, -1.364f, 0.273f, -2.91f, 1.365f)
                lineToRelative(-0.547f, 0.09f)
                arcToRelative(13.307f, 13.307f, 0f, false, false, -6.55f, 0f)
                lineToRelative(-0.547f, -0.09f)
                curveTo(7.57f, 2.71f, 6.66f, 2.437f, 6.204f, 2.437f)
                curveToRelative(-0.273f, 0.91f, -0.273f, 1.91f, 0.09f, 2.73f)
                lineToRelative(-0.181f, 0.727f)
                curveToRelative(-0.91f, 0.91f, -1.365f, 2.093f, -1.365f, 3.185f)
                curveToRelative(0f, 4.822f, 2.73f, 5.823f, 5.732f, 6.187f)
                lineToRelative(0.364f, 1.183f)
                curveToRelative(-0.546f, 0.546f, -0.819f, 1.274f, -0.728f, 2.002f)
                verticalLineToRelative(3.821f)
                arcToRelative(0.718f, 0.718f, 0f, false, true, -0.728f, 0.728f)
                arcToRelative(0.718f, 0.718f, 0f, false, true, -0.728f, -0.728f)
                verticalLineTo(20.18f)
                curveToRelative(-3.002f, 0.637f, -4.185f, -0.91f, -5.095f, -2.092f)
                curveToRelative(-0.455f, -0.546f, -0.819f, -1.001f, -1.274f, -1.092f)
                curveToRelative(-0.09f, -0.091f, -0.364f, -0.455f, -0.273f, -0.819f)
                curveToRelative(0.091f, -0.364f, 0.455f, -0.637f, 0.82f, -0.455f)
                curveToRelative(0.91f, 0.182f, 1.455f, 0.91f, 2f, 1.547f)
                curveToRelative(0.82f, 1.092f, 1.639f, 2.092f, 4.095f, 1.547f)
                verticalLineToRelative(-0.364f)
                curveToRelative(-0.09f, -0.728f, 0.091f, -1.456f, 0.455f, -2.093f)
                curveToRelative(-2.73f, -0.546f, -5.914f, -2.093f, -5.914f, -7.279f)
                curveToRelative(0f, -1.456f, 0.455f, -2.73f, 1.365f, -3.822f)
                curveToRelative(-0.273f, -1.273f, -0.182f, -2.638f, 0.273f, -3.73f)
                lineToRelative(0.455f, -0.364f)
                curveTo(5.749f, 1.073f, 7.023f, 0.8f, 9.66f, 2.437f)
                arcToRelative(13.673f, 13.673f, 0f, false, true, 6.642f, 0f)
                curveTo(18.851f, 0.708f, 20.216f, 0.98f, 20.398f, 1.072f)
                lineToRelative(0.455f, 0.364f)
                curveToRelative(0.455f, 1.274f, 0.546f, 2.548f, 0.182f, 3.821f)
                close()
            }
        }.build()

        return _GithubAlt!!
    }

private var _GithubAlt: ImageVector? = null

