package com.klyx.editor.compose.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.EditorColorScheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Stable
internal fun Modifier.drawHorizontalScrollbar(
    editorState: CodeEditorState,
    reverseScrolling: Boolean = false
): Modifier = drawScrollbar(editorState, Orientation.Horizontal, reverseScrolling)

@Stable
internal fun Modifier.drawVerticalScrollbar(
    editorState: CodeEditorState,
    reverseScrolling: Boolean = false
): Modifier = drawScrollbar(editorState, Orientation.Vertical, reverseScrolling)

@Stable
private inline val Orientation.isHorizontal get() = this == Orientation.Horizontal

@Stable
private fun Modifier.drawScrollbar(
    state: CodeEditorState,
    orientation: Orientation,
    reverseScrolling: Boolean
): Modifier = drawScrollbar(
    orientation, reverseScrolling, state.colorScheme
) { reverseDirection, atEnd, thickness, color, cornerRadius, alpha ->
    val maxValue = if (orientation.isHorizontal) state.maxScrollX else state.maxScrollY
    val value = if (orientation.isHorizontal) state.scrollX else state.scrollY
    val leftOffset = if (orientation.isHorizontal) state.getContentLeftOffset() else 0f

    val showScrollbar = abs(maxValue) > 0
    val canvasSize = if (orientation.isHorizontal) size.width else size.height
    val totalSize = canvasSize + abs(maxValue)
    val thumbSize = (canvasSize / totalSize * canvasSize) - leftOffset
    val startOffset = (abs(value) / totalSize * canvasSize) + leftOffset
    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, cornerRadius, alpha, thumbSize, startOffset
    )
    onDrawWithContent {
        drawContent()
        drawScrollbar()
    }
}

private fun CacheDrawScope.onDrawScrollbar(
    orientation: Orientation,
    reverseDirection: Boolean,
    atEnd: Boolean,
    showScrollbar: Boolean,
    thickness: Float,
    color: Color,
    cornerRadius: CornerRadius,
    alpha: () -> Float,
    thumbSize: Float,
    startOffset: Float
): DrawScope.() -> Unit {
    val topLeft = if (orientation.isHorizontal) {
        Offset(
            if (reverseDirection) size.width - startOffset - thumbSize else startOffset,
            if (atEnd) size.height - thickness else 0f
        )
    } else {
        Offset(
            if (atEnd) size.width - thickness else 0f,
            if (reverseDirection) size.height - startOffset - thumbSize else startOffset
        )
    }
    val size = if (orientation.isHorizontal) {
        Size(thumbSize, thickness)
    } else {
        Size(thickness, thumbSize)
    }

    return {
        if (showScrollbar) {
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = size,
                alpha = alpha(),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Stable
private fun Modifier.drawScrollbar(
    orientation: Orientation,
    reverseScrolling: Boolean,
    colorScheme: EditorColorScheme,
    onBuildDrawCache: CacheDrawScope.(
        reverseDirection: Boolean,
        atEnd: Boolean,
        thickness: Float,
        color: Color,
        cornerRadius: CornerRadius,
        alpha: () -> Float
    ) -> DrawResult
): Modifier = composed {
    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }
    val nestedScrollConnection = remember(orientation, scrolled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = if (orientation.isHorizontal) consumed.x else consumed.y
                if (delta != 0f) scrolled.tryEmit(Unit)
                return Offset.Zero
            }
        }
    }

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolled, alpha) {
        scrolled.collectLatest {
            alpha.snapTo(1f)
            delay(ScrollBarDefaultDelay)
            alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
        }
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val reverseDirection = if (orientation.isHorizontal) {
        if (isLtr) reverseScrolling else !reverseScrolling
    } else reverseScrolling
    val atEnd = if (orientation == Orientation.Vertical) isLtr else true

    // Calculate thickness here to workaround https://issuetracker.google.com/issues/206972664
    val thickness = with(LocalDensity.current) { Thickness.toPx() }
    val color = colorScheme.scrollbar
    Modifier
        .nestedScroll(nestedScrollConnection)
        .drawWithCache {
            onBuildDrawCache(reverseDirection, atEnd, thickness, color, cornerRadius, alpha::value)
        }
}

private val Thickness = 4.dp
private val cornerRadius = CornerRadius(5f)
private val FadeOutAnimationSpec = tween<Float>(durationMillis = ScrollBarFadeDuration)

private const val ScrollBarFadeDuration = 250
private const val ScrollBarDefaultDelay = 300L
