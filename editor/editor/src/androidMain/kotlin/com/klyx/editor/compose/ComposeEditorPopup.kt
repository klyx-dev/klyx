package com.klyx.editor.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import com.klyx.core.createParent
import com.klyx.editor.KlyxEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow

abstract class ComposeEditorPopup(
    protected val editor: KlyxEditor,
    localView: View,
    compositionContext: CompositionContext,
    features: Int = 0
) : EditorPopupWindow(editor, features) {

    protected val eventManager = editor.createSubEventManager()

    private val composeView = ComposeView(localView.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setParentCompositionContext(compositionContext)
        setContent { this@ComposeEditorPopup.Content() }
    }

    val rootView: View get() = composeView

    init {
        popup.contentView = composeView.createParent(
            localView.context,
            localView.findViewTreeLifecycleOwner(),
            localView.findViewTreeSavedStateRegistryOwner()
        )

        measureAndResize()
        onInitialized()
    }

    @Composable
    abstract fun Content()

    protected open fun onInitialized() {}

    protected fun measureAndResize(
        maxWidth: Int = 10000000,
        maxHeight: Int = 10000000
    ) {
        popup.contentView.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        )

        setSize(popup.contentView.measuredWidth, popup.contentView.measuredHeight)
    }

    protected fun getEditorColor(colorType: Int): Color {
        return Color(editor.colorScheme.getColor(colorType))
    }

    fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    fun isEnabled(): Boolean = eventManager.isEnabled

    protected open fun onBeforeShow(): Boolean = true
    protected open fun onDismissed() {}

    override fun show() {
        if (onBeforeShow()) {
            super.show()
        }
    }

    override fun dismiss() {
        super.dismiss()
        onDismissed()
    }

    protected fun positionRelativeToCursor(
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        preferAbove: Boolean = false
    ) {
        val cursor = editor.cursor.left()
        val charX = editor.getCharOffsetX(cursor.line, cursor.column)
        val charY = editor.getCharOffsetY(cursor.line, cursor.column)

        val locationBuffer = IntArray(2)
        editor.getLocationInWindow(locationBuffer)

        val windowX = (charX + offsetX).coerceAtLeast(0f)
        val windowY = if (preferAbove) {
            (charY - height + offsetY).coerceAtLeast(0f)
        } else {
            charY + editor.rowHeight + offsetY
        }

        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }

    protected fun positionRelativeToSelection(
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        centerHorizontally: Boolean = true
    ) {
        val cursor = editor.cursor
        if (!cursor.isSelected) {
            positionRelativeToCursor(offsetX, offsetY)
            return
        }

        val leftX = editor.getOffset(cursor.leftLine, cursor.leftColumn)
        val rightX = editor.getOffset(cursor.rightLine, cursor.rightColumn)

        val windowX = if (centerHorizontally) {
            ((leftX + rightX) / 2f - width / 2f + offsetX).coerceAtLeast(0f)
        } else {
            (leftX + offsetX).coerceAtLeast(0f)
        }

        val selectionTop = editor.getCharOffsetY(cursor.leftLine, cursor.leftColumn)
        val windowY = selectionTop + editor.rowHeight + offsetY

        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }
}
