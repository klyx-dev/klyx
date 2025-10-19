package com.klyx.editor.compose.text

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

// This implementation detail is used by Jewel.
// When removing it, please provide an alternative of retrieving an annotated string,
// and notify a Jewel developer that they need to change the implementation.
private val annotatedStringFlavor: DataFlavor =
    DataFlavor(AnnotatedString::class.java, "AnnotatedString")

internal actual suspend fun ClipEntry.readText(): String? {
    if (!hasText()) return null

    val transferable = nativeClipEntry as? Transferable
    return withContext(Dispatchers.IO) {
        try {
            transferable?.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (_: IOException) {
            // the data is no longer available in the requested flavor
            null
        }
    }
}

internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    if (!hasAnnotatedString()) {
        if (!hasText()) return null
        return readText()?.let { AnnotatedString(it) }
    }

    val transferable = nativeClipEntry as? Transferable
    return withContext(Dispatchers.IO) {
        try {
            transferable?.getTransferData(annotatedStringFlavor) as? AnnotatedString
        } catch (_: IOException) {
            // the data is no longer available in the requested flavor
            null
        }
    }
}

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    val transferable = AnnotatedStringTransferable(this)
    return ClipEntry(transferable)
}

internal fun ClipEntry?.hasAnnotatedString(): Boolean {
    if (this == null) return false
    val transferable = nativeClipEntry as? Transferable ?: return false
    return transferable.isDataFlavorSupported(annotatedStringFlavor)
}

internal actual fun ClipEntry?.hasText(): Boolean {
    if (this == null) return false
    val transferable = nativeClipEntry as? Transferable ?: return false
    return transferable.isDataFlavorSupported(DataFlavor.stringFlavor)
}

internal actual fun androidx.compose.ui.platform.Clipboard.isReadSupported(): Boolean = true
internal actual fun androidx.compose.ui.platform.Clipboard.isWriteSupported(): Boolean = true

// Here we rely on the NativeClipboard directly instead of using ClipEntry,
// because getClipEntry is a suspend function, but in ContextMenu.desktop.kt we have older code
// expecting a synchronous execution.
// Note: the name can't be just `hasText` because NativeClipboard is a typealias to Any,
// so it would conflict with ClipEntry?.hasText declaration. Therefore, we need a unique name.
internal fun NativeClipboard.nativeClipboardHasText(): Boolean {
    val awtClipboard = this as? Clipboard ?: return false
    return awtClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
}

// Derived from StringSelection
@VisibleForTesting
internal class AnnotatedStringTransferable(
    private val data: AnnotatedString
) : Transferable, ClipboardOwner {
    override fun getTransferDataFlavors(): Array<DataFlavor?> = supportedFlavors

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
        flavor in supportedFlavors

    override fun getTransferData(flavor: DataFlavor): Any =
        when (flavor) {
            annotatedStringFlavor -> data
            DataFlavor.stringFlavor -> data.text
            else -> throw UnsupportedFlavorException(flavor)
        }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
        // Empty
    }

    companion object {
        private val supportedFlavors = arrayOf(annotatedStringFlavor, DataFlavor.stringFlavor)
    }
}
