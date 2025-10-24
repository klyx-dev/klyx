@file:OptIn(ExperimentalFoundationApi::class)

package com.klyx.editor.compose.selection

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import arrow.core.getOrElse
import com.klyx.editor.compose.CodeEditorHandleState
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.checkPreconditionNotNull
import com.klyx.editor.compose.internal.detectPressDownGesture
import com.klyx.editor.compose.renderer.CurrentLineVerticalOffset
import com.klyx.editor.compose.selection.TextToolbarState.Cursor
import com.klyx.editor.compose.selection.TextToolbarState.None
import com.klyx.editor.compose.selection.TextToolbarState.Selection
import com.klyx.editor.compose.selection.contextmenu.ContextMenuScope
import com.klyx.editor.compose.selection.contextmenu.ContextMenuState
import com.klyx.editor.compose.selection.contextmenu.MenuItemsAvailability
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.Copy
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.Cut
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.Paste
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.SelectAll
import com.klyx.editor.compose.selection.contextmenu.TextItem
import com.klyx.editor.compose.selection.contextmenu.modifier.ToolbarRequester
import com.klyx.editor.compose.text.Content
import com.klyx.editor.compose.text.getLineHeight
import com.klyx.editor.compose.text.isReadSupported
import com.klyx.editor.compose.text.isWriteSupported
import com.klyx.editor.compose.text.readText
import com.klyx.editor.compose.text.toClipEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

