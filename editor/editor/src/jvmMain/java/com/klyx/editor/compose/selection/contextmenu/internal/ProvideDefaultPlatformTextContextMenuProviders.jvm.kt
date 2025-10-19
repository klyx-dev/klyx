package com.klyx.editor.compose.selection.contextmenu.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.editor.compose.selection.contextmenu.provider.LocalTextContextMenuDropdownProvider

@Composable
internal actual fun ProvideDefaultPlatformTextContextMenuProviders(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val dropdownDefined = LocalTextContextMenuDropdownProvider.current != null
    if (!dropdownDefined) {
        ProvideDefaultTextContextMenuDropdown(modifier, content)
    } else {
        Box(modifier, propagateMinConstraints = true) { content() }
    }
}
