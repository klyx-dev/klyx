package com.klyx.ui.component.log

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LogViewerScreen(
    buffer: LogBuffer,
    modifier: Modifier = Modifier
) {
    LogViewer(buffer, modifier)
}
