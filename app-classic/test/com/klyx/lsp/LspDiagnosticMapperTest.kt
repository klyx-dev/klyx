package com.klyx.lsp

import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LspDiagnosticMapperTest : FunSpec({
    test("maps the Pririri semicolon diagnostic to the whole token") {
        val result = LspDiagnosticMapper.map(
            diags = listOf(
                DiagnosticModel(0, 0, 0, 8, EditorDiagnosticSeverity.ERROR),
            ),
            text = "Pririri;",
        )

        result.getValue(0).shouldHaveSize(1)
        result.getValue(0).single().column shouldBe 0
        result.getValue(0).single().length shouldBe 8
    }

    test("keeps a zero-width end-of-line diagnostic visible") {
        val result = LspDiagnosticMapper.map(
            diags = listOf(
                DiagnosticModel(0, 8, 0, 8, EditorDiagnosticSeverity.ERROR),
            ),
            text = "Pririri;",
        )

        result.getValue(0).single().column shouldBe 8
        result.getValue(0).single().length shouldBe 1
    }

    test("maps multiline diagnostics to every affected line") {
        val result = LspDiagnosticMapper.map(
            diags = listOf(
                DiagnosticModel(0, 2, 1, 3, EditorDiagnosticSeverity.WARNING),
            ),
            text = "one two\nthree four",
        )

        result.keys shouldBe setOf(0, 1)
        result.getValue(0).single().column shouldBe 2
        result.getValue(0).single().length shouldBe 5
        result.getValue(1).single().column shouldBe 0
        result.getValue(1).single().length shouldBe 3
    }

    test("clamps stale ranges to the current editor text") {
        val result = LspDiagnosticMapper.map(
            diags = listOf(
                DiagnosticModel(20, 99, 20, 120, EditorDiagnosticSeverity.ERROR),
            ),
            text = "Pririri;",
        )

        result.keys shouldBe setOf(0)
        result.getValue(0).single().column shouldBe 8
        result.getValue(0).single().length shouldBe 1
    }
})
