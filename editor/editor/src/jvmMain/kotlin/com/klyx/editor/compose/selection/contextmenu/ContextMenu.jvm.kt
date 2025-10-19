package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.ui.platform.PlatformLocalization
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuKeys

/**
 * The default text context menu items.
 *
 * @param label The label of this item
 */
internal enum class DesktopTextContextMenuItems(val key: Any, val label: (PlatformLocalization) -> String) {
    Cut(
        key = TextContextMenuKeys.CutKey,
        label = { it.cut },
    ),
    Copy(
        key = TextContextMenuKeys.CopyKey,
        label = { it.copy },
    ),
    Paste(
        key = TextContextMenuKeys.PasteKey,
        label = { it.paste },
    ),
    SelectAll(
        key = TextContextMenuKeys.SelectAllKey,
        label = { it.selectAll },
    )
}
