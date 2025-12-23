package com.klyx.lsp

import com.klyx.lsp.types.asLeft
import com.klyx.lsp.types.asRight
import com.klyx.lsp.types.isLeft
import com.klyx.lsp.types.isRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

class TextEditTest : FunSpec({

    val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    fun position(line: Int, character: Int) = Position(line, character)
    fun range(startLine: Int, startChar: Int, endLine: Int, endChar: Int) =
        Range(position(startLine, startChar), position(endLine, endChar))

    context("TextEdit factory function and basic operations") {
        test("should create TextEdit with range and newText") {
            val range = range(0, 0, 0, 5)
            val edit = TextEdit(range, "hello")

            edit.range shouldBe range
            edit.newText shouldBe "hello"
        }

        test("should create TextEdit for insertion (start === end)") {
            val insertPos = range(1, 10, 1, 10)
            val edit = TextEdit(insertPos, "inserted text")

            edit.range.start shouldBe edit.range.end
            edit.newText shouldBe "inserted text"
        }

        test("should create TextEdit for deletion (empty newText)") {
            val deleteRange = range(2, 0, 2, 10)
            val edit = TextEdit(deleteRange, "")

            edit.newText shouldBe ""
        }

        test("should implement sealed interface") {
            val edit = TextEdit(range(0, 0, 0, 1), "a")

            edit.shouldBeInstanceOf<TextEdit>()
        }
    }

    context("TextEdit serialization and deserialization") {
        test("should serialize basic TextEdit") {
            val edit = TextEdit(range(0, 0, 0, 5), "hello")

            val encoded = json.encodeToString<TextEdit>(edit)
            val decoded = json.decodeFromString<TextEdit>(encoded)

            decoded.range shouldBe edit.range
            decoded.newText shouldBe edit.newText
        }

        test("should deserialize from JSON") {
            val jsonString = """
                {
                    "range": {
                        "start": {"line": 1, "character": 5},
                        "end": {"line": 1, "character": 10}
                    },
                    "newText": "replacement"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<TextEdit>(jsonString)

            decoded.range.start.line shouldBe 1u
            decoded.range.start.character shouldBe 5u
            decoded.range.end.line shouldBe 1u
            decoded.range.end.character shouldBe 10u
            decoded.newText shouldBe "replacement"
        }

        test("should handle empty newText") {
            val edit = TextEdit(range(0, 0, 1, 0), "")

            val encoded = json.encodeToString<TextEdit>(edit)
            val decoded = json.decodeFromString<TextEdit>(encoded)

            decoded.newText shouldBe ""
        }
    }

    context("AnnotatedTextEdit") {
        test("should create with annotation") {
            val edit = AnnotatedTextEdit(
                range = range(0, 0, 0, 5),
                newText = "hello",
                annotationId = "annotation-1"
            )

            edit.range shouldBe range(0, 0, 0, 5)
            edit.newText shouldBe "hello"
            edit.annotationId shouldBe "annotation-1"
        }

        test("should implement TextEdit interface") {
            val edit = AnnotatedTextEdit(
                range = range(0, 0, 0, 5),
                newText = "hello",
                annotationId = "annotation-1"
            )

            edit.shouldBeInstanceOf<TextEdit>()
        }

        test("should serialize and deserialize") {
            val edit = AnnotatedTextEdit(
                range = range(1, 0, 1, 10),
                newText = "annotated",
                annotationId = "change-1"
            )

            val encoded = json.encodeToString(edit)
            val decoded = json.decodeFromString<AnnotatedTextEdit>(encoded)

            decoded.range shouldBe edit.range
            decoded.newText shouldBe edit.newText
            decoded.annotationId shouldBe edit.annotationId
        }

        test("should deserialize from JSON") {
            val jsonString = """
                {
                    "range": {
                        "start": {"line": 0, "character": 0},
                        "end": {"line": 0, "character": 5}
                    },
                    "newText": "test",
                    "annotationId": "ann-1"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<AnnotatedTextEdit>(jsonString)

            decoded.newText shouldBe "test"
            decoded.annotationId shouldBe "ann-1"
        }
    }

    context("ChangeAnnotation") {
        test("should create with label only") {
            val annotation = ChangeAnnotation(label = "Rename variable")

            annotation.label shouldBe "Rename variable"
            annotation.needsConfirmation shouldBe null
            annotation.description shouldBe null
        }

        test("should create with all fields") {
            val annotation = ChangeAnnotation(
                label = "Delete file",
                needsConfirmation = true,
                description = "This will permanently delete the file"
            )

            annotation.label shouldBe "Delete file"
            annotation.needsConfirmation shouldBe true
            annotation.description shouldBe "This will permanently delete the file"
        }

        test("should serialize and deserialize") {
            val annotation = ChangeAnnotation(
                label = "Format code",
                needsConfirmation = false,
                description = "Auto-format document"
            )

            val encoded = json.encodeToString(annotation)
            val decoded = json.decodeFromString<ChangeAnnotation>(encoded)

            decoded.label shouldBe annotation.label
            decoded.needsConfirmation shouldBe annotation.needsConfirmation
            decoded.description shouldBe annotation.description
        }

        test("should handle mutable fields") {
            val annotation = ChangeAnnotation(label = "Test")
            annotation.needsConfirmation shouldBe null

            annotation.needsConfirmation = true
            annotation.needsConfirmation shouldBe true

            annotation.description = "Updated description"
            annotation.description shouldBe "Updated description"
        }
    }

    context("SnippetTextEdit") {
        test("should create with required fields") {
            val snippet = SnippetTextEdit(
                range = range(0, 0, 0, 0),
                snippet = StringValue($$"console.log($1)")
            )

            snippet.range shouldBe range(0, 0, 0, 0)
            snippet.snippet shouldBe StringValue($$"console.log($1)")
            snippet.annotationId shouldBe null
        }

        test("should create with annotation") {
            val snippet = SnippetTextEdit(
                range = range(1, 5, 1, 5),
                snippet = StringValue($$"for (let $1 = 0; $1 < $2; $1++) {\n\t$0\n}"),
                annotationId = "snippet-1"
            )

            snippet.annotationId shouldBe "snippet-1"
        }

        test("should serialize and deserialize") {
            val snippet = SnippetTextEdit(
                range = range(0, 0, 0, 0),
                snippet = StringValue("if (\$1) {\n\t\$0\n}"),
                annotationId = "conditional-snippet"
            )

            val encoded = json.encodeToString(snippet)
            val decoded = json.decodeFromString<SnippetTextEdit>(encoded)

            decoded.range shouldBe snippet.range
            decoded.snippet shouldBe snippet.snippet
            decoded.annotationId shouldBe snippet.annotationId
        }

        test("should handle mutable annotationId") {
            val snippet = SnippetTextEdit(
                range = range(0, 0, 0, 0),
                snippet = StringValue("test")
            )

            snippet.annotationId shouldBe null
            snippet.annotationId = "new-id"
            snippet.annotationId shouldBe "new-id"
        }
    }

    context("TextDocumentEdit with OneOf") {
        test("should create with TextEdit (left variant)") {
            val textEdit = TextEdit(range(0, 0, 0, 5), "hello")
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt",
                    version = 1
                ),
                edits = listOf(textEdit.asLeft())
            )

            docEdit.edits.size shouldBe 1
            docEdit.edits[0].isLeft() shouldBe true
        }

        test("should create with SnippetTextEdit (right variant)") {
            val snippet = SnippetTextEdit(
                range = range(0, 0, 0, 0),
                snippet = StringValue("snippet")
            )
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt",
                    version = 1
                ),
                edits = listOf(snippet.asRight())
            )

            docEdit.edits.size shouldBe 1
            docEdit.edits[0].isRight() shouldBe true
        }

        test("should create with mixed edits") {
            val textEdit = TextEdit(range(0, 0, 0, 5), "text")
            val snippet = SnippetTextEdit(
                range = range(1, 0, 1, 0),
                snippet = StringValue("snippet")
            )

            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt",
                    version = 1
                ),
                edits = listOf(
                    textEdit.asLeft(),
                    snippet.asRight()
                )
            )

            docEdit.edits.size shouldBe 2
            docEdit.edits[0].isLeft() shouldBe true
            docEdit.edits[1].isRight() shouldBe true
        }

        test("should serialize and deserialize with TextEdit") {
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt",
                    version = 1
                ),
                edits = listOf(
                    TextEdit(range(0, 0, 0, 5), "hello").asLeft()
                )
            )

            val encoded = json.encodeToString(docEdit)
            val decoded = json.decodeFromString<TextDocumentEdit>(encoded)

            decoded.textDocument.uri shouldBe "file:///test.txt"
            decoded.edits.size shouldBe 1
            decoded.edits[0].isLeft() shouldBe true
        }

        test("should serialize and deserialize with SnippetTextEdit") {
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt"
                ),
                edits = listOf(
                    SnippetTextEdit(
                        range = range(0, 0, 0, 0),
                        snippet = StringValue("snippet")
                    ).asRight()
                )
            )

            val encoded = json.encodeToString(docEdit)
            val decoded = json.decodeFromString<TextDocumentEdit>(encoded)

            decoded.edits.size shouldBe 1
            decoded.edits[0].isRight() shouldBe true
        }

        test("should handle AnnotatedTextEdit as TextEdit variant") {
            val annotatedEdit = AnnotatedTextEdit(
                range = range(0, 0, 0, 5),
                newText = "annotated",
                annotationId = "ann-1"
            )

            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt",
                    version = 1
                ),
                edits = listOf(annotatedEdit.asLeft())
            )

            docEdit.edits[0].isLeft() shouldBe true
            if (docEdit.edits[0].isLeft()) {
                val edit = docEdit.edits[0].left
                edit.shouldBeInstanceOf<AnnotatedTextEdit>()
                (edit as AnnotatedTextEdit).annotationId shouldBe "ann-1"
            }
        }
    }

    context("Real-world LSP scenarios") {
        test("Simple text replacement") {
            val jsonString = """
                {
                    "textDocument": {
                        "uri": "file:///project/src/main.kt",
                        "version": 5
                    },
                    "edits": [
                        {
                            "range": {
                                "start": {"line": 10, "character": 8},
                                "end": {"line": 10, "character": 16}
                            },
                            "newText": "newName"
                        }
                    ]
                }
            """.trimIndent()

            val decoded = json.decodeFromString<TextDocumentEdit>(jsonString)

            decoded.textDocument.uri shouldBe "file:///project/src/main.kt"
            decoded.textDocument.version shouldBe 5
            decoded.edits.size shouldBe 1
        }

        test("Multiple edits in one document") {
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.kt",
                    version = 3
                ),
                edits = listOf(
                    TextEdit(range(0, 0, 0, 0), "import kotlin.test.*\n").asLeft(),
                    TextEdit(range(5, 4, 5, 8), "renamed").asLeft(),
                    TextEdit(range(10, 0, 11, 0), "").asLeft() // deletion
                )
            )

            docEdit.edits.size shouldBe 3
            docEdit.edits.all { it.isLeft() } shouldBe true
        }

        test("Snippet insertion for code generation") {
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.kt"
                ),
                edits = listOf(
                    SnippetTextEdit(
                        range = range(5, 0, 5, 0),
                        snippet = StringValue("fun \${1:name}(\${2:params}): \${3:ReturnType} {\n\t\$0\n}")
                    ).asRight()
                )
            )

            val encoded = json.encodeToString(docEdit)
            val decoded = json.decodeFromString<TextDocumentEdit>(encoded)

            decoded.edits[0].isRight() shouldBe true
        }

        test("Annotated refactoring edit") {
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///legacy.kt",
                    version = 10
                ),
                edits = listOf(
                    AnnotatedTextEdit(
                        range = range(0, 0, 100, 0),
                        newText = "// Deprecated: Use newApi() instead\n",
                        annotationId = "deprecation-warning"
                    ).asLeft()
                )
            )

            docEdit.edits[0].isLeft() shouldBe true
            val edit = docEdit.edits[0].left
            edit.shouldBeInstanceOf<AnnotatedTextEdit>()
        }
    }

    context("Edge cases") {
        test("should handle empty edits list") {
            val docEdit = TextDocumentEdit(
                textDocument = OptionalVersionedTextDocumentIdentifier(
                    uri = "file:///test.txt"
                ),
                edits = emptyList()
            )

            docEdit.edits.size shouldBe 0
        }

        test("should handle multi-line replacement") {
            val multilineText = """
                fun example() {
                    println("Hello")
                    println("World")
                }
            """.trimIndent()

            val edit = TextEdit(range(5, 0, 8, 1), multilineText)

            edit.newText shouldBe multilineText
        }

        test("should handle special characters in newText") {
            val specialText = "Line1\nLine2\tTabbed\r\nLine3"
            val edit = TextEdit(range(0, 0, 0, 0), specialText)

            edit.newText shouldBe specialText
        }

        test("should handle Unicode in newText") {
            val unicodeText = "Hello 世界 🌍"
            val edit = TextEdit(range(0, 0, 0, 5), unicodeText)

            edit.newText shouldBe unicodeText
        }
    }
})