internal class EditorSelectionState(
    val editorState: CodeEditorState,
    enabled: Boolean,
    readOnly: Boolean,
    private var density: Density,
    private var clipboard: Clipboard,
    private val toolbarRequester: ToolbarRequester,
    internal val platformSelectionBehaviors: PlatformSelectionBehaviors?,
    private val coroutineScope: CoroutineScope,
) {
    var enabled: Boolean = enabled
        private set

    var readOnly: Boolean = readOnly
        private set

    internal val editable: Boolean
        get() = enabled && !readOnly

    var isInTouchMode: Boolean by mutableStateOf(true)

    var hapticFeedBack: HapticFeedback? = null
    private var textToolbarHandler: TextToolbarHandler? = null

    enum class InputType {
        None,
        Touch,
        Mouse,
    }

    var directDragGestureInitiator: InputType by mutableStateOf(InputType.None)

    var showCursorHandle by mutableStateOf(false)

    private val textLayoutCoordinates: LayoutCoordinates?
        get() = editorState.editorLayoutCoordinates?.takeIf { it.isAttached }

    private var textToolbarState by mutableStateOf(None)
    var textToolbarShown by mutableStateOf(false)
        internal set

    private var rawHandleDragPosition by mutableStateOf(Offset.Unspecified)
    private var startTextLayoutPositionInWindow by mutableStateOf(Offset.Unspecified)

    /** Calculates the offset of currently visible bounds. */
    private val currentTextLayoutPositionInWindow: Offset
        get() = textLayoutCoordinates?.positionInWindow() ?: Offset.Unspecified

    var draggingHandle by mutableStateOf<Handle?>(null)
    val handleDragPosition: Offset
        get() =
            when {
                // nothing is being dragged.
                rawHandleDragPosition.isUnspecified -> {
                    Offset.Unspecified
                }
                // no real handle is being dragged, we need to offset the drag position by current
                // inner-decorator relative positioning.
                startTextLayoutPositionInWindow.isUnspecified -> {
                    //textLayoutState.fromDecorationToTextLayout(rawHandleDragPosition)
                    rawHandleDragPosition
                }
                // a cursor or a selection handle is being dragged, offset by comparing the current
                // and starting text layout positions.
                else -> {
                    rawHandleDragPosition +
                            (startTextLayoutPositionInWindow - currentTextLayoutPositionInWindow)
                }
            }

    private var previousSelectionLayout: SelectionLayout? = null

    /**
     * The previous offset of a drag, before selection adjustments. Only update when a selection
     * layout change has occurred, or set to -1 if a new drag begins.
     */
    private var previousRawDragOffset: Int = -1

    internal fun updateTextToolbarState(textToolbarState: TextToolbarState) {
        this.textToolbarState = textToolbarState
    }

    private suspend fun observeTextToolbarVisibility() {
        snapshotFlow { derivedVisibleContentBounds }
            .run {
                if (ComposeFoundationFlags.isNewContextMenuEnabled) {
                    /*
                     * The old context menu needs show to be called for every position update.
                     * However, the new context menu only needs show called once, then it will be
                     * updated by reading the `derivedVisibleContentBounds` directly. So, for the
                     * new context menu, only trigger a show/hide based on whether or not a position
                     * exists at all, and then the cached position in the derived state will be used
                     * by the context menu itself.
                     */
                    distinctUntilChangedBy { it == null }
                } else {
                    this
                }
            }
            .collect { rect ->
                if (rect != null) {
                    showTextToolbar(rect)
                } else {
                    hideTextToolbar()
                }
            }
    }

    private suspend fun observeTextChanges() {
        snapshotFlow { editorState.content }
            .distinctUntilChanged(Content::contentEquals)
            // first value needs to be dropped because it cannot be compared to a prior value
            .drop(1)
            .collect {
                showCursorHandle = false
                // hide the toolbar any time text content changes.
                updateTextToolbarState(None)
            }
    }

    private suspend fun showTextToolbar(contentRect: Rect) {
        if (ComposeFoundationFlags.isNewContextMenuEnabled) {
            toolbarRequester.show()
        } else {
            textToolbarHandler?.showTextToolbar(this, contentRect)
        }
    }

    internal fun getCursorHandleState(includePosition: Boolean): CodeEditorHandleState {
        val text = editorState.content
        val showCursorHandle = showCursorHandle
        val notBeingDragged = directDragGestureInitiator == InputType.None
        val draggingHandle = draggingHandle

        val visible =
            showCursorHandle &&
                    notBeingDragged &&
                    editorState.content.selection.collapsed &&
                    text.isNotEmpty() &&
                    (draggingHandle == Handle.Cursor || isCursorHandleInVisibleBounds())

        if (!visible) return CodeEditorHandleState.Hidden

        // The line height field for the cursor handle state is currently unused.
        // There is no need to calculate it.
        val lineHeight = 0f

        return CodeEditorHandleState(
            visible = true,
            position = if (includePosition) getCursorRect().bottomCenter else Offset.Unspecified,
            lineHeight = lineHeight,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = false,
        )
    }

    private fun isCursorHandleInVisibleBounds(): Boolean {
        val position = Snapshot.withoutReadObservation { getCursorRect().bottomCenter }
        return textLayoutCoordinates?.visibleBounds()?.containsInclusive(position) ?: false
    }

    fun getCursorRect() = editorState.cursorRect.copy(
        top = with(editorState) { (content.cursor.value.line - 1) * lineHeight + scrollY + CurrentLineVerticalOffset },
        bottom = with(editorState) { (content.cursor.value.line - 1) * lineHeight + lineHeight + scrollY + CurrentLineVerticalOffset },
    )

    fun update(
        hapticFeedBack: HapticFeedback,
        clipboard: Clipboard,
        showTextToolbar: TextToolbarHandler,
        density: Density,
        enabled: Boolean,
        readOnly: Boolean,
    ) {
        if (!enabled) {
            hideTextToolbar()
        }
        val previousClipboard = this.clipboard

        this.hapticFeedBack = hapticFeedBack
        this.clipboard = clipboard
        this.textToolbarHandler = showTextToolbar
        this.density = density
        this.enabled = enabled
        this.readOnly = readOnly

        if (previousClipboard !== clipboard) {
            clipboardPasteState = ClipboardPasteState(clipboard)
        }
    }

    private fun hideTextToolbar() {
        if (ComposeFoundationFlags.isNewContextMenuEnabled) {
            toolbarRequester.hide()
        } else {
            textToolbarHandler?.hideTextToolbar()
        }
    }

    suspend fun PointerInputScope.detectTouchMode() {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                isInTouchMode = !event.isMouseOrTouchPad()
            }
        }
    }

    fun clearHandleDragging() {
        draggingHandle = null
        rawHandleDragPosition = Offset.Unspecified
        startTextLayoutPositionInWindow = Offset.Unspecified
    }

    private suspend fun PointerInputScope.detectCursorHandleDragGestures() {
        var cursorDragStart = Offset.Unspecified
        var cursorDragDelta = Offset.Unspecified

        fun onDragStop() {
            // Only execute clear-up if drag was actually ongoing.
            if (cursorDragStart.isSpecified) {
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                clearHandleDragging()
            }
        }

        // b/288931376: detectDragGestures do not call onDragCancel when composable is disposed.
        try {
            detectDragGestures(
                onDragStart = {
                    // mark start drag point
                    cursorDragStart = getAdjustedCoordinates(getCursorRect().bottomCenter)
                    cursorDragDelta = Offset.Zero
                    isInTouchMode = true
                    markStartContentVisibleOffset()
                    updateHandleDragging(Handle.Cursor, cursorDragStart)
                },
                onDragEnd = { onDragStop() },
                onDragCancel = { onDragStop() },
                onDrag = onDrag@{ change, dragAmount ->
                    cursorDragDelta += dragAmount

                    updateHandleDragging(Handle.Cursor, cursorDragStart + cursorDragDelta)

                    if (placeCursorAtNearestOffset(handleDragPosition)) {
                        change.consume()
                        // TODO: only perform haptic feedback if filter does not override the
                        // change
                        hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
            )
        } finally {
            onDragStop()
        }
    }

    suspend fun PointerInputScope.cursorHandleGestures() {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) { detectTouchMode() }
            launch(start = CoroutineStart.UNDISPATCHED) { detectCursorHandleDragGestures() }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTapGestures(
                    onTap = { textToolbarState = if (textToolbarState == Cursor) None else Cursor }
                )
            }
        }
    }

    fun updateHandleDragging(handle: Handle, position: Offset) {
        draggingHandle = handle
        rawHandleDragPosition = position
    }

    /**
     * When a Selection or Cursor Handle starts being dragged, this function should be called to
     * mark the current visible offset, so that if content gets scrolled during the drag, we can
     * correctly offset the actual position where drag corresponds to.
     */
    private fun markStartContentVisibleOffset() {
        startTextLayoutPositionInWindow = currentTextLayoutPositionInWindow
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun placeCursorAtNearestOffset(offset: Offset): Boolean {
        return false
    }

    fun dispose() {
        hideTextToolbar()
        hapticFeedBack = null
    }

    fun canShowCopyMenuItem(): Boolean = isCopyAllowed() && clipboard.isWriteSupported()

    @Suppress("NOTHING_TO_INLINE")
    inline fun isCopyAllowed(): Boolean = !editorState.content.selection.collapsed

    suspend fun copy(cancelSelection: Boolean = true) {
        val valueToCopy = copyWithResult(cancelSelection) ?: return
        clipboard.setClipEntry(valueToCopy.toClipEntry())
    }

    internal fun copyWithResult(cancelSelection: Boolean = true): AnnotatedString? {
        if (!isCopyAllowed()) return null
        val selectedText = editorState.content.getSelectedText()
        return AnnotatedString(selectedText).also {
            if (cancelSelection) editorState.collapseSelection()
        }
    }

    fun canShowPasteMenuItem(): Boolean {
        if (!isPasteAllowed() || !clipboard.isReadSupported()) return false
        if (clipboardPasteState.hasText) return true
        return clipboardPasteState.hasClip
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun isPasteAllowed(): Boolean = editorState.editable

    suspend fun paste() {
        pasteAsPlainText()
    }

    private suspend fun pasteAsPlainText() {
        val clipboardText = clipboard.getClipEntry()?.readText() ?: return
        editorState.content.replaceSelectedText(clipboardText)
    }

    fun canShowSelectAllMenuItem() = editorState.content.selection.length != editorState.content.length

    fun selectAll() {
        editorState.setSelection(0, editorState.content.length)
    }

    fun canShowCutMenuItem() = isCutAllowed() && clipboard.isWriteSupported()

    @Suppress("NOTHING_TO_INLINE")
    inline fun isCutAllowed(): Boolean = !editorState.content.selection.collapsed && editorState.editable

    suspend fun cut() {
        val cutValue = cutWithResult() ?: return
        clipboard.setClipEntry(cutValue.toClipEntry())
    }

    fun cutWithResult(): AnnotatedString? {
        if (!isCutAllowed()) return null
        val selectedText = editorState.content.getSelectedText()
        return AnnotatedString(selectedText).also { editorState.content.deleteSelectedText() }
    }

    // TODO(grantapher) android ClipboardManager has a way to notify primary clip changes.
    //  That could possibly be used so that this doesn't have to be updated manually.
    private var clipboardPasteState = ClipboardPasteState(clipboard)

    suspend fun updateClipboardEntry() = clipboardPasteState.update()

    internal fun getSelectionHandleState(
        isStartHandle: Boolean,
        includePosition: Boolean,
    ): CodeEditorHandleState {
        val handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
        val selection = editorState.content.selection
        if (selection.collapsed) return CodeEditorHandleState.Hidden

        val position = getHandlePosition(isStartHandle)
        val visible =
            directDragGestureInitiator == InputType.None &&
                    (draggingHandle == handle ||
                            (textLayoutCoordinates?.visibleBounds()?.containsInclusive(position) ?: false))
        if (!visible) return CodeEditorHandleState.Hidden

        val directionOffset = if (isStartHandle) selection.start else max(selection.end - 1, 0)
        val direction = run {
            val (line, column) = editorState.cursorAt(directionOffset)
            val lineText = editorState.getLine(line)
            val result = editorState.measureText(lineText).getOrElse { return CodeEditorHandleState.Hidden }
            result.getBidiRunDirection(column)
        }
        val handlesCrossed = selection.reversed

        // Handle normally is visible when it's out of bounds but when the handle is being dragged,
        // we let it stay on the screen to maintain gesture continuation. However, we still want
        // to coerce handle's position to visible bounds to not let it jitter while scrolling the
        // TextField as the selection is expanding.
        val coercedPosition =
            if (includePosition) {
                textLayoutCoordinates?.visibleBounds()?.let { position.coerceIn(it) } ?: position
            } else {
                Offset.Unspecified
            }
        val handleOffset = if (isStartHandle) selection.start else selection.end

        return CodeEditorHandleState(
            visible = true,
            position = coercedPosition,
            lineHeight = run {
                val (line, column) = editorState.cursorAt(handleOffset)
                val lineText = editorState.getLine(line)
                val result = editorState.measureText(lineText).getOrElse { return CodeEditorHandleState.Hidden }
                result.getLineHeight(column)
            },
            direction = direction,
            handlesCrossed = handlesCrossed,
        )
    }

    private fun getHandlePosition(isStartHandle: Boolean): Offset {
        val selection = editorState.content.selection
        val offset = if (isStartHandle) {
            selection.start
        } else {
            selection.end
        }
        return getSelectionHandleCoordinates(
            editorState = editorState,
            offset = offset,
            isStart = isStartHandle,
            areHandlesCrossed = selection.reversed,
        )
    }

    suspend fun PointerInputScope.selectionHandleGestures(isStartHandle: Boolean) {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) { detectTouchMode() }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectPressDownGesture(
                    onDown = {
                        markStartContentVisibleOffset()
                        updateHandleDragging(
                            handle =
                                if (isStartHandle) {
                                    Handle.SelectionStart
                                } else {
                                    Handle.SelectionEnd
                                },
                            position = getAdjustedCoordinates(getHandlePosition(isStartHandle)),
                        )
                    },
                    onUp = { clearHandleDragging() },
                )
            }
                .invokeOnCompletion { clearHandleDragging() }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectSelectionHandleDragGestures(isStartHandle)
            }
        }
    }

    private suspend fun PointerInputScope.detectSelectionHandleDragGestures(isStartHandle: Boolean) {
        var dragBeginPosition: Offset = Offset.Unspecified
        var dragTotalDistance: Offset = Offset.Zero
        val handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd

        fun onDragStop() {
            // Only execute clear-up if drag was actually ongoing.
            if (dragBeginPosition.isSpecified) {
                clearHandleDragging()
                dragBeginPosition = Offset.Unspecified
                dragTotalDistance = Offset.Zero
                previousRawDragOffset = -1
            }
        }

        // b/288931376: detectDragGestures do not call onDragCancel when composable is disposed.
        try {
            detectDragGestures(
                onDragStart = {
                    // The position of the character where the drag gesture should begin. This is in
                    // the composable coordinates.
                    dragBeginPosition = getAdjustedCoordinates(getHandlePosition(isStartHandle))

                    // no need to call markStartContentVisibleOffset, since it was called by the
                    // initial down event.
                    updateHandleDragging(handle, dragBeginPosition)

                    // Zero out the total distance that being dragged.
                    dragTotalDistance = Offset.Zero

                    previousRawDragOffset = -1
                },
                onDragEnd = { onDragStop() },
                onDragCancel = { onDragStop() },
                onDrag = onDrag@{ _, delta ->
                    dragTotalDistance += delta
                    updateHandleDragging(handle, dragBeginPosition + dragTotalDistance)

                    val startOffset =
                        if (isStartHandle) {
                            with(editorState.content) {
                                editorState.calculateCursorPositionFromScreenOffset(handleDragPosition).offset
                            }
                        } else {
                            editorState.content.selection.start
                        }

                    val endOffset =
                        if (isStartHandle) {
                            editorState.content.selection.end
                        } else {
                            with(editorState.content) {
                                editorState.calculateCursorPositionFromScreenOffset(handleDragPosition).offset
                            }
                        }

                    val prevSelection = editorState.content.selection
                    val newSelection =
                        updateSelection(
                            selection = editorState.content.selection,
                            startOffset = startOffset,
                            endOffset = endOffset,
                            isStartHandle = isStartHandle,
                            adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                        )
                    // Do not allow selection to collapse on itself while dragging selection
                    // handles. Selection can reverse but does not collapse.
                    if (prevSelection.collapsed || !newSelection.collapsed) {
                        editorState.setSelection(newSelection)
                    }
                },
            )
        } finally {
            logDebug {
                "Selection Handle drag cancelled for " +
                        "draggingHandle: $draggingHandle definedOn: $handle"
            }
            if (draggingHandle == handle) {
                onDragStop()
            }
        }
    }

    internal val derivedVisibleContentBounds: Rect? by derivedStateOf {
        val isCollapsedSelection = editorState.content.selection.collapsed

        // toolbar is requested specifically for the current selection state
        val textToolbarStateVisible =
            isCollapsedSelection && textToolbarState == Cursor ||
                    !isCollapsedSelection && textToolbarState == Selection

        val textToolbarVisible =
            textToolbarStateVisible &&
                    draggingHandle == null && // not dragging any selection handles
                    isInTouchMode // toolbar hidden when not in touch mode

        // final visibility decision is made by contentRect visibility. if contentRect is not in
        // visible bounds, just pass Rect.Zero to the observer so that it hides the toolbar.
        // If Rect sis successfully passed to the observer, toolbar will be displayed.
        if (!textToolbarVisible) return@derivedStateOf null

        // contentRect is calculated in root coordinates.
        // VisibleBounds are in textLayoutCoordinates.
        // Convert visibleBounds to root before checking the overlap.
        val textLayoutCoordinates = textLayoutCoordinates ?: return@derivedStateOf null
        val visibleBounds = textLayoutCoordinates.visibleBounds()
        val visibleBoundsTopLeftInRoot = textLayoutCoordinates.localToRoot(visibleBounds.topLeft)
        val visibleBoundsInRoot = Rect(visibleBoundsTopLeftInRoot, visibleBounds.size)

        // contentRect can be very wide if a big part of text is selected.
        // Our toolbar should be aligned only to visible region.
        val contentRect = getContentRect()
        if (!contentRect.overlaps(visibleBoundsInRoot)) return@derivedStateOf null

        contentRect.intersect(visibleBoundsInRoot)
    }

    suspend fun startToolbarAndHandlesVisibilityObserver() {
        try {
            coroutineScope {
                launch { observeTextChanges() }
                launch { observeTextToolbarVisibility() }
            }
        } finally {
            showCursorHandle = false
            if (textToolbarState != None) {
                hideTextToolbar()
            }
        }
    }

    suspend fun PointerInputScope.detectTextFieldTapGestures(requestFocus: () -> Unit = {})=
        defaultDetectTextFieldTapGestures(this, requestFocus)

    suspend fun PointerInputScope.textFieldSelectionGestures(requestFocus: () -> Unit) =
        textFieldSelectionGestures(
            this,
            EditorMouseSelectionObserver(requestFocus),
            EditorTextDragObserver(requestFocus)
        )

    private inner class EditorMouseSelectionObserver(private val requestFocus: () -> Unit) : MouseSelectionObserver {
        private var dragBeginOffsetInText = -1
        private var dragBeginPosition: Offset = Offset.Unspecified

        private var isDoubleOrTripleClickOnly = true

        override fun onExtend(downPosition: Offset): Boolean {
            if (!enabled || editorState.content.isEmpty()) {
                return false
            }

            logDebug { "Mouse.onExtend" }
            isDoubleOrTripleClickOnly = false
            requestFocus()
            updateSelection(
                dragPosition = downPosition,
                adjustment = SelectionAdjustment.None,
                isStartOfSelection = false
            )

            return true
        }

        override fun onExtendDrag(dragPosition: Offset): Boolean {
            logDebug { "Mouse.onExtendDrag" }
            return true
        }

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment,
            clickCount: Int
        ): Boolean {
            if (!enabled || editorState.content.isEmpty()) {
                return false
            }

            isDoubleOrTripleClickOnly = clickCount >= 2

            logDebug { "Mouse.onStart" }
            directDragGestureInitiator = InputType.Mouse

            requestFocus()

            previousRawDragOffset = -1
            dragBeginOffsetInText = -1
            dragBeginPosition = downPosition

            val newSelection = updateSelection(downPosition, adjustment, isStartOfSelection = true)
            dragBeginOffsetInText = newSelection.start

            return true
        }

        override fun onDrag(
            dragPosition: Offset,
            adjustment: SelectionAdjustment
        ): Boolean {
            if (!enabled || editorState.content.isEmpty()) {
                return false
            }

            logDebug { "Mouse.onDrag $dragPosition" }
            val prevSelection = editorState.content.selection
            val newSelection = updateSelection(dragPosition, adjustment, isStartOfSelection = false)

            if (prevSelection != newSelection) {
                isDoubleOrTripleClickOnly = false
            }

            return true
        }

        override fun onDragDone() {
            logDebug { "Mouse.onDragDone" }
            directDragGestureInitiator = InputType.None
            if (isDoubleOrTripleClickOnly) {
                maybeSuggestSelectionRange()
            }
        }

        private fun updateSelection(
            dragPosition: Offset,
            adjustment: SelectionAdjustment,
            isStartOfSelection: Boolean = false,
        ): TextRange {
            val textLength = editorState.content.length
            val startOffset = if (0 <= dragBeginOffsetInText && dragBeginOffsetInText <= textLength) {
                dragBeginOffsetInText
            } else {
                with(editorState) {
                    offsetAt(calculateCursorPositionFromScreenOffset(dragPosition))
                }
            }

            val endOffset = with(editorState) {
                offsetAt(calculateCursorPositionFromScreenOffset(dragPosition))
            }

            var newSelection = updateSelection(
                selection = editorState.content.selection,
                startOffset = startOffset,
                endOffset = endOffset,
                isStartHandle = false,
                adjustment = adjustment,
                allowPreviousSelectionCollapsed = false,
                isStartOfSelection = isStartOfSelection,
            )

            // When drag starts from the end padding, we eventually need to update the start
            // point once a selection is initiated. Otherwise, startOffset is always calculated
            // from dragBeginPosition which can refer to different positions on text if
            // TextField starts scrolling.
            if (dragBeginOffsetInText == -1 && !newSelection.collapsed) {
                dragBeginOffsetInText = newSelection.start
            }

            // Although we support reversed selection, reversing the selection after it's
            // initiated via long press has a visual glitch that's hard to get rid of. When
            // handles (start/end) switch places after the selection reverts, draw happens a
            // bit late, making it obvious that selection handles switched places. We simply do
            // not allow reversed selection during long press drag.
            if (newSelection.reversed) {
                newSelection = newSelection.reverse()
            }

            editorState.setSelection(newSelection)
            updateTextToolbarState(Selection)

            return newSelection
        }
    }

    private inner class EditorTextDragObserver(private val requestFocus: () -> Unit) : TextDragObserver {
        private var dragBeginOffsetInText = -1
        private var dragBeginPosition: Offset = Offset.Unspecified
        private var dragTotalDistance: Offset = Offset.Zero
        private var actingHandle: Handle = Handle.SelectionEnd // start with a placeholder.
        private var isLongPressSelectionOnly = true

        private fun onDragStop() {
            // Only execute clear-up if drag was actually ongoing.
            if (dragBeginPosition.isSpecified) {
                logDebug { "Touch.onDragStop" }
                clearHandleDragging()
                dragBeginOffsetInText = -1
                dragBeginPosition = Offset.Unspecified
                dragTotalDistance = Offset.Zero
                previousRawDragOffset = -1

                directDragGestureInitiator = InputType.Touch
                requestFocus()
                if (isLongPressSelectionOnly) {
                    maybeSuggestSelectionRange()
                }
            }
        }

        override fun onDown(point: Offset) = Unit

        override fun onUp() = Unit

        override fun onStart(startPoint: Offset) {
            if (!enabled) return
            logDebug { "Touch.onDragStart after longPress at $startPoint" }
            // this gesture detector is applied on the decoration box. We do not need to
            // convert the gesture offset, that's going to be calculated by [handleDragPosition]
            updateHandleDragging(handle = actingHandle, position = startPoint)

            showCursorHandle = false
            directDragGestureInitiator = InputType.Touch

            dragBeginPosition = startPoint
            dragTotalDistance = Offset.Zero
            previousRawDragOffset = -1
            isLongPressSelectionOnly = true

            if (editorState.content.isEmpty()) return

            val offset = with(editorState) {
                offsetAt(calculateCursorPositionFromScreenOffset(startPoint))
            }
            val newSelection = updateSelection(
                selection = TextRange.Zero,
                startOffset = offset,
                endOffset = offset,
                isStartHandle = false,
                adjustment = SelectionAdjustment.Word
            )

            editorState.setSelection(newSelection)
            updateTextToolbarState(Selection)

            // For touch, set the begin offset to the adjusted selection.
            // When char based selection is used, we want to ensure we snap the
            // beginning offset to the start word boundary of the first selected word.
            dragBeginOffsetInText = newSelection.start
        }

        override fun onDrag(delta: Offset) {
            // selection never started, did not consume any drag
            if (!enabled || editorState.content.isEmpty()) return

            dragTotalDistance += delta

            // "start position + total delta" is not enough to understand the current
            // pointer position relative to text layout. We need to also account for any
            // changes to visible offset that's caused by auto-scrolling while dragging.
            val currentDragPosition = dragBeginPosition + dragTotalDistance

            logDebug { "Touch.onDrag at $currentDragPosition" }

            val startOffset: Int
            val endOffset: Int
            val adjustment: SelectionAdjustment

            if (
                dragBeginOffsetInText < 0 //&& // drag started in end padding
            // !textLayoutState.isPositionOnText(currentDragPosition) // still in end padding
            ) {
                startOffset = editorState.getOffsetForPosition(dragBeginPosition)
                endOffset = editorState.getOffsetForPosition(currentDragPosition)

                adjustment = if (startOffset == endOffset) {
                    // start and end is in the same end padding, keep the collapsed selection
                    SelectionAdjustment.None
                } else {
                    SelectionAdjustment.Word
                }
            } else {
                startOffset =
                    dragBeginOffsetInText.takeIf { it >= 0 }
                        ?: editorState.getOffsetForPosition(dragBeginPosition)
                endOffset = editorState.getOffsetForPosition(currentDragPosition)

                if (dragBeginOffsetInText < 0 && startOffset == endOffset) {
                    // if we are selecting starting from end padding,
                    // don't start selection until we have and un-collapsed selection.
                    return
                }

                adjustment = SelectionAdjustment.Word
                updateTextToolbarState(Selection)
            }

            val prevSelection = editorState.content.selection
            var newSelection = updateSelection(
                selection = editorState.content.selection,
                startOffset = startOffset,
                endOffset = endOffset,
                isStartHandle = false,
                adjustment = adjustment,
                allowPreviousSelectionCollapsed = false,
            )

            // When drag starts from the end padding, we eventually need to update the start
            // point once a selection is initiated. Otherwise, startOffset is always calculated
            // from dragBeginPosition which can refer to different positions on text if
            // TextField starts scrolling.
            if (dragBeginOffsetInText == -1 && !newSelection.collapsed) {
                dragBeginOffsetInText = newSelection.start
            }

            // Although we support reversed selection, reversing the selection after it's
            // initiated via long press has a visual glitch that's hard to get rid of. When
            // handles (start/end) switch places after the selection reverts, draw happens a
            // bit late, making it obvious that selection handles switched places. We simply do
            // not allow reversed selection during long press drag.
            if (newSelection.reversed) {
                newSelection = newSelection.reverse()
            }

            // if the new selection is not equal to previous selection, consider updating the
            // acting handle. Otherwise, acting handle should remain the same.
            if (newSelection != prevSelection) {
                // Find the growing direction of selection
                // - Since we do not allow reverse selection,
                //   - selection.start == selection.min
                //   - selection.end == selection.max
                // - If only start or end changes ([A, B] => [A, C]; [C, E] => [D, E])
                //   - acting handle is the changing handle.
                // - If both change, find the middle point and see how it moves.
                //   - If middle point moves right, acting handle is SelectionEnd
                //   - Otherwise, acting handle is SelectionStart
                actingHandle =
                    when {
                        newSelection.start != prevSelection.start &&
                                newSelection.end == prevSelection.end -> Handle.SelectionStart

                        newSelection.start == prevSelection.start &&
                                newSelection.end != prevSelection.end -> Handle.SelectionEnd

                        else -> {
                            val newMiddle = (newSelection.start + newSelection.end) / 2f
                            val prevMiddle = (prevSelection.start + prevSelection.end) / 2f
                            if (newMiddle > prevMiddle) {
                                Handle.SelectionEnd
                            } else {
                                Handle.SelectionStart
                            }
                        }
                    }
                isLongPressSelectionOnly = false
            }

            // Do not allow selection to collapse on itself while dragging. Selection can
            // reverse but does not collapse.
            if (prevSelection.collapsed || !newSelection.collapsed) {
                editorState.setSelection(newSelection)
            }
            updateHandleDragging(handle = actingHandle, position = currentDragPosition)
        }

        override fun onStop() = onDragStop()

        override fun onCancel() = onDragStop()
    }

    private fun CodeEditorState.getOffsetForPosition(position: Offset): Int {
        return offsetAt(calculateCursorPositionFromScreenOffset(position))
    }

    fun maybeSuggestSelectionRange() {
        val platformSelectionBehaviors = platformSelectionBehaviors ?: return
        val text = editorState.content
        val selection = editorState.content.selection

        if (text.isNotEmpty() && !selection.collapsed) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val suggestedSelection = platformSelectionBehaviors.suggestSelectionForLongPressOrDoubleClick(
                    text, selection
                )

                // Ideally, the selection suggestion job should be cancelled whenever the
                // selection or text is updated. However, implementing this for all
                // selection/editing options is unmaintainable. Therefore, we only require
                // that the text and selection remain unchanged since the selection
                // suggestion was made.
                if (
                    suggestedSelection != null &&
                    editorState.content == text &&
                    editorState.content.selection == selection &&
                    suggestedSelection != editorState.content.selection
                ) {
                    editorState.setSelection(suggestedSelection)
                }
            }
        }
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected line, and the
     * bottom is the bottom of the last selected line. The left is the leftmost handle's horizontal
     * coordinates, and the right is the rightmost handle's coordinates.
     *
     * This function expects [textLayoutCoordinates] to be non-null.
     */
    private fun getContentRect(): Rect {
        val textLayoutCoordinates =
            checkPreconditionNotNull(textLayoutCoordinates) {
                "textLayoutCoordinates should not be null."
            }

        // accept cursor position as content rect when selection is collapsed
        // contentRect is defined in text layout node coordinates, so it needs to be realigned to
        // the root container.
        if (editorState.content.selection.collapsed) {
            val cursorRect = getCursorRect()
            val topLeft = textLayoutCoordinates.localToRoot(cursorRect.topLeft)
            return Rect(topLeft, cursorRect.size)
        }
        val startOffset = textLayoutCoordinates.localToRoot(getHandlePosition(true))
        val endOffset = textLayoutCoordinates.localToRoot(getHandlePosition(false))

        val (startLine) = with(editorState.content) { cursorAt(selection.start) }
        val (endLine) = with(editorState.content) { cursorAt(selection.end) }

        val verticalOffset = { line: Int ->
            with(editorState) { (line - 1) * lineHeight + scrollY + CurrentLineVerticalOffset }
        }

        val startTop =
            textLayoutCoordinates
                .localToRoot(
                    Offset(
                        0f,
                        with(editorState) {
                            getCursorRect(content.selection.start).top + verticalOffset(startLine)
                        })
                )
                .y
        val endTop =
            textLayoutCoordinates
                .localToRoot(
                    Offset(
                        0f,
                        with(editorState) {
                            getCursorRect(content.selection.end).top + verticalOffset(endLine)
                        })
                )
                .y
        return Rect(
            left = min(startOffset.x, endOffset.x),
            right = max(startOffset.x, endOffset.x),
            top = min(startTop, endTop),
            bottom = max(startOffset.y, endOffset.y),
        )
    }

    internal fun updateSelection(
        selection: TextRange,
        startOffset: Int,
        endOffset: Int,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
        allowPreviousSelectionCollapsed: Boolean = false,
        isStartOfSelection: Boolean = false,
    ): TextRange {
        val newSelection =
            getTextFieldSelection(
                rawStartOffset = startOffset,
                rawEndOffset = endOffset,
                previousSelection =
                    selection.takeIf {
                        !isStartOfSelection && (allowPreviousSelectionCollapsed || !it.collapsed)
                    },
                isStartHandle = isStartHandle,
                adjustment = adjustment,
            )

        if (newSelection == selection) return newSelection

        val onlyChangeIsReversed =
            newSelection.reversed != selection.reversed &&
                    newSelection.run { TextRange(end, start) } == selection

        // don't haptic if we are using a mouse or if we aren't moving the selection bounds
        if (isInTouchMode && !onlyChangeIsReversed) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        return newSelection
    }

    private fun getTextFieldSelection(
        rawStartOffset: Int,
        rawEndOffset: Int,
        previousSelection: TextRange?,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): TextRange {
        // When the previous selection is null, it's allowed to have collapsed selection on
        // TextField. So we can ignore the SelectionAdjustment.Character.
        if (previousSelection == null && adjustment == SelectionAdjustment.Character) {
            return TextRange(rawStartOffset, rawEndOffset)
        }

        val selectionLayout =
            getTextFieldSelectionLayout(
                editorState = editorState,
                rawStartHandleOffset = rawStartOffset,
                rawEndHandleOffset = rawEndOffset,
                rawPreviousHandleOffset = previousRawDragOffset,
                previousSelectionRange = previousSelection ?: TextRange.Zero,
                isStartOfSelection = previousSelection == null,
                isStartHandle = isStartHandle,
            )

        if (
            previousSelection != null &&
            !selectionLayout.shouldRecomputeSelection(previousSelectionLayout)
        ) {
            return previousSelection
        }

        val result = adjustment.adjust(selectionLayout).toTextRange()
        previousSelectionLayout = selectionLayout
        previousRawDragOffset = if (isStartHandle) rawStartOffset else rawEndOffset

        return result
    }
}

