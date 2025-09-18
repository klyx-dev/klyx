package com.klyx.editor.textaction

import android.graphics.RectF
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.klyx.editor.KlyxEditor
import com.klyx.editor.compose.ComposeActionPopup
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.HandleStateChangeEvent
import io.github.rosemoe.sora.event.InterceptTarget
import io.github.rosemoe.sora.event.LongPressEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.subscribeAlways
import kotlin.math.max
import kotlin.math.min

class TextActionWindow(
    editor: KlyxEditor,
    localView: View,
    compositionContext: CompositionContext
) : ComposeActionPopup(editor, localView, compositionContext) {
    companion object {
        private const val DELAY = 200L
        private const val CHECK_FOR_DISMISS_INTERVAL = 100L
    }

    private val handler = editor.eventHandler
    private var lastScroll = 0L
    private var lastPosition = 0
    private var lastCause = 0

    override fun onInitialized() {
        popup.animationStyle = R.style.text_action_popup_animation
        setSize((editor.dpUnit * 200).toInt(), (editor.dpUnit * 48).toInt())
        subscribeEvents()
    }

    @Composable
    override fun Content() {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(popup.elevation.dp))
                .horizontalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextActionItems(editor) { actionId ->
                onActionPerformed(actionId)
            }
        }
    }

    private fun subscribeEvents() {
        with(eventManager) {
            subscribeAlways<SelectionChangeEvent>(::onSelectionChange)
            subscribeAlways<ScrollEvent>(::onEditorScroll)
            subscribeAlways<HandleStateChangeEvent>(::onHandleStateChange)
            subscribeAlways<LongPressEvent>(::onEditorLongPress)
            subscribeAlways<EditorFocusChangeEvent>(::onEditorFocusChange)
        }
    }

    override fun onBeforeShow(): Boolean {
        return !editor.snippetController.isInSnippet() &&
                editor.hasFocus() &&
                !editor.isInMouseMode()
    }

    private fun onSelectionChange(event: SelectionChangeEvent) {
        if (handler.hasAnyHeldHandle()) return

        lastCause = event.cause
        if (event.isSelected) {
            if (event.cause != SelectionChangeEvent.CAUSE_SEARCH) {
                editor.postInLifecycle(::displayWindow)
            } else {
                dismiss()
            }
            lastPosition = -1
        } else {
            var show = false
            if (event.cause == SelectionChangeEvent.CAUSE_TAP && event.left.index == lastPosition && !isShowing && !editor.text.isInBatchEdit && editor.isEditable) {
                editor.postInLifecycle(::displayWindow)
                show = true
            } else {
                dismiss()
            }

            lastPosition = if (event.cause == SelectionChangeEvent.CAUSE_TAP && !show) event.left.index else -1
        }
    }

    private fun onEditorScroll(event: ScrollEvent) {
        val last = lastScroll
        lastScroll = System.currentTimeMillis()
        if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
            postDisplay()
        }
    }

    private fun onHandleStateChange(event: HandleStateChangeEvent) {
        if (event.isHeld) postDisplay()

        if (
            !event.editor.cursor.isSelected &&
            event.handleType == HandleStateChangeEvent.HANDLE_TYPE_INSERT &&
            !event.isHeld
        ) {
            displayWindow()
            // Also, post to hide the window on handle disappearance
            editor.postDelayedInLifecycle(object : Runnable {
                override fun run() {
                    if (!editor.eventHandler.shouldDrawInsertHandle() && !editor.cursor.isSelected) {
                        dismiss()
                    } else if (!editor.cursor.isSelected) {
                        editor.postDelayedInLifecycle(this, CHECK_FOR_DISMISS_INTERVAL)
                    }
                }
            }, CHECK_FOR_DISMISS_INTERVAL)
        }
    }

    private fun onEditorLongPress(event: LongPressEvent) {
        if (editor.cursor.isSelected && lastCause == SelectionChangeEvent.CAUSE_SEARCH) {
            val idx = event.index
            if (idx >= editor.cursor.left && idx <= editor.cursor.right) {
                lastCause = 0
                displayWindow()
            }
            event.intercept(InterceptTarget.TARGET_EDITOR)
        }
    }

    private fun onEditorFocusChange(event: EditorFocusChangeEvent) {
        if (!event.isGainFocus) dismiss()
    }

    private fun postDisplay() {
        if (!isShowing) return
        dismiss()

        if (!editor.cursor.isSelected) return

        editor.postDelayedInLifecycle(object : Runnable {
            override fun run() {
                if (
                    !handler.hasAnyHeldHandle() &&
                    !editor.snippetController.isInSnippet() &&
                    System.currentTimeMillis() - lastScroll > DELAY &&
                    editor.scroller.isFinished
                ) {
                    displayWindow()
                } else {
                    editor.postDelayedInLifecycle(this, DELAY)
                }
            }
        }, DELAY)
    }

    private fun selectTop(rect: RectF): Int {
        val rowHeight = editor.getRowHeight()
        return if (rect.top - rowHeight * 3 / 2f > height) {
            (rect.top - rowHeight * 3 / 2 - height).toInt()
        } else {
            (rect.bottom + rowHeight / 2).toInt()
        }
    }

    private fun displayWindow() {
        popup.contentView.measure(
            View.MeasureSpec.makeMeasureSpec(1000000, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST)
        )

        var top: Int
        val cursor = editor.cursor
        if (cursor.isSelected) {
            val leftRect = editor.leftHandleDescriptor.position
            val rightRect = editor.rightHandleDescriptor.position
            val top1 = selectTop(leftRect)
            val top2 = selectTop(rightRect)
            top = min(top1, top2)
        } else {
            top = selectTop(editor.insertHandleDescriptor.position)
        }
        top = max(0, min(top, editor.height - height - 5))
        val handleLeftX = editor.getOffset(editor.cursor.leftLine, editor.cursor.leftColumn)
        val handleRightX = editor.getOffset(editor.cursor.rightLine, editor.cursor.rightColumn)
        val panelX = ((handleLeftX + handleRightX) / 2f - popup.contentView.measuredWidth / 2f).toInt()
        setLocationAbsolutely(panelX, top)
        show()
    }
}
