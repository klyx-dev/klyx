package com.klyx.editor.compose.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Clickable padding of handler
 */
private val PADDING = 5.dp

/**
 * Radius of handle circle
 */
private val RADIUS = 6.dp

/**
 * Thickness of handlers vertical line
 */
private val THICKNESS = 2.dp

/**
 * [SelectionHandle] is used for drawing selection handlers on all non-android platforms
 * if a certain set of touch-based events happened.
 * The logic of when that happened is out of scope of [SelectionHandle], this is something that
 * basically implementations of [Modifier.textFieldPointer] are in charge of.
 * De-facto on any platform, these handles won't be shown on mouse events.
 * That said, one can see them while selecting text on iOS and mobile web targets.
 *
 * [SelectionHandle] was initially designed as iOS entity but later was commonized as is.
 */
@Composable
internal actual fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    minTouchTargetSize: DpSize, // TODO https://youtrack.jetbrains.com/issue/COMPOSE-1266
    lineHeight: Float,
    modifier: Modifier,
) {
    val isLeft = isLeftSelectionHandle(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val handleReferencePoint = if (isLeft) Alignment.BottomCenter else Alignment.TopCenter

    HandlePopup(
        positionProvider = object : OffsetProvider {
            override fun provide(): Offset {
                var offset = offsetProvider.provide()
                if (offset.isSpecified && !isLeft) {
                    offset += Offset(0f, -lineHeight)
                }
                return offset
            }
        },
        handleReferencePoint = handleReferencePoint
    ) {
        SelectionHandleIcon(
            modifier = modifier.semantics {
                val position = offsetProvider.provide()
                this[SelectionHandleInfoKey] = SelectionHandleInfo(
                    handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd,
                    position = position,
                    anchor = if (isLeft) SelectionHandleAnchor.Left else SelectionHandleAnchor.Right,
                    visible = position.isSpecified,
                )
            },
            iconVisible = { offsetProvider.provide().isSpecified },
            lineHeight = lineHeight,
            isLeft = isLeft,
        )
    }
}

@Composable
internal fun SelectionHandleIcon(
    modifier: Modifier,
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean,
) {
    val density = LocalDensity.current
    val lineHeightDp = with(density) { lineHeight.toDp() }
    Spacer(
        modifier
            .size(
                width = (PADDING + RADIUS) * 2,
                height = RADIUS * 2 + PADDING + lineHeightDp
            )
            .drawSelectionHandle(iconVisible, lineHeight, isLeft)
    )
}

internal fun Modifier.drawSelectionHandle(
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean
): Modifier = composed {
    val density = LocalDensity.current
    val paddingPx = with(density) { PADDING.toPx() }
    val radiusPx = with(density) { RADIUS.toPx() }
    val thicknessPx = with(density) { THICKNESS.toPx() }
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.drawWithCache {
        onDrawWithContent {
            drawContent()
            if (!iconVisible()) return@onDrawWithContent

            // vertical line
            drawRect(
                color = handleColor,
                topLeft = Offset(
                    x = paddingPx + radiusPx - thicknessPx / 2,
                    y = if (isLeft) paddingPx + radiusPx else 0f
                ),
                size = Size(thicknessPx, lineHeight + radiusPx)
            )
            // handle circle
            drawCircle(
                color = handleColor,
                radius = radiusPx,
                center = center.copy(
                    y = if (isLeft) paddingPx + radiusPx else lineHeight + radiusPx
                )
            )
        }
    }
}

@Composable
internal fun HandlePopup(
    positionProvider: OffsetProvider,
    handleReferencePoint: Alignment,
    content: @Composable () -> Unit
) {
    val popupPositionProvider = remember(handleReferencePoint, positionProvider) {
        HandlePositionProvider(handleReferencePoint, positionProvider)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(clippingEnabled = false),
        content = content,
    )
}
