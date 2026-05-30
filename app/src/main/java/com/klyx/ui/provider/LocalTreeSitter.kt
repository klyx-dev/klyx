package com.klyx.ui.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.klyx.editor.TreeSitter

val LocalTreeSitter = staticCompositionLocalOf<TreeSitter> {
    error("No LocalTreeSitter provided")
}

@Composable
fun rememberTreeSitter(): TreeSitter {
    val context = LocalContext.current
    return remember(context) { TreeSitter(context) }
}
