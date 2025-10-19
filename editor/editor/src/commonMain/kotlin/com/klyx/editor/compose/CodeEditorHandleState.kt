package com.klyx.editor.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.ResolvedTextDirection

internal data class CodeEditorHandleState(
    val visible: Boolean,
    val position: Offset,
    val lineHeight: Float,
    val direction: ResolvedTextDirection,
    val handlesCrossed: Boolean,
) {
    companion object {
        val Hidden = CodeEditorHandleState(
            visible = false,
            position = Offset.Unspecified,
            lineHeight = 0f,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = false,
        )
    }
}
