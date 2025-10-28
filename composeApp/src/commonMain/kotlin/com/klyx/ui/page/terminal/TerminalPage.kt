package com.klyx.ui.page.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TerminalPage(modifier: Modifier = Modifier, onSessionFinish: () -> Unit) {
    Terminal(modifier = modifier, onSessionFinish = onSessionFinish)
}
