package com.klyx.ui.page.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.ui.component.terminal.Terminal

@Composable
actual fun Terminal(modifier: Modifier, onSessionFinish: () -> Unit) {
    Terminal(
        modifier = modifier,
        onSessionFinish = { _ -> onSessionFinish() }
    )
}
