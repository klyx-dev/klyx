package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import kotlin.jvm.JvmName
import kotlin.math.roundToInt

@Stable
internal class HandleState(
    val initialOrientation: HandleOrientation,
    val handleWidthPx: Float,
    val handleHeightPx: Float,
) {
    /** may flip near screen edges. */
    var orientation by mutableStateOf(initialOrientation)
        private set

    /** Top-left X of the popup window in screen pixels. */
    var pointX by mutableStateOf(0f)
        internal set

    /** Top-left Y of the popup window in screen pixels. */
    var pointY by mutableStateOf(0f)
        internal set

    /** X offset from [pointX] to the handle's cursor tip. */
    var hotspotX by mutableStateOf(computeHotspotX(initialOrientation, handleWidthPx))
        private set

    /** Y offset from [pointY] to the handle's cursor tip (always 0 — mHotspotY). */
    val hotspotY: Float = 0f

    /** Vertical nudge applied during drag so the finger doesn't cover the handle. */
    val touchOffsetY: Float get() = -handleHeightPx * 0.3f

    /** true while the user's finger is down. */
    var isDragging by mutableStateOf(false)
        internal set

    /**
     * Offset between the raw screen X and [pointX] at the moment of touch-down.
     */
    var touchToWindowOffsetX: Float = 0f
        internal set

    /**
     * Offset between the raw screen Y and [pointY] at the moment of touch-down.
     */
    var touchToWindowOffsetY: Float = 0f
        internal set

    var lastParentX: Int = 0
        internal set
    var lastParentY: Int = 0
        internal set

    val popupOffset: IntOffset
        get() = IntOffset(pointX.roundToInt(), pointY.roundToInt())

    var isVisible by mutableStateOf(false)
        internal set

    @JvmName("changeOrientation")
    fun setOrientation(newOrientation: HandleOrientation) {
        if (orientation == newOrientation) return
        orientation = newOrientation
        hotspotX = computeHotspotX(newOrientation, handleWidthPx)
    }

    /** Reset orientation back to [initialOrientation]. */
    fun resetOrientation() = setOrientation(initialOrientation)

    fun moveTo(screenX: Float, screenY: Float) {
        pointX = screenX - hotspotX
        pointY = screenY
    }

    fun onDragStart(rawX: Float, rawY: Float, parentX: Int, parentY: Int) {
        touchToWindowOffsetX = rawX - pointX
        touchToWindowOffsetY = rawY - pointY
        lastParentX = parentX
        lastParentY = parentY
        isDragging = true
    }

    /**
     * @return The screen-pixel point that should be mapped back to a terminal cell.
     */
    fun onDragMove(rawX: Float, rawY: Float): Pair<Int, Int> {
        val newPosX = rawX - touchToWindowOffsetX + hotspotX
        val newPosY = rawY - touchToWindowOffsetY + hotspotY + touchOffsetY
        return Pair(newPosX.roundToInt(), newPosY.roundToInt())
    }

    fun onParentMoved(currentParentX: Int, currentParentY: Int) {
        if (currentParentX != lastParentX || currentParentY != lastParentY) {
            touchToWindowOffsetX += currentParentX - lastParentX
            touchToWindowOffsetY += currentParentY - lastParentY
            lastParentX = currentParentX
            lastParentY = currentParentY
        }
    }

    fun onDragEnd() {
        isDragging = false
    }

    fun checkOrientation(posX: Float, clipLeft: Float, clipRight: Float) {
        when {
            posX - handleWidthPx < clipLeft -> setOrientation(HandleOrientation.Right)
            posX + handleWidthPx > clipRight -> setOrientation(HandleOrientation.Left)
            else -> resetOrientation()
        }
    }

    fun isHotspotVisible(
        clipLeft: Float,
        clipTop: Float,
        clipRight: Float,
        clipBottom: Float,
        parentScreenX: Float,
        parentScreenY: Float,
    ): Boolean {
        if (isDragging) return true // always show while dragging
        val posX = parentScreenX + pointX + hotspotX
        val posY = parentScreenY + pointY + hotspotY
        return posX in clipLeft..clipRight && posY >= clipTop && posY <= clipBottom
    }

    companion object {
        fun computeHotspotX(orientation: HandleOrientation, widthPx: Float): Float =
            when (orientation) {
                HandleOrientation.Left -> widthPx * 3f / 4f
                HandleOrientation.Right -> widthPx / 4f
            }
    }
}
