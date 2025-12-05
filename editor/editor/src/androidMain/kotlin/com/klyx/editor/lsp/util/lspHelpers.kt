package com.klyx.editor.lsp.util

import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree
import com.klyx.core.language
import com.klyx.extension.ExtensionManager
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.TextRange
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.io.File
import java.net.URI

val Worktree.uri: URI get() = rootFile.uri
val Worktree.uriString get() = uri.toString()

val KxFile.uri: URI get() = File(absolutePath).toURI()
val KxFile.uriString get() = uri.toString()

val KxFile.languageId get() = ExtensionManager.getLanguageIdForLanguage(language()) ?: "invalid"

fun String.asTextDocumentIdentifier() = TextDocumentIdentifier(this)

fun createRange(start: Position, end: Position): Range {
    return Range(start, end)
}

fun createRange(start: CharPosition, end: CharPosition): Range {
    return createRange(start.asLspPosition(), end.asLspPosition())
}

fun createPosition(line: Int, character: Int): Position {
    return Position(line, character)
}

fun CharPosition.asLspPosition(): Position {
    return Position(this.line, this.column)
}

fun TextRange.asLspRange(): Range {
    return Range(this.start.asLspPosition(), this.end.asLspPosition())
}

