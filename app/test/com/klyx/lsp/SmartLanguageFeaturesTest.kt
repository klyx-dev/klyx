package com.klyx.lsp

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class SmartLanguageFeaturesTest : FunSpec({

    test("LspServerRegistry does not launch LSP for txt and non-code formats") {
        LspServerRegistry.hasServerFor("plaintext", "txt") shouldBe false
        LspServerRegistry.hasServerFor("plaintext", "text") shouldBe false
        LspServerRegistry.hasServerFor("plaintext", "log") shouldBe false
        LspServerRegistry.hasServerFor("markdown", "md") shouldBe false
        LspServerRegistry.hasServerFor("plaintext", "") shouldBe false
    }

    test("LspServerRegistry detects configured code language servers") {
        LspServerRegistry.hasServerFor("rust", "rs") shouldBe true
        LspServerRegistry.hasServerFor("python", "py") shouldBe true
        LspServerRegistry.hasServerFor("kotlin", "kt") shouldBe true
        LspServerRegistry.hasServerFor("c", "c") shouldBe true
        LspServerRegistry.hasServerFor("typescript", "ts") shouldBe true
        LspServerRegistry.hasServerFor("json", "json") shouldBe true
    }

    test("SmartLanguageFeatures provides keywords for languages") {
        val ktKeywords = SmartLanguageFeatures.keywordsFor("kotlin")
        ktKeywords shouldContain "val"
        ktKeywords shouldContain "fun"
        ktKeywords shouldContain "class"

        val pyKeywords = SmartLanguageFeatures.keywordsFor("python")
        pyKeywords shouldContain "def"
        pyKeywords shouldContain "import"
    }

    test("extractWords returns buffer words matching prefix") {
        val text = "hello world help helper testing"
        val words = SmartLanguageFeatures.extractWords(text, "hel")
        words shouldContain "hello"
        words shouldContain "help"
        words shouldContain "helper"
    }

    test("getWordAt extracts word at cursor") {
        val line = "val fooBar = 42"
        SmartLanguageFeatures.getWordAt(line, 6) shouldBe "fooBar"
        SmartLanguageFeatures.getWordAt(line, 0) shouldBe "val"
        SmartLanguageFeatures.getWordAt(line, 3) shouldBe ""
    }

    test("SyntacticFolding computes fold regions for braces and indents") {
        val text = """
            fun test() {
                val a = 1
                val b = 2
            }
        """.trimIndent()
        val folds = SyntacticFolding.computeFoldRegions(text)
        folds.shouldNotBeEmpty()
        folds.first().startLine shouldBe 0
        folds.first().endLine shouldBe 3
    }

    test("formatTextFallback trims trailing whitespace") {
        val text = "val a = 1   \nval b = 2  \n"
        val formatted = SmartLanguageFeatures.formatTextFallback(text, "kt")
        formatted shouldBe "val a = 1\nval b = 2\n"
    }
})
