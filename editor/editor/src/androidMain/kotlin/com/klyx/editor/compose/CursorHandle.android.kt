package com.klyx.editor.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.klyx.editor.compose.selection.Handle
import com.klyx.editor.compose.selection.HandlePopup
import com.klyx.editor.compose.selection.OffsetProvider
import com.klyx.editor.compose.selection.SelectionHandleAnchor
import com.klyx.editor.compose.selection.SelectionHandleInfo
import com.klyx.editor.compose.selection.SelectionHandleInfoKey
import com.klyx.editor.compose.selection.createHandleImage

private const val Sqrt2 = 1.41421356f
internal val CursorHandleHeight = 25.dp
internal val CursorHandleWidth = CursorHandleHeight * 2f / (1 + Sqrt2)

@Composable
internal actual fun CursorHandle(
    offsetProvider: OffsetProvider,
    modifier: Modifier,
    minTouchTargetSize: DpSize
) {
    val finalModifier =
        modifier.semantics {
            this[SelectionHandleInfoKey] =
                SelectionHandleInfo(
                    handle = Handle.Cursor,
                    position = offsetProvider.provide(),
                    anchor = SelectionHandleAnchor.Middle,
                    visible = true,
                )
        }

    HandlePopup(positionProvider = offsetProvider, handleReferencePoint = Alignment.TopCenter) {
        if (minTouchTargetSize.isSpecified) {
            Box(
                modifier =
                    finalModifier.requiredSizeIn(
                        minWidth = minTouchTargetSize.width,
                        minHeight = minTouchTargetSize.height,
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                DefaultCursorHandle()
            }
        } else {
            DefaultCursorHandle(finalModifier)
        }
    }
}

@Composable
private fun DefaultCursorHandle(modifier: Modifier = Modifier) {
    Spacer(modifier
        .size(CursorHandleWidth, CursorHandleHeight)
        .drawCursorHandle())
}

private fun Modifier.drawCursorHandle() = composed {
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.then(
        Modifier.drawWithCache {
            // Cursor handle is the same as a SelectionHandle rotated 45 degrees clockwise.
            val radius = size.width / 2f
            val imageBitmap = createHandleImage(radius = radius)
            val colorFilter = ColorFilter.tint(handleColor)
            onDrawWithContent {
                drawContent()
                withTransform({
                    translate(left = radius)
                    rotate(degrees = 45f, pivot = Offset.Zero)
                }) {
                    drawImage(image = imageBitmap, colorFilter = colorFilter)
                }
            }
        }
    )
}
