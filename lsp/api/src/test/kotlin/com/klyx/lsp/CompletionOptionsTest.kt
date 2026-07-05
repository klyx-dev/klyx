package com.klyx.lsp

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json

class CompletionOptionsTest : FunSpec({

    val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    context("CompletionItemOptions deserialization") {
        test("should deserialize with labelDetailsSupport true") {
            val jsonString = """
                {
                    "labelDetailsSupport": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionItemOptions>(jsonString)

            decoded.labelDetailsSupport shouldBe true
        }

        test("should deserialize with labelDetailsSupport false") {
            val jsonString = """
                {
                    "labelDetailsSupport": false
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionItemOptions>(jsonString)

            decoded.labelDetailsSupport shouldBe false
        }

        test("should deserialize with null labelDetailsSupport") {
            val jsonString = """
                {
                    "labelDetailsSupport": null
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionItemOptions>(jsonString)

            decoded.labelDetailsSupport shouldBe null
        }

        test("should handle empty JSON object") {
            val jsonString = "{}"

            val decoded = json.decodeFromString<CompletionItemOptions>(jsonString)

            decoded.labelDetailsSupport shouldBe null
        }

        test("should ignore unknown fields") {
            val jsonString = """
                {
                    "labelDetailsSupport": true,
                    "unknownField": "ignored"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionItemOptions>(jsonString)

            decoded.labelDetailsSupport shouldBe true
        }
    }

    context("CompletionOptions deserialization - basic fields") {
        test("should deserialize minimal options (empty object)") {
            val jsonString = "{}"

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe null
            decoded.allCommitCharacters shouldBe null
            decoded.resolveProvider shouldBe null
            decoded.completionItem shouldBe null
            decoded.workDoneProgress shouldBe null
        }

        test("should deserialize with triggerCharacters") {
            val jsonString = """
                {
                    "triggerCharacters": [".", ":", ">"]
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters.shouldNotBeNull()
            decoded.triggerCharacters shouldHaveSize 3
            decoded.triggerCharacters shouldContain "."
            decoded.triggerCharacters shouldContain ":"
            decoded.triggerCharacters shouldContain ">"
        }

        test("should deserialize with allCommitCharacters") {
            val jsonString = """
                {
                    "allCommitCharacters": [" ", "\t", "\n", ";"]
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.allCommitCharacters.shouldNotBeNull()
            decoded.allCommitCharacters shouldHaveSize 4
            decoded.allCommitCharacters shouldContain " "
            decoded.allCommitCharacters shouldContain "\t"
            decoded.allCommitCharacters shouldContain "\n"
            decoded.allCommitCharacters shouldContain ";"
        }

        test("should deserialize with resolveProvider true") {
            val jsonString = """
                {
                    "resolveProvider": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.resolveProvider shouldBe true
        }

        test("should deserialize with resolveProvider false") {
            val jsonString = """
                {
                    "resolveProvider": false
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.resolveProvider shouldBe false
        }

        test("should deserialize with completionItem options") {
            val jsonString = """
                {
                    "completionItem": {
                        "labelDetailsSupport": true
                    }
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.completionItem shouldNotBe null
            decoded.completionItem?.labelDetailsSupport shouldBe true
        }

        test("should deserialize with workDoneProgress from parent class") {
            val jsonString = """
                {
                    "workDoneProgress": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.workDoneProgress shouldBe true
        }
    }

    context("CompletionOptions deserialization - combined fields") {
        test("should deserialize all fields together") {
            val jsonString = """
                {
                    "triggerCharacters": [".", "::"],
                    "allCommitCharacters": ["\n", ";"],
                    "resolveProvider": true,
                    "completionItem": {
                        "labelDetailsSupport": true
                    },
                    "workDoneProgress": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(".", "::")
            decoded.allCommitCharacters shouldBe listOf("\n", ";")
            decoded.resolveProvider shouldBe true
            decoded.completionItem?.labelDetailsSupport shouldBe true
            decoded.workDoneProgress.shouldBeTrue()
        }

        test("should deserialize with partial fields") {
            val jsonString = """
                {
                    "triggerCharacters": ["."],
                    "resolveProvider": false
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(".")
            decoded.resolveProvider shouldBe false
            decoded.allCommitCharacters shouldBe null
            decoded.completionItem shouldBe null
            decoded.workDoneProgress shouldBe null
        }

        test("should handle completionItem with null labelDetailsSupport") {
            val jsonString = """
                {
                    "triggerCharacters": ["."],
                    "completionItem": {
                        "labelDetailsSupport": null
                    }
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.completionItem shouldNotBe null
            decoded.completionItem?.labelDetailsSupport shouldBe null
        }
    }

    context("Real-world LSP completion options") {
        test("should deserialize JavaScript/TypeScript server options") {
            val jsonString = """
                {
                    "triggerCharacters": [".", "(", "'", "\"", "`", "/", "@", "<"],
                    "allCommitCharacters": [" ", "\t", "\n", ";", ",", ".", "(", ")"],
                    "resolveProvider": true,
                    "completionItem": {
                        "labelDetailsSupport": true
                    },
                    "workDoneProgress": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters?.shouldHaveSize(8)
            decoded.allCommitCharacters?.shouldHaveSize(8)
            decoded.resolveProvider shouldBe true
            decoded.completionItem?.labelDetailsSupport shouldBe true
            decoded.workDoneProgress shouldBe true
        }

        test("should deserialize Python language server options") {
            val jsonString = """
                {
                    "triggerCharacters": [".", "(", ","],
                    "resolveProvider": true,
                    "workDoneProgress": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(".", "(", ",")
            decoded.resolveProvider shouldBe true
            decoded.workDoneProgress shouldBe true
            decoded.allCommitCharacters shouldBe null
            decoded.completionItem shouldBe null
        }

        test("should deserialize Rust analyzer options") {
            val jsonString = """
                {
                    "triggerCharacters": [":", ".", ">"],
                    "allCommitCharacters": [";", ","],
                    "resolveProvider": true,
                    "completionItem": {
                        "labelDetailsSupport": true
                    },
                    "workDoneProgress": false
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(":", ".", ">")
            decoded.allCommitCharacters shouldBe listOf(";", ",")
            decoded.resolveProvider shouldBe true
            decoded.completionItem?.labelDetailsSupport shouldBe true
            decoded.workDoneProgress shouldBe false
        }

        test("should deserialize C/C++ language server options") {
            val jsonString = """
                {
                    "triggerCharacters": [".", "->", "::"],
                    "resolveProvider": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(".", "->", "::")
            decoded.resolveProvider shouldBe true
        }

        test("should deserialize minimal language server (no features)") {
            val jsonString = """
                {
                    "resolveProvider": false
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.resolveProvider shouldBe false
            decoded.triggerCharacters shouldBe null
            decoded.allCommitCharacters shouldBe null
            decoded.completionItem shouldBe null
        }

        test("should deserialize Go language server options") {
            val jsonString = """
                {
                    "triggerCharacters": ["."],
                    "resolveProvider": true,
                    "completionItem": {
                        "labelDetailsSupport": true
                    }
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(".")
            decoded.resolveProvider shouldBe true
            decoded.completionItem?.labelDetailsSupport shouldBe true
        }
    }

    context("Edge cases and error handling") {
        test("should handle empty arrays") {
            val jsonString = """
                {
                    "triggerCharacters": [],
                    "allCommitCharacters": []
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe emptyList()
            decoded.allCommitCharacters shouldBe emptyList()
        }

        test("should handle special characters in arrays") {
            val jsonString = """
                {
                    "triggerCharacters": ["\n", "\t", "\r", "\\", "\"", "'"]
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters?.shouldHaveSize(6)
            decoded.triggerCharacters?.shouldContain("\n")
            decoded.triggerCharacters?.shouldContain("\t")
            decoded.triggerCharacters?.shouldContain("\r")
            decoded.triggerCharacters?.shouldContain("\\")
            decoded.triggerCharacters?.shouldContain("\"")
            decoded.triggerCharacters?.shouldContain("'")
        }

        test("should ignore unknown fields") {
            val jsonString = """
                {
                    "triggerCharacters": ["."],
                    "unknownField": "should be ignored",
                    "anotherUnknown": 123,
                    "nestedUnknown": {
                        "nested": "value"
                    }
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe listOf(".")
        }

        test("should handle null values explicitly set") {
            val jsonString = """
                {
                    "triggerCharacters": null,
                    "allCommitCharacters": null,
                    "resolveProvider": null,
                    "completionItem": null,
                    "workDoneProgress": null
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.triggerCharacters shouldBe null
            decoded.allCommitCharacters shouldBe null
            decoded.resolveProvider shouldBe null
            decoded.completionItem shouldBe null
            decoded.workDoneProgress shouldBe null
        }

        test("should handle completionItem as empty object") {
            val jsonString = """
                {
                    "completionItem": {}
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.completionItem shouldNotBe null
            decoded.completionItem?.labelDetailsSupport shouldBe null
        }
    }

    context("Nested structure validation") {
        test("should properly nest completionItem object") {
            val jsonString = """
                {
                    "triggerCharacters": ["."],
                    "completionItem": {
                        "labelDetailsSupport": false
                    },
                    "resolveProvider": true
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            // Verify nested structure
            decoded.completionItem shouldNotBe null
            decoded.completionItem?.labelDetailsSupport shouldBe false
            decoded.resolveProvider shouldBe true
        }

        test("should handle multiple nested fields") {
            val jsonString = """
                {
                    "completionItem": {
                        "labelDetailsSupport": true
                    },
                    "triggerCharacters": [".", "->"],
                    "workDoneProgress": false
                }
            """.trimIndent()

            val decoded = json.decodeFromString<CompletionOptions>(jsonString)

            decoded.completionItem?.labelDetailsSupport shouldBe true
            decoded.triggerCharacters shouldBe listOf(".", "->")
            decoded.workDoneProgress shouldBe false
        }
    }
})
