package com.klyx.lsp

import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity


internal object LspDiagnosticMapper {
    fun map(
        diags: List<DiagnosticModel>,
        text: String,
    ): Map<Int, List<Diagnostic>> {
        val diagnosticsByLine = mutableMapOf<Int, MutableList<Diagnostic>>()
        val lines = text.split('\n')

        for (diag in diags) {
            if (lines.isEmpty()) continue
            val startLine = diag.startLine.coerceIn(0, lines.lastIndex)
            val endLine = diag.endLine.coerceIn(startLine, lines.lastIndex)

            for (line in startLine..endLine) {
                val lineLength = lines[line].length
                val column = if (line == startLine) diag.startCharacter.coerceIn(0, lineLength) else 0
                val endColumn = if (line == endLine) {
                    diag.endCharacter.coerceIn(column, lineLength)
                } else {
                    lineLength
                }
                val length = (endColumn - column).coerceAtLeast(1)
                diagnosticsByLine.getOrPut(line) { mutableListOf() }
                    .add(Diagnostic(column = column, length = length, severity = diag.severity))
            }
        }
        return diagnosticsByLine
    }
}

internal data class DiagnosticModel(
    val startLine: Int,
    val startCharacter: Int,
    val endLine: Int,
    val endCharacter: Int,
    val severity: EditorDiagnosticSeverity,
)
