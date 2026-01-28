package com.klyx.util.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.asAwtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
actual fun clipEntryOf(string: String) = ClipEntry(StringSelection(string))

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.paste() = withContext(Dispatchers.IO) {
    getClipEntry()?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor) as? CharSequence
}
