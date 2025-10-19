package com.klyx.editor.compose.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.Clipboard
import com.klyx.editor.compose.selection.contextmenu.DesktopTextContextMenuItems
import com.klyx.editor.compose.selection.contextmenu.DesktopTextContextMenuItems.Copy
import com.klyx.editor.compose.selection.contextmenu.DesktopTextContextMenuItems.Cut
import com.klyx.editor.compose.selection.contextmenu.DesktopTextContextMenuItems.Paste
import com.klyx.editor.compose.selection.contextmenu.DesktopTextContextMenuItems.SelectAll
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope
import com.klyx.editor.compose.selection.contextmenu.builder.item
import com.klyx.editor.compose.selection.contextmenu.modifier.addTextContextMenuComponentsWithLocalization
import com.klyx.editor.compose.text.nativeClipboardHasText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip = false
    private var _hasText = false

    actual val hasText: Boolean get() = _hasText
    actual val hasClip: Boolean get() = _hasClip

    actual suspend fun update() {
        val nativeClipboard = (clipboard.nativeClipboard as? java.awt.datatransfer.Clipboard)
        _hasClip = nativeClipboard?.availableDataFlavors?.isNotEmpty() ?: false
        _hasText = nativeClipboard?.nativeClipboardHasText() ?: false
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal actual fun Modifier.addEditorTextContextMenuComponents(
    state: EditorSelectionState,
    coroutineScope: CoroutineScope
): Modifier = addTextContextMenuComponentsWithLocalization { localization ->
    fun TextContextMenuBuilderScope.textFieldItem(
        item: DesktopTextContextMenuItems,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        item(
            key = item.key,
            label = item.label(localization),
            enabled = enabled,
            onClick = {
                onClick()
                close()
            }
        )
    }

    fun TextContextMenuBuilderScope.textFieldSuspendItem(
        item: DesktopTextContextMenuItems,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        textFieldItem(item, enabled) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { onClick() }
        }
    }

    with(state) {
        separator()
        textFieldSuspendItem(Cut, enabled = canShowCutMenuItem()) { cut() }
        textFieldSuspendItem(Copy, enabled = canShowCopyMenuItem()) { copy(cancelSelection = false) }
        textFieldSuspendItem(Paste, enabled = canShowPasteMenuItem()) { paste() }
        textFieldItem(SelectAll, enabled = canShowSelectAllMenuItem()) { selectAll() }
        separator()
    }
}

internal actual suspend fun EditorSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) = pointerInputScope.defaultTextFieldSelectionGestures(mouseSelectionObserver, textDragObserver)
