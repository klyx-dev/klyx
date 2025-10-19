package com.klyx.editor.compose.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.max

/**
 * Selection is transparent in terms of measurement and layout and passes the same constraints to
 * the children.
 */
@Composable
internal fun SimpleLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        var width = 0
        var height = 0
        val placeables =
            measurables.fastMap { measurable ->
                val placeable = measurable.measure(constraints)
                width = max(width, placeable.width)
                height = max(height, placeable.height)
                placeable
            }
        layout(width, height) { placeables.fastForEach { placeable -> placeable.place(0, 0) } }
    }
}