private const val DEBUG = false
private const val DEBUG_TAG = "EditorSelectionState"

private fun logDebug(text: () -> String) {
    if (DEBUG) {
        println("$DEBUG_TAG: ${text()}")
    }
}

private fun TextRange.reverse() = TextRange(end, start)

internal expect fun Modifier.addEditorTextContextMenuComponents(
    state: EditorSelectionState,
    coroutineScope: CoroutineScope
): Modifier

internal inline fun EditorSelectionState.menuItem(
    enabled: Boolean,
    desiredState: TextToolbarState,
    crossinline operation: () -> Unit,
): (() -> Unit)? =
    if (!enabled) null
    else {
        {
            operation()
            updateTextToolbarState(desiredState)
        }
    }

internal fun EditorSelectionState.contextMenuBuilder(
    state: ContextMenuState,
    itemsAvailability: State<MenuItemsAvailability>,
    onMenuItemClicked: EditorSelectionState.(TextContextMenuItems) -> Unit,
): ContextMenuScope.() -> Unit = {
    fun textFieldItem(label: TextContextMenuItems, enabled: Boolean) {
        TextItem(state, label, enabled) { onMenuItemClicked(label) }
    }

    val availability: MenuItemsAvailability = itemsAvailability.value

    textFieldItem(Cut, availability.canCut)
    textFieldItem(Copy, availability.canCopy)
    textFieldItem(Paste, availability.canPaste)
    textFieldItem(SelectAll, availability.canSelectAll)
}

