package com.klyx.lsp

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

class TextDocumentRegistrationOptionsTest : FunSpec({

    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    context("TextDocumentRegistrationOptions factory function") {
        test("should create with null documentSelector") {
            val options = TextDocumentRegistrationOptions()

            options.documentSelector shouldBe null
        }

        test("should create with empty documentSelector") {
            val options = TextDocumentRegistrationOptions(documentSelector = emptyList())

            options.documentSelector shouldNotBe null
            options.documentSelector?.shouldBeEmpty()
        }

        test("should create with documentSelector") {
            val selector = listOf(
                DocumentFilter(language = "kotlin"),
                DocumentFilter(language = "java")
            )
            val options = TextDocumentRegistrationOptions(documentSelector = selector)

            options.documentSelector shouldBe selector
            options.documentSelector?.shouldHaveSize(2)
        }

        test("should implement sealed interface") {
            val options = TextDocumentRegistrationOptions()

            options.shouldBeInstanceOf<TextDocumentRegistrationOptions>()
        }

        test("documentSelector should be mutable") {
            val options = TextDocumentRegistrationOptions()
            options.documentSelector shouldBe null

            options.documentSelector = listOf(DocumentFilter(language = "kotlin"))
            options.documentSelector?.shouldHaveSize(1)
        }
    }

    context("Serialization") {
        test("should serialize null documentSelector") {
            val options = TextDocumentRegistrationOptions(documentSelector = null)

            val encoded = json.encodeToString(options)

            encoded shouldBe "null"
        }

        test("should serialize empty documentSelector") {
            val options = TextDocumentRegistrationOptions(documentSelector = emptyList())

            val encoded = json.encodeToString(options)

            encoded shouldBe "[]"
        }

        test("should serialize single DocumentFilter") {
            val options = TextDocumentRegistrationOptions(
                documentSelector = listOf(
                    DocumentFilter(language = "kotlin")
                )
            )

            val encoded = json.encodeToString(options)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.language shouldBe "kotlin"
        }

        test("should serialize multiple DocumentFilters") {
            val options = TextDocumentRegistrationOptions(
                documentSelector = listOf(
                    DocumentFilter(language = "kotlin", scheme = "file"),
                    DocumentFilter(language = "java", pattern = GlobPattern("**/*.java"))
                )
            )

            val encoded = json.encodeToString(options)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector?.shouldHaveSize(2)
            decoded.documentSelector?.get(0)?.language shouldBe "kotlin"
            decoded.documentSelector?.get(1)?.language shouldBe "java"
        }
    }

    context("Deserialization") {
        test("should deserialize null") {
            val jsonString = "null"

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector shouldBe null
        }

        test("should deserialize empty array") {
            val jsonString = "[]"

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector shouldNotBe null
            decoded.documentSelector?.shouldBeEmpty()
        }

        test("should deserialize single DocumentFilter with language") {
            val jsonString = """
                [
                    {
                        "language": "kotlin"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.language shouldBe "kotlin"
        }

        test("should deserialize single DocumentFilter with scheme") {
            val jsonString = """
                [
                    {
                        "scheme": "file"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.scheme shouldBe "file"
        }

        test("should deserialize single DocumentFilter with pattern") {
            val jsonString = """
                [
                    {
                        "pattern": "**/*.kt"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.pattern?.pattern shouldBe "**/*.kt"
        }

        test("should deserialize DocumentFilter with all fields") {
            val jsonString = """
                [
                    {
                        "language": "kotlin",
                        "scheme": "file",
                        "pattern": "**/*.kt"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            val filter = decoded.documentSelector?.get(0)
            filter?.language shouldBe "kotlin"
            filter?.scheme shouldBe "file"
            filter?.pattern?.pattern shouldBe "**/*.kt"
        }

        test("should deserialize multiple DocumentFilters") {
            val jsonString = """
                [
                    {
                        "language": "kotlin"
                    },
                    {
                        "language": "java"
                    },
                    {
                        "scheme": "untitled"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(3)
            decoded.documentSelector?.get(0)?.language shouldBe "kotlin"
            decoded.documentSelector?.get(1)?.language shouldBe "java"
            decoded.documentSelector?.get(2)?.scheme shouldBe "untitled"
        }
    }

    context("Round-trip serialization") {
        test("should survive round-trip with null") {
            val original = TextDocumentRegistrationOptions(documentSelector = null)

            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector shouldBe original.documentSelector
        }

        test("should survive round-trip with empty list") {
            val original = TextDocumentRegistrationOptions(documentSelector = emptyList())

            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector?.shouldBeEmpty()
        }

        test("should survive round-trip with single filter") {
            val original = TextDocumentRegistrationOptions(
                documentSelector = listOf(
                    DocumentFilter(language = "typescript", scheme = "file")
                )
            )

            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.language shouldBe "typescript"
            decoded.documentSelector?.get(0)?.scheme shouldBe "file"
        }

        test("should survive round-trip with multiple filters") {
            val original = TextDocumentRegistrationOptions(
                documentSelector = listOf(
                    DocumentFilter(language = "kotlin", scheme = "file", pattern = GlobPattern("**/*.kt")),
                    DocumentFilter(language = "java", scheme = "file", pattern = GlobPattern("**/*.java")),
                    DocumentFilter(scheme = "untitled")
                )
            )

            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector?.shouldHaveSize(3)
        }
    }

    context("Real-world LSP scenarios") {
        test("Kotlin language server registration") {
            val jsonString = """
                [
                    {
                        "language": "kotlin",
                        "scheme": "file"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.language shouldBe "kotlin"
            decoded.documentSelector?.get(0)?.scheme shouldBe "file"
        }

        test("Multi-language server (TypeScript/JavaScript)") {
            val jsonString = """
                [
                    {
                        "language": "typescript",
                        "scheme": "file"
                    },
                    {
                        "language": "javascript",
                        "scheme": "file"
                    },
                    {
                        "language": "typescriptreact",
                        "scheme": "file"
                    },
                    {
                        "language": "javascriptreact",
                        "scheme": "file"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(4)
            val languages = decoded.documentSelector?.map { it.language }
            languages?.shouldContain("typescript")
            languages?.shouldContain("javascript")
            languages?.shouldContain("typescriptreact")
            languages?.shouldContain("javascriptreact")
        }

        test("Pattern-based registration (all Python files)") {
            val jsonString = """
                [
                    {
                        "language": "python",
                        "pattern": "**/*.py"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.pattern?.pattern shouldBe "**/*.py"
        }

        test("Scheme-only registration (untitled documents)") {
            val jsonString = """
                [
                    {
                        "scheme": "untitled"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.scheme shouldBe "untitled"
            decoded.documentSelector?.get(0)?.language shouldBe null
        }

        test("Mixed filters (language + pattern)") {
            val jsonString = """
                [
                    {
                        "language": "rust",
                        "scheme": "file",
                        "pattern": "**/src/**/*.rs"
                    },
                    {
                        "language": "rust",
                        "scheme": "file",
                        "pattern": "**/tests/**/*.rs"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(2)
            decoded.documentSelector?.all { it.language == "rust" } shouldBe true
        }

        test("Client-side selector (null documentSelector)") {
            val jsonString = "null"

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            // null means use client-side document selector
            decoded.documentSelector shouldBe null
        }
    }

    context("Edge cases") {
        test("should handle DocumentFilter with only one field") {
            val jsonString = """
                [
                    {
                        "language": "go"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.shouldHaveSize(1)
            decoded.documentSelector?.get(0)?.language shouldBe "go"
            decoded.documentSelector?.get(0)?.scheme shouldBe null
            decoded.documentSelector?.get(0)?.pattern shouldBe null
        }

        test("should handle wildcard patterns") {
            val jsonString = """
                [
                    {
                        "pattern": "**/*"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.get(0)?.pattern?.pattern shouldBe "**/*"
        }

        test("should handle complex glob patterns") {
            val jsonString = """
                [
                    {
                        "pattern": "**/{src,test}/**/*.{kt,kts}"
                    }
                ]
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(jsonString)

            decoded.documentSelector?.get(0)?.pattern?.pattern shouldBe "**/{src,test}/**/*.{kt,kts}"
        }

        test("should handle various URI schemes") {
            val schemes = listOf("file", "untitled", "vscode-notebook-cell", "git", "ssh")

            val filters = schemes.map { DocumentFilter(scheme = it) }
            val options = TextDocumentRegistrationOptions(documentSelector = filters)

            val encoded = json.encodeToString(options)
            val decoded = json.decodeFromString<TextDocumentRegistrationOptions>(encoded)

            decoded.documentSelector?.shouldHaveSize(schemes.size)
            decoded.documentSelector?.map { it.scheme } shouldBe schemes
        }
    }

    context("Mutability tests") {
        test("should allow modifying documentSelector after creation") {
            val options = TextDocumentRegistrationOptions(documentSelector = null)

            options.documentSelector shouldBe null

            options.documentSelector = listOf(DocumentFilter(language = "python"))
            options.documentSelector!! shouldHaveSize 1

            options.documentSelector = listOf(
                DocumentFilter(language = "python"),
                DocumentFilter(language = "rust")
            )
            options.documentSelector!! shouldHaveSize 2

            options.documentSelector = null
            options.documentSelector shouldBe null
        }
    }

    context("DeclarationRegistrationOptions test") {
        test("should create with null documentSelector") {
            val options = DeclarationRegistrationOptions()
            options.documentSelector shouldBe null
        }

        test("should create with empty documentSelector") {
            val options = DeclarationRegistrationOptions(documentSelector = emptyList())
            options.documentSelector shouldNotBe null
            options.documentSelector?.shouldBeEmpty()
        }

        test("should create with documentSelector") {
            val selector = listOf(
                DocumentFilter(language = "kotlin"),
                DocumentFilter(language = "java")
            )
            val options = DeclarationRegistrationOptions(documentSelector = selector)
            options.documentSelector shouldBe selector
        }

        test("should implement sealed interface") {
            val options = DeclarationRegistrationOptions()
            options.shouldBeInstanceOf<DeclarationOptions>()
            options.shouldBeInstanceOf<TextDocumentRegistrationOptions>()
            options.shouldBeInstanceOf<StaticRegistrationOptions>()
        }
    }
})
