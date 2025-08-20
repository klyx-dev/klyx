package com.klyx.ui.component.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.tab.Tab

@Composable
actual fun EditorTab(
    tab: Tab,
    isSelected: Boolean,
    modifier: Modifier,
    isDirty: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
}
