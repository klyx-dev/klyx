package com.klyx.editor.lsp.util

import com.klyx.core.app.GlobalApp
import com.klyx.core.app.UnsafeGlobalAccess
import com.klyx.core.file.KxFile
import com.klyx.core.language
import com.klyx.editor.language.LanguageName
import com.klyx.editor.lsp.getLanguageIdForLanguage
import com.klyx.lsp.Position
import com.klyx.lsp.Range
import com.klyx.project.Worktree
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.TextRange
import java.io.File
import java.net.URI

val Worktree.uri: URI get() = rootFile.uri
val Worktree.uriString get() = uri.toString()

val KxFile.uri: URI get() = File(absolutePath).toURI()
val KxFile.uriString get() = uri.toString()

@OptIn(UnsafeGlobalAccess::class)
val KxFile.languageId get() = getLanguageIdForLanguage(LanguageName(language()), GlobalApp) ?: "invalid"

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

