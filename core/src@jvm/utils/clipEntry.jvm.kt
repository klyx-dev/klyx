package com.klyx.core.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual fun clipEntryOfText(text: String): ClipEntry {
    return ClipEntry(StringSelection(text))
}
