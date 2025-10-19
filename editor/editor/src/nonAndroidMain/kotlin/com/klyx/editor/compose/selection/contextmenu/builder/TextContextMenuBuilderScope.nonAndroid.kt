package com.klyx.editor.compose.selection.contextmenu.builder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuSession

/**
 * Adds an item to the list of text context menu components.
 *
 * @param key A unique key that identifies this item. Used to identify context menu items in the
 *   context menu. It is advisable to use a `data object` as a key here.
 * @param label string to display as the text of the item.
 * @param leadingIcon Icon that precedes the label in the context menu.
 * @param onClick Action to perform upon the item being clicked/pressed.
 */
@ExperimentalFoundationApi
fun TextContextMenuBuilderScope.item(
    key: Any,
    label: String,
    enabled: Boolean = true,
    leadingIcon: (@Composable (color: Color) -> Unit)? = null,
    onClick: TextContextMenuSession.() -> Unit,
) {
    addComponent(TextContextMenuItemWithComposableLeadingIcon(key, label, enabled, leadingIcon, onClick))
}
