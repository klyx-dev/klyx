package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import com.klyx.editor.compose.selection.OffsetProvider

@Composable
@Suppress("UNUSED_PARAMETER")
internal actual fun CursorHandle(
    offsetProvider: OffsetProvider,
    modifier: Modifier,
    minTouchTargetSize: DpSize
) {
    /* Not implemented. */
}
