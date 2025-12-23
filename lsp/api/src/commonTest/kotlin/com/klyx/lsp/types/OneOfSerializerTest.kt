package com.klyx.lsp.types

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class OneOfSerializerTest : FunSpec({
    // Test data classes
    @Serializable
    data class Person(val name: String, val age: Int)

    @Serializable
    data class Company(val title: String, val employees: Int)

    @Serializable
    data class Number(val value: Int)

    val json = Json {
        prettyPrint = false
        isLenient = true
    }

    context("OneOf.Left serialization") {
        test("should serialize Left value correctly") {
            val left: OneOf<Person, Company> = OneOf.Left(Person("Alice", 30))
            val encoded = json.encodeToString(left)

            encoded shouldBe """{"name":"Alice","age":30}"""
        }

        test("should serialize Left with primitive type") {
            val left: OneOf<String, Int> = OneOf.Left("hello")
            val encoded = json.encodeToString(left)

            encoded shouldBe "\"hello\""
        }
    }

    context("OneOf.Right serialization") {
        test("should serialize Right value correctly") {
            val right: OneOf<Person, Company> = OneOf.Right(Company("Acme Corp", 100))
            val encoded = json.encodeToString(right)

            encoded shouldBe """{"title":"Acme Corp","employees":100}"""
        }

        test("should serialize Right with primitive type") {
            val right: OneOf<String, Int> = OneOf.Right(42)
            val encoded = json.encodeToString(right)

            encoded shouldBe "42"
        }
    }

    context("OneOf.Left deserialization") {
        test("should deserialize to Left when JSON matches Left type") {
            val jsonString = """{"name":"Bob","age":25}"""
            val decoded = json.decodeFromString<OneOf<Person, Company>>(jsonString)

            decoded.shouldBeInstanceOf<OneOf.Left<Person>>()
            decoded.value shouldBe Person("Bob", 25)
        }

        test("should deserialize primitive as Left") {
            val jsonString = "\"world\""
            val decoded = json.decodeFromString<OneOf<String, Int>>(jsonString)

            decoded.shouldBeInstanceOf<OneOf.Left<String>>()
            decoded.value shouldBe "world"
        }
    }

    context("OneOf.Right deserialization") {
        test("should deserialize to Right when JSON matches Right type") {
            val jsonString = """{"title":"Tech Inc","employees":50}"""
            val decoded = json.decodeFromString<OneOf<Person, Company>>(jsonString)

            decoded.shouldBeInstanceOf<OneOf.Right<Company>>()
            decoded.value shouldBe Company("Tech Inc", 50)
        }

        test("should deserialize primitive as Right when Left fails") {
            val jsonString = "99"
            val decoded = json.decodeFromString<OneOf<String, Int>>(jsonString)

            decoded.shouldBeInstanceOf<OneOf.Right<Int>>()
            decoded.value shouldBe 99
        }
    }

    context("OneOf round-trip serialization") {
        test("Left value should survive round-trip") {
            val original: OneOf<Person, Company> = OneOf.Left(Person("Charlie", 35))
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<OneOf<Person, Company>>(encoded)

            decoded.shouldBeInstanceOf<OneOf.Left<Person>>()
            decoded.value shouldBe Person("Charlie", 35)
        }

        test("Right value should survive round-trip") {
            val original: OneOf<Person, Company> = OneOf.Right(Company("StartUp", 10))
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<OneOf<Person, Company>>(encoded)

            decoded.shouldBeInstanceOf<OneOf.Right<Company>>()
            decoded.value shouldBe Company("StartUp", 10)
        }
    }

    context("OneOf error handling") {
        test("should throw SerializationException when JSON matches neither type") {
            val jsonString = """{"unknown":"field","random":123}"""

            shouldThrow<SerializationException> {
                json.decodeFromString<OneOf<Person, Company>>(jsonString)
            }
        }

        test("should throw SerializationException for invalid JSON") {
            val jsonString = "not valid json"

            shouldThrow<SerializationException> {
                json.decodeFromString<OneOf<Person, Number>>(jsonString)
            }
        }
    }

    context("OneOf with overlapping types") {
        test("should prefer Left when both types could match") {
            // When both types have same structure, Left should win
            @Serializable
            data class TypeA(val id: Int)

            @Serializable
            data class TypeB(val id: Int)

            val jsonString = """{"id":42}"""
            val decoded = json.decodeFromString<OneOf<TypeA, TypeB>>(jsonString)

            decoded.shouldBeInstanceOf<OneOf.Left<TypeA>>()
            decoded.value shouldBe TypeA(42)
        }
    }
})
