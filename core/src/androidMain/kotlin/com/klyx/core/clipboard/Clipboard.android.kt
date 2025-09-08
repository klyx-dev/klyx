package com.klyx.core.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun clipEntryOf(string: String) = ClipData.newPlainText("Klyx", string).toClipEntry()

actual suspend fun Clipboard.paste() = withContext(Dispatchers.IO) {
    getClipEntry()?.clipData?.getItemAt(0)?.text
}
