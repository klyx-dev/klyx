package com.klyx.editor.compose.selection

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastAll
import com.klyx.editor.compose.selection.EditorSelectionState.InputType
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuKeys
import com.klyx.editor.compose.selection.contextmenu.modifier.addTextContextMenuComponents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip = false
    private var _hasText = false

    actual val hasText: Boolean get() = _hasText
    actual val hasClip: Boolean get() = _hasClip

    actual suspend fun update() {
        val nativeClipboard = clipboard.nativeClipboard
        _hasClip = nativeClipboard.numberOfItems > 0
        _hasText = nativeClipboard.hasStrings
    }
}

internal actual fun Modifier.addEditorTextContextMenuComponents(
    state: EditorSelectionState,
    coroutineScope: CoroutineScope
): Modifier = addTextContextMenuComponents {
    fun TextContextMenuBuilderScope.textFieldItem(
        key: Any,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        addComponent(
            TextContextMenuItemWithComposableLeadingIcon(
                key = key,
                label = "$key",
                enabled = enabled,
                onClick = {
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        onClick()
                        close()
                    }
                }
            )
        )
    }

    with(state) {
        separator()
        textFieldItem(TextContextMenuKeys.CutKey, canShowCutMenuItem()) { cut() }
        textFieldItem(TextContextMenuKeys.CopyKey, canShowCopyMenuItem()) { copy() }
        textFieldItem(TextContextMenuKeys.PasteKey, canShowPasteMenuItem()) { paste() }
        textFieldItem(TextContextMenuKeys.SelectAllKey, canShowSelectAllMenuItem()) { selectAll() }
        separator()
    }
}

internal actual suspend fun EditorSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) {
    val selectionState = this
    val uiKitTextDragObserver = UIKitEditorTextDragObserver(selectionState)
    val clicksCounter = ClicksCounter(pointerInputScope.viewConfiguration)
    pointerInputScope.awaitEachGesture {
        while (true) {
            val downEvent = awaitPress({ true })
            clicksCounter.update(downEvent.changes[0])
            val isPrecise = downEvent.isMouseOrTouchPad()

            if (isPrecise && downEvent.buttons.isPrimaryPressed && downEvent.changes.fastAll { !it.isConsumed }) {
                // Use default BTF2 logic for mouse
                mouseSelection(mouseSelectionObserver, clicksCounter, downEvent)
            } else if (!isPrecise) {
                when (clicksCounter.clicks) {
                    1 -> {
                        // The default BTF2 logic, except
                        // moving text cursor without selection requires custom TextDragObserver
                        touchSelectionFirstPress(
                            observer = uiKitTextDragObserver,
                            downEvent = downEvent
                        )
                    }

                    2 -> {
                        doRepeatingTapSelection(
                            downEvent.changes.first(),
                            selectionState,
                            SelectionAdjustment.Word
                        )
                    }

                    else -> {
                        val downChange = downEvent.changes.first()
                        clearSelection(
                            downChange,
                            selectionState
                        ) // Previous selection must be cleared, otherwise this closure won't get third (and further) click
                        doRepeatingTapSelection(
                            downChange,
                            selectionState,
                            SelectionAdjustment.Paragraph
                        )
                    }
                }
            }
        }
    }
}

private fun doRepeatingTapSelection(
    touchChange: PointerInputChange,
    selectionState: EditorSelectionState,
    selectionAdjustment: SelectionAdjustment
) {
    val selectionOffset = with(selectionState.editorState) {
        offsetAt(calculateCursorPositionFromScreenOffset(touchChange.position))
    }
    touchChange.consume()

    val newSelection = selectionState.updateSelection(
        selection = selectionState.editorState.selection,
        selectionOffset,
        selectionOffset,
        isStartHandle = false,
        adjustment = selectionAdjustment
    )

    selectionState.editorState.setSelection(newSelection)
    selectionState.updateTextToolbarState(TextToolbarState.Selection)
}

