package com.klyx.editor.compose.selection.contextmenu.builder

import android.content.res.Resources
import android.view.textclassifier.TextClassification
import androidx.annotation.DrawableRes
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuItem
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuSession
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuTextClassificationItem

/**
 * Adds an item to the list of text context menu components.
 *
 * @param key A unique key that identifies this item. Used to identify context menu items in the
 *   context menu. It is advisable to use a `data object` as a key here.
 * @param label string to display as the text of the item.
 * @param leadingIcon Icon that precedes the label in the context menu. This is expected to be a
 *   drawable resource reference. Setting this to the default value [Resources.ID_NULL] means that
 *   it will not be displayed.
 * @param onClick Action to perform upon the item being clicked/pressed.
 */
fun TextContextMenuBuilderScope.item(
    key: Any,
    label: String,
    @DrawableRes leadingIcon: Int = 0,
    onClick: TextContextMenuSession.() -> Unit,
) {
    addComponent(TextContextMenuItem(key, label, leadingIcon, onClick))
}

/**
 * Adds an item returned by [TextClassification] to the list of text context menu components.
 *
 * @param textClassification The [TextClassification] object returned by
 *   [android.view.textclassifier.TextClassifier].
 * @param index The index of the item in the list of [TextClassification.getActions] or a negative
 *   value if the [TextClassification] hold an legacy assist item.
 */
internal fun TextContextMenuBuilderScope.textClassificationItem(
    key: Any,
    textClassification: TextClassification,
    index: Int,
) {
    addComponent(TextContextMenuTextClassificationItem(key, textClassification, index))
}
