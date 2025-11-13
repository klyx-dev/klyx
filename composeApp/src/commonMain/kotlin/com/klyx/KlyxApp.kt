package com.klyx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.ui.component.log.LogBuffer
import com.klyx.ui.theme.KlyxTheme

val LocalLogBuffer = staticCompositionLocalOf { LogBuffer(maxSize = 2000) }

@Composable
fun KlyxApp(content: @Composable () -> Unit) = ProvideCompositionLocals { KlyxTheme(content = content) }
