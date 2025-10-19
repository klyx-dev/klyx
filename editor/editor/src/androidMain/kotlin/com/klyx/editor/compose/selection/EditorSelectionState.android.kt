package com.klyx.editor.compose.selection

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.Clipboard
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.Copy
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.Cut
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.Paste
import com.klyx.editor.compose.selection.contextmenu.TextContextMenuItems.SelectAll
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope
import com.klyx.editor.compose.selection.contextmenu.modifier.addTextContextMenuComponentsWithContext
import com.klyx.editor.compose.selection.contextmenu.textItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip: Boolean = false
    private var _hasText: Boolean = false

    actual val hasText: Boolean
        get() = _hasText

    actual val hasClip: Boolean
        get() = _hasClip

    actual suspend fun update() {
        // On Android, we don't need to read `clipEntry` to evaluate `canPaste`.
        // Reading `clipEntry` directly can trigger a "App pasted from Clipboard" system warning.
        _hasClip = clipboard.nativeClipboard.hasPrimaryClip()
        _hasText = _hasClip && clipboard.nativeClipboard.primaryClipDescription?.hasMimeType("text/*") == true
    }
}

internal actual fun Modifier.addEditorTextContextMenuComponents(
    state: EditorSelectionState,
    coroutineScope: CoroutineScope
): Modifier = addTextContextMenuComponentsWithContext { context ->
    fun TextContextMenuBuilderScope.textFieldItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        desiredState: TextToolbarState = TextToolbarState.None,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit,
    ) {
        textItem(context.resources, item, enabled) {
            onClick()
            if (closePredicate?.invoke() ?: true) close()
            state.updateTextToolbarState(desiredState)
        }
    }

    fun TextContextMenuBuilderScope.textFieldSuspendItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        textFieldItem(item, enabled) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { onClick() }
        }
    }

    addPlatformTextContextMenuItems(
        context = context,
        editable = state.editable,
        text = state.editorState.content,
        selection = state.editorState.selection,
        platformSelectionBehaviors = state.platformSelectionBehaviors,
    ) {
        with(state) {
            separator()
            textFieldSuspendItem(Cut, enabled = canShowCutMenuItem()) { cut() }
            textFieldSuspendItem(Copy, enabled = canShowCopyMenuItem()) {
                copy(cancelSelection = textToolbarShown)
            }
            textFieldSuspendItem(Paste, enabled = canShowPasteMenuItem()) { paste() }
            textFieldItem(
                item = SelectAll,
                enabled = canShowSelectAllMenuItem(),
                desiredState = TextToolbarState.Selection,
                closePredicate = { !textToolbarShown },
            ) {
                selectAll()
            }
            separator()
        }
    }
}

internal actual suspend fun EditorSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) = pointerInputScope.defaultTextFieldSelectionGestures(mouseSelectionObserver, textDragObserver)
