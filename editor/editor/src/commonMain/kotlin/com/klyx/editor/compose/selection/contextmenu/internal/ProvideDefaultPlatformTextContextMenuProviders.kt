package com.klyx.editor.compose.selection.contextmenu.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun ProvideDefaultPlatformTextContextMenuProviders(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
