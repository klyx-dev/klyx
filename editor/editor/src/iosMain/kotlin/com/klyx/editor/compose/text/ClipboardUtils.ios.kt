@file:OptIn(ExperimentalComposeUiApi::class)

package com.klyx.editor.compose.text

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

internal actual suspend fun ClipEntry.readText(): String? = getPlainText()

internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    val text = getPlainText() ?: return null
    return AnnotatedString(text)
}

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    return ClipEntry.withPlainText(this.text)
}

internal actual fun ClipEntry?.hasText(): Boolean = this?.hasPlainText() ?: false

internal actual fun Clipboard.isReadSupported(): Boolean = true
internal actual fun Clipboard.isWriteSupported(): Boolean = true
