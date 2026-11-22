package com.klyx.ui.filetree

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composeunstyled.Text
import com.composeunstyled.UnstyledIcon

@Immutable
sealed interface FileIcon {
    data class Vector(val imageVector: ImageVector, val tint: Color = Color.Unspecified) : FileIcon
    data class Glyph(val text: String, val color: Color = Color.Unspecified) : FileIcon
    data class Custom(val content: @Composable () -> Unit) : FileIcon
    data object None : FileIcon
}

@Composable
fun FileIconView(
    icon: FileIcon,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    when (icon) {
        is FileIcon.Vector -> UnstyledIcon(
            imageVector = icon.imageVector,
            contentDescription = null,
            modifier = modifier,
            tint = if (icon.tint != Color.Unspecified) icon.tint else tint,
        )

        is FileIcon.Glyph -> Text(text = icon.text, modifier = modifier)

        is FileIcon.Custom -> Box(modifier = modifier) { icon.content() }

        FileIcon.None -> Unit
    }
}


