package com.klyx.util.clipboard

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

expect fun clipEntryOf(string: String): ClipEntry
expect suspend fun Clipboard.paste(): CharSequence?
