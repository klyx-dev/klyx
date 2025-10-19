package com.klyx.editor.compose.selection.contextmenu

import android.content.res.Resources
import com.klyx.editor.compose.selection.contextmenu.builder.TextContextMenuBuilderScope
import com.klyx.editor.compose.selection.contextmenu.builder.item
import com.klyx.editor.compose.selection.contextmenu.data.TextContextMenuSession

internal fun TextContextMenuBuilderScope.textItem(
    resources: Resources,
    item: TextContextMenuItems,
    enabled: Boolean,
    onClick: TextContextMenuSession.() -> Unit,
) {
    if (enabled) {
        item(
            key = item.key,
            label = resources.getString(item.stringId.value),
            leadingIcon = item.drawableId.value,
            onClick = onClick,
        )
    }
}
