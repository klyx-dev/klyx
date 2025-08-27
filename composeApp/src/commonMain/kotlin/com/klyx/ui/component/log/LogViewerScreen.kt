package com.klyx.ui.component.log

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.LocalLogBuffer

@Composable
fun LogViewerScreen(modifier: Modifier = Modifier) {
    val buffer = LocalLogBuffer.current
    LogViewer(buffer, modifier)
}