private fun clearSelection(
    touchChange: PointerInputChange,
    selectionState: EditorSelectionState
) {
    val selectionOffset = with(selectionState.editorState) {
        offsetAt(calculateCursorPositionFromScreenOffset(touchChange.position))
    }

    val clearedSelection = selectionState.updateSelection(
        selection = TextRange.Zero,
        selectionOffset,
        selectionOffset,
        isStartHandle = false,
        adjustment = SelectionAdjustment.None
    )

    selectionState.editorState.setSelection(clearedSelection)
}

internal suspend fun AwaitPointerEventScope.awaitPress(
    filter: (PointerEvent) -> Boolean,
    requireUnconsumed: Boolean = true
): PointerEvent {
    var event: PointerEvent? = null

    while (event == null) {
        event = awaitPointerEvent().takeIf {
            it.isAllPressedDown(requireUnconsumed = requireUnconsumed) && filter(it)
        }
    }

    return event
}

private fun PointerEvent.isAllPressedDown(requireUnconsumed: Boolean = true) =
    type == PointerEventType.Press &&
            changes.fastAll { it.type == PointerType.Mouse && (!requireUnconsumed || !it.isConsumed) } ||
            changes.fastAll { if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed() }

private fun PointerEvent.isAllPressedUp(requireUnconsumed: Boolean = true) =
    type == PointerEventType.Release &&
            changes.fastAll { it.type == PointerType.Mouse && (!requireUnconsumed || !it.isConsumed) } ||
            changes.fastAll { if (requireUnconsumed) it.changedToUp() else it.changedToUpIgnoreConsumed() }

private class UIKitEditorTextDragObserver(
    private val selectionState: EditorSelectionState,
    private val requestFocus: () -> Unit = {}
) : TextDragObserver {

    private var dragBeginPosition: Offset = Offset.Unspecified
    private var dragTotalDistance: Offset = Offset.Zero

    private fun onDragStop() {
        // Only execute clear-up if drag was actually ongoing.
        if (dragBeginPosition.isSpecified) {
            selectionState.clearHandleDragging()
            dragBeginPosition = Offset.Unspecified
            dragTotalDistance = Offset.Zero
            selectionState.directDragGestureInitiator = InputType.None
            requestFocus()
            selectionState.clearHandleDragging()
        }
    }

    override fun onDown(point: Offset) = Unit

    override fun onUp() = Unit

    override fun onStart(startPoint: Offset) {
        if (!selectionState.enabled) return

        selectionState.directDragGestureInitiator = InputType.Touch

        dragBeginPosition = startPoint
        dragTotalDistance = Offset.Zero

        selectionState.hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // Long Press at the blank area, the cursor should show up at the end of the line.
//        if (!textFieldSelectionState.textLayoutState.isPositionOnText(startPoint)) {
//            val offset = textFieldSelectionState.textLayoutState.getOffsetForPosition(startPoint)
//            textFieldSelectionState.textFieldState.placeCursorBeforeCharAt(offset)
//        } else {
//            if (textFieldSelectionState.textFieldState.visualText.isEmpty()) return
//            val coercedOffset =
//                textFieldSelectionState.textLayoutState.coercedInVisibleBoundsOfInputText(startPoint)
//            textFieldSelectionState.placeCursorAtNearestOffset(
//                textFieldSelectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
//            )
//        }

        selectionState.showCursorHandle = true
        selectionState.updateHandleDragging(Handle.Cursor, startPoint)
    }

    override fun onDrag(delta: Offset) {
        if (!selectionState.enabled || selectionState.editorState.content.isEmpty()) return

        dragTotalDistance += delta

        val currentDragPosition = dragBeginPosition + dragTotalDistance

//        val coercedOffset =
//            textFieldSelectionState.textLayoutState.coercedInVisibleBoundsOfInputText(
//                currentDragPosition
//            )
        // A common function must be used here because in iOS during a drag the cursor should move without adjustments,
        // as it does with a single tap
//        textFieldSelectionState.placeCursorAtNearestOffset(
//            textFieldSelectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
//        )

        selectionState.updateHandleDragging(Handle.Cursor, currentDragPosition)
    }

    override fun onStop() = onDragStop()

    override fun onCancel() = onDragStop()
}