internal suspend fun EditorSelectionState.getContextMenuItemsAvailability(): MenuItemsAvailability {
    updateClipboardEntry()
    return MenuItemsAvailability(
        canCopy = canShowCopyMenuItem(),
        canCut = canShowCutMenuItem(),
        canPaste = canShowPasteMenuItem(),
        canSelectAll = canShowSelectAllMenuItem(),
        canAutofill = false
    )
}

internal suspend fun EditorSelectionState.defaultDetectTextFieldTapGestures(
    pointerInputScope: PointerInputScope,
    requestFocus: () -> Unit = {}
) {
    pointerInputScope.detectTapGestures(
        onTap = {
            requestFocus()

            if (enabled) {
                if (!readOnly && editorState.content.isNotEmpty()) {
                    showCursorHandle = true
                }

                // do not show any TextToolbar.
                updateTextToolbarState(None)
            }
        }
    )
}

internal expect suspend fun EditorSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
)

internal suspend fun PointerInputScope.defaultTextFieldSelectionGestures(
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) {
    awaitSelectionGestures(mouseSelectionObserver, textDragObserver)
}

/**
 * A state that indicates when to show TextToolbar.
 * - [None] Do not show the TextToolbar at all.
 * - [Cursor] if selection is collapsed and all the other criteria are met, show the TextToolbar.
 * - [Selection] if selection is expanded and all the other criteria are met, show the TextToolbar.
 *
 * @see [EditorSelectionState.observeTextToolbarVisibility]
 */
internal enum class TextToolbarState {
    None,
    Cursor,
    Selection,
}

internal interface TextToolbarHandler {
    suspend fun showTextToolbar(selectionState: EditorSelectionState, rect: Rect)

    fun hideTextToolbar()
}

/**
 * The way we calculate whether something can be pasted from Clipboard can be different on each
 * platform due to Clipboard permissions and access warnings. Furthermore, [update] may want to
 * cache information to be able to evaluate [hasText] and [hasClip] more efficiently.
 *
 * Therefore, this class provides the necessary abstraction between platforms to help access
 * [Clipboard] more effectively.
 */
internal expect class ClipboardPasteState(clipboard: Clipboard) {
    val hasText: Boolean

    val hasClip: Boolean

    suspend fun update()
}
