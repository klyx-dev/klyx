package com.klyx.core.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun clipEntryOfText(text: String): ClipEntry {
    return ClipEntry(ClipData.newPlainText("text", text))
}
