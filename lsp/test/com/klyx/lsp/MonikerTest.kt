package com.klyx.lsp

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json

class MonikerTest : FunSpec({

    val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    context("UniquenessLevel") {
        test("should have Document level") {
            val level = UniquenessLevel.Document
            level.toString() shouldBe "document"
        }

        test("should have Project level") {
            val level = UniquenessLevel.Project
            level.toString() shouldBe "project"
        }

        test("should have Group level") {
            val level = UniquenessLevel.Group
            level.toString() shouldBe "group"
        }

        test("should have Scheme level") {
            val level = UniquenessLevel.Scheme
            level.toString() shouldBe "scheme"
        }

        test("should have Global level") {
            val level = UniquenessLevel.Global
            level.toString() shouldBe "global"
        }

        test("should serialize to string value") {
            val level = UniquenessLevel.Project
            val encoded = json.encodeToString(level)

            encoded shouldBe "\"project\""
        }

        test("should deserialize from string value") {
            val jsonString = "\"global\""
            val decoded = json.decodeFromString<UniquenessLevel>(jsonString)

            decoded.toString() shouldBe "global"
        }

        test("all levels should have unique string representations") {
            val levels = setOf(
                UniquenessLevel.Document.toString(),
                UniquenessLevel.Project.toString(),
                UniquenessLevel.Group.toString(),
                UniquenessLevel.Scheme.toString(),
                UniquenessLevel.Global.toString()
            )

            levels.size shouldBe 5
        }

        test("should round-trip serialize and deserialize") {
            val original = UniquenessLevel.Scheme
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<UniquenessLevel>(encoded)

            decoded.toString() shouldBe original.toString()
        }
    }

    context("MonikerKind") {
        test("should have Import kind") {
            val kind = MonikerKind.Import
            kind.toString() shouldBe "import"
        }

        test("should have Export kind") {
            val kind = MonikerKind.Export
            kind.toString() shouldBe "export"
        }

        test("should have Local kind") {
            val kind = MonikerKind.Local
            kind.toString() shouldBe "local"
        }

        test("should serialize to string value") {
            val kind = MonikerKind.Export
            val encoded = json.encodeToString(kind)

            encoded shouldBe "\"export\""
        }

        test("should deserialize from string value") {
            val jsonString = "\"import\""
            val decoded = json.decodeFromString<MonikerKind>(jsonString)

            decoded.toString() shouldBe "import"
        }

        test("all kinds should have unique string representations") {
            val kinds = setOf(
                MonikerKind.Import.toString(),
                MonikerKind.Export.toString(),
                MonikerKind.Local.toString()
            )

            kinds.size shouldBe 3
        }

        test("should round-trip serialize and deserialize") {
            val original = MonikerKind.Local
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<MonikerKind>(encoded)

            decoded.toString() shouldBe original.toString()
        }
    }

    context("Moniker creation") {
        test("should create with all fields") {
            val moniker = Moniker(
                scheme = "tsc",
                identifier = "lib/typescript.d.ts::Array",
                unique = UniquenessLevel.Global,
                kind = MonikerKind.Export
            )

            moniker.scheme shouldBe "tsc"
            moniker.identifier shouldBe "lib/typescript.d.ts::Array"
            moniker.unique shouldBe UniquenessLevel.Global
            moniker.kind shouldBe MonikerKind.Export
        }

        test("should create with null kind") {
            val moniker = Moniker(
                scheme = "custom",
                identifier = "my.identifier",
                unique = UniquenessLevel.Document,
                kind = null
            )

            moniker.kind shouldBe null
        }

        test("should handle different schemes") {
            val schemes = listOf("tsc", ".NET", "rust-analyzer", "java", "python")

            schemes.forEach { scheme ->
                val moniker = Moniker(
                    scheme = scheme,
                    identifier = "test",
                    unique = UniquenessLevel.Global,
                    kind = MonikerKind.Export
                )
                moniker.scheme shouldBe scheme
            }
        }
    }

    context("Moniker serialization") {
        test("should serialize with all fields") {
            val moniker = Moniker(
                scheme = "tsc",
                identifier = "typescript::Array",
                unique = UniquenessLevel.Global,
                kind = MonikerKind.Export
            )

            val encoded = json.encodeToString(moniker)

            encoded.contains("\"scheme\"") shouldBe true
            encoded.contains("\"tsc\"") shouldBe true
            encoded.contains("\"identifier\"") shouldBe true
            encoded.contains("\"unique\"") shouldBe true
            encoded.contains("\"global\"") shouldBe true
            encoded.contains("\"kind\"") shouldBe true
            encoded.contains("\"export\"") shouldBe true
        }

        test("should serialize with null kind") {
            val moniker = Moniker(
                scheme = "test",
                identifier = "id",
                unique = UniquenessLevel.Document,
                kind = null
            )

            val encoded = json.encodeToString(moniker)

            encoded.contains("\"scheme\"") shouldBe true
            // kind should not be present when null and encodeDefaults is false
            encoded.contains("\"kind\"") shouldBe false
        }

        test("should round-trip with all fields") {
            val original = Moniker(
                scheme = ".NET",
                identifier = "System.Collections.Generic.List",
                unique = UniquenessLevel.Scheme,
                kind = MonikerKind.Import
            )

            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.scheme shouldBe original.scheme
            decoded.identifier shouldBe original.identifier
            decoded.unique.toString() shouldBe original.unique.toString()
            decoded.kind?.toString() shouldBe original.kind?.toString()
        }

        test("should round-trip with null kind") {
            val original = Moniker(
                scheme = "custom",
                identifier = "test::symbol",
                unique = UniquenessLevel.Project,
                kind = null
            )

            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.scheme shouldBe original.scheme
            decoded.identifier shouldBe original.identifier
            decoded.unique.toString() shouldBe original.unique.toString()
            decoded.kind shouldBe null
        }
    }

    context("Moniker deserialization") {
        test("should deserialize from JSON with all fields") {
            val jsonString = """
                {
                    "scheme": "tsc",
                    "identifier": "lib/typescript.d.ts::Promise",
                    "unique": "global",
                    "kind": "export"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<Moniker>(jsonString)

            decoded.scheme shouldBe "tsc"
            decoded.identifier shouldBe "lib/typescript.d.ts::Promise"
            decoded.unique.toString() shouldBe "global"
            decoded.kind?.toString() shouldBe "export"
        }

        test("should deserialize from JSON without kind") {
            val jsonString = """
                {
                    "scheme": "rust-analyzer",
                    "identifier": "std::vec::Vec",
                    "unique": "scheme"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<Moniker>(jsonString)

            decoded.scheme shouldBe "rust-analyzer"
            decoded.identifier shouldBe "std::vec::Vec"
            decoded.unique.toString() shouldBe "scheme"
            decoded.kind shouldBe null
        }

        test("should deserialize with null kind explicitly") {
            val jsonString = """
                {
                    "scheme": "test",
                    "identifier": "test::id",
                    "unique": "document",
                    "kind": null
                }
            """.trimIndent()

            val decoded = json.decodeFromString<Moniker>(jsonString)

            decoded.kind shouldBe null
        }
    }

    context("Real-world LSP scenarios") {
        test("TypeScript exported symbol") {
            val moniker = Moniker(
                scheme = "tsc",
                identifier = "lib/typescript.d.ts::Array::map",
                unique = UniquenessLevel.Global,
                kind = MonikerKind.Export
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.scheme shouldBe "tsc"
            decoded.kind?.toString() shouldBe "export"
            decoded.unique.toString() shouldBe "global"
        }

        test(".NET library type") {
            val jsonString = """
                {
                    "scheme": ".NET",
                    "identifier": "System.Collections.Generic.List`1",
                    "unique": "global",
                    "kind": "import"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<Moniker>(jsonString)

            decoded.scheme shouldBe ".NET"
            decoded.identifier shouldBe "System.Collections.Generic.List`1"
            decoded.unique shouldBe UniquenessLevel.Global
            decoded.kind?.toString() shouldBe "import"
        }

        test("Local variable in project") {
            val moniker = Moniker(
                scheme = "custom",
                identifier = "src/main.kt::MyClass::localVar",
                unique = UniquenessLevel.Document,
                kind = MonikerKind.Local
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.kind?.toString() shouldBe "local"
            decoded.unique shouldBe UniquenessLevel.Document
        }

        test("Rust standard library") {
            val moniker = Moniker(
                scheme = "rust-analyzer",
                identifier = "std::collections::HashMap",
                unique = UniquenessLevel.Global,
                kind = MonikerKind.Import
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.scheme shouldBe "rust-analyzer"
            decoded.identifier.contains("HashMap") shouldBe true
        }

        test("Project-scoped symbol") {
            val moniker = Moniker(
                scheme = "java",
                identifier = "com.example.project.MyClass",
                unique = UniquenessLevel.Project,
                kind = MonikerKind.Export
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.unique.toString() shouldBe "project"
            decoded.scheme shouldBe "java"
        }

        test("Group-scoped dependency") {
            val moniker = Moniker(
                scheme = "maven",
                identifier = "org.springframework:spring-core:5.3.0",
                unique = UniquenessLevel.Group,
                kind = MonikerKind.Import
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.unique.toString() shouldBe "group"
            decoded.identifier.contains("springframework") shouldBe true
        }

        test("Unknown kind symbol") {
            val jsonString = """
                {
                    "scheme": "custom",
                    "identifier": "unknown::symbol",
                    "unique": "scheme"
                }
            """.trimIndent()

            val decoded = json.decodeFromString<Moniker>(jsonString)

            decoded.scheme shouldBe "custom"
            decoded.kind shouldBe null
        }
    }

    context("Edge cases") {
        test("should handle complex identifier paths") {
            val complexId = "namespace.subnamespace.Class.InnerClass.Method<T1,T2>"
            val moniker = Moniker(
                scheme = "test",
                identifier = complexId,
                unique = UniquenessLevel.Global,
                kind = MonikerKind.Export
            )

            moniker.identifier shouldBe complexId
        }

        test("should handle special characters in identifier") {
            val specialId = "path/to/file::Class::method$1"
            val moniker = Moniker(
                scheme = "test",
                identifier = specialId,
                unique = UniquenessLevel.Document,
                kind = MonikerKind.Local
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.identifier shouldBe specialId
        }

        test("should handle Unicode in identifiers") {
            val unicodeId = "类名::方法名"
            val moniker = Moniker(
                scheme = "custom",
                identifier = unicodeId,
                unique = UniquenessLevel.Project,
                kind = null
            )

            val encoded = json.encodeToString(moniker)
            val decoded = json.decodeFromString<Moniker>(encoded)

            decoded.identifier shouldBe unicodeId
        }

        test("should handle empty-like schemes") {
            val schemes = listOf("", " ", "a")

            schemes.forEach { scheme ->
                val moniker = Moniker(
                    scheme = scheme,
                    identifier = "test",
                    unique = UniquenessLevel.Document,
                    kind = null
                )
                moniker.scheme shouldBe scheme
            }
        }
    }

    context("Value class behavior") {
        test("UniquenessLevel should be value class with no overhead") {
            val level1 = UniquenessLevel.Global
            val level2 = UniquenessLevel.Global

            level1.toString() shouldBe level2.toString()
        }

        test("MonikerKind should be value class with no overhead") {
            val kind1 = MonikerKind.Export
            val kind2 = MonikerKind.Export

            kind1.toString() shouldBe kind2.toString()
        }

        test("should handle all UniquenessLevel values") {
            val levels = listOf(
                UniquenessLevel.Document,
                UniquenessLevel.Project,
                UniquenessLevel.Group,
                UniquenessLevel.Scheme,
                UniquenessLevel.Global
            )

            levels.forEach { level ->
                level shouldNotBe null
                level.toString().isNotEmpty() shouldBe true
            }
        }

        test("should handle all MonikerKind values") {
            val kinds = listOf(
                MonikerKind.Import,
                MonikerKind.Export,
                MonikerKind.Local
            )

            kinds.forEach { kind ->
                kind shouldNotBe null
                kind.toString().isNotEmpty() shouldBe true
            }
        }
    }
})
