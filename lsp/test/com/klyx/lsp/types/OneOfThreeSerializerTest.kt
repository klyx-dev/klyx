package com.klyx.lsp.types

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class OneOfThreeSerializerTest : FunSpec({

    // Test data classes
    @Serializable
    data class Person(val name: String, val age: Int)

    @Serializable
    data class Company(val title: String, val employees: Int)

    @Serializable
    data class Location(val city: String, val country: String)

    val json = Json {
        prettyPrint = false
        isLenient = true
    }

    context("OneOfThree.First serialization") {
        test("should serialize First value correctly") {
            val first: OneOfThree<Person, Company, Location> =
                OneOfThree.First(Person("Alice", 30))
            val encoded = json.encodeToString(first)

            encoded shouldBe """{"name":"Alice","age":30}"""
        }

        test("should serialize First with primitive type") {
            val first: OneOfThree<String, Int, Boolean> = OneOfThree.First("hello")
            val encoded = json.encodeToString(first)

            encoded shouldBe "\"hello\""
        }

        test("should use asFirst() extension") {
            val first: OneOfThree<String, Int, Boolean> = "world".asFirst()
            val encoded = json.encodeToString(first)

            encoded shouldBe "\"world\""
        }
    }

    context("OneOfThree.Second serialization") {
        test("should serialize Second value correctly") {
            val second: OneOfThree<Person, Company, Location> =
                OneOfThree.Second(Company("Acme Corp", 100))
            val encoded = json.encodeToString(second)

            encoded shouldBe """{"title":"Acme Corp","employees":100}"""
        }

        test("should serialize Second with primitive type") {
            val second: OneOfThree<String, Int, Boolean> = OneOfThree.Second(42)
            val encoded = json.encodeToString(second)

            encoded shouldBe "42"
        }

        test("should use asSecond() extension") {
            val second: OneOfThree<String, Int, Boolean> = 99.asSecond()
            val encoded = json.encodeToString(second)

            encoded shouldBe "99"
        }
    }

    context("OneOfThree.Third serialization") {
        test("should serialize Third value correctly") {
            val third: OneOfThree<Person, Company, Location> =
                OneOfThree.Third(Location("Paris", "France"))
            val encoded = json.encodeToString(third)

            encoded shouldBe """{"city":"Paris","country":"France"}"""
        }

        test("should serialize Third with primitive type") {
            val third: OneOfThree<String, Int, Boolean> = OneOfThree.Third(true)
            val encoded = json.encodeToString(third)

            encoded shouldBe "true"
        }

        test("should use asThird() extension") {
            val third: OneOfThree<String, Int, Boolean> = false.asThird()
            val encoded = json.encodeToString(third)

            encoded shouldBe "false"
        }
    }

    context("OneOfThree.First deserialization") {
        test("should deserialize to First when JSON matches First type") {
            val jsonString = """{"name":"Bob","age":25}"""
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Location>>(jsonString)

            decoded.shouldBeInstanceOf<OneOfThree.First<Person>>()
            decoded.value shouldBe Person("Bob", 25)
        }

        test("should deserialize primitive as First") {
            val jsonString = "\"world\""
            val decoded = json.decodeFromString<OneOfThree<String, Int, Boolean>>(jsonString)

            decoded.shouldBeInstanceOf<OneOfThree.First<String>>()
            decoded.value shouldBe "world"
        }

        test("should check isFirst() returns true") {
            val value: OneOfThree<String, Int, Boolean> = "test".asFirst()

            value.isFirst() shouldBe true
            value.isSecond() shouldBe false
            value.isThird() shouldBe false
        }
    }

    context("OneOfThree.Second deserialization") {
        test("should deserialize to Second when JSON matches Second type") {
            val jsonString = """{"title":"Tech Inc","employees":50}"""
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Location>>(jsonString)

            decoded.shouldBeInstanceOf<OneOfThree.Second<Company>>()
            decoded.value shouldBe Company("Tech Inc", 50)
        }

        test("should deserialize primitive as Second when First fails") {
            val jsonString = "99"
            val decoded = json.decodeFromString<OneOfThree<Person, Int, Boolean>>(jsonString)

            decoded.shouldBeInstanceOf<OneOfThree.Second<Int>>()
            decoded.value shouldBe 99
        }

        test("should check isSecond() returns true") {
            val value: OneOfThree<String, Int, Boolean> = 42.asSecond()

            value.isFirst() shouldBe false
            value.isSecond() shouldBe true
            value.isThird() shouldBe false
        }
    }

    context("OneOfThree.Third deserialization") {
        test("should deserialize to Third when JSON matches Third type") {
            val jsonString = """{"city":"London","country":"UK"}"""
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Location>>(jsonString)

            decoded.shouldBeInstanceOf<OneOfThree.Third<Location>>()
            decoded.value shouldBe Location("London", "UK")
        }

        test("should deserialize primitive as Third when First and Second fail") {
            val jsonString = "true"
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Boolean>>(jsonString)

            decoded.shouldBeInstanceOf<OneOfThree.Third<Boolean>>()
            decoded.value shouldBe true
        }

        test("should check isThird() returns true") {
            val value: OneOfThree<String, Int, Boolean> = true.asThird()

            value.isFirst() shouldBe false
            value.isSecond() shouldBe false
            value.isThird() shouldBe true
        }
    }

    context("OneOfThree round-trip serialization") {
        test("First value should survive round-trip") {
            val original: OneOfThree<Person, Company, Location> =
                OneOfThree.First(Person("Charlie", 35))
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Location>>(encoded)

            decoded.shouldBeInstanceOf<OneOfThree.First<Person>>()
            decoded.value shouldBe Person("Charlie", 35)
        }

        test("Second value should survive round-trip") {
            val original: OneOfThree<Person, Company, Location> =
                OneOfThree.Second(Company("StartUp", 10))
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Location>>(encoded)

            decoded.shouldBeInstanceOf<OneOfThree.Second<Company>>()
            decoded.value shouldBe Company("StartUp", 10)
        }

        test("Third value should survive round-trip") {
            val original: OneOfThree<Person, Company, Location> =
                OneOfThree.Third(Location("Berlin", "Germany"))
            val encoded = json.encodeToString(original)
            val decoded = json.decodeFromString<OneOfThree<Person, Company, Location>>(encoded)

            decoded.shouldBeInstanceOf<OneOfThree.Third<Location>>()
            decoded.value shouldBe Location("Berlin", "Germany")
        }

        test("Primitive values should survive round-trip") {
            val str: OneOfThree<String, Int, Boolean> = "test".asFirst()
            val num: OneOfThree<String, Int, Boolean> = 123.asSecond()
            val bool: OneOfThree<String, Int, Boolean> = false.asThird()

            val strDecoded = json.decodeFromString<OneOfThree<String, Int, Boolean>>(
                json.encodeToString(str)
            )
            val numDecoded = json.decodeFromString<OneOfThree<String, Int, Boolean>>(
                json.encodeToString(num)
            )
            val boolDecoded = json.decodeFromString<OneOfThree<String, Int, Boolean>>(
                json.encodeToString(bool)
            )

            (strDecoded as OneOfThree.First).value shouldBe "test"
            (numDecoded as OneOfThree.Second).value shouldBe 123
            (boolDecoded as OneOfThree.Third).value shouldBe false
        }
    }

    context("OneOfThree error handling") {
        test("should throw SerializationException when JSON matches no type") {
            val jsonString = """{"unknown":"field","random":123}"""

            shouldThrow<SerializationException> {
                json.decodeFromString<OneOfThree<Person, Company, Location>>(jsonString)
            }
        }

        test("should throw SerializationException for invalid JSON") {
            val jsonString = "not valid json"

            shouldThrow<SerializationException> {
                json.decodeFromString<OneOfThree<Person, Company, Location>>(jsonString)
            }
        }
    }

    context("OneOfThree with overlapping types") {
        test("should prefer First when multiple types could match") {
            @Serializable
            data class TypeA(val id: Int)

            @Serializable
            data class TypeB(val id: Int)

            @Serializable
            data class TypeC(val id: Int)

            val jsonString = """{"id":42}"""
            val decoded = json.decodeFromString<OneOfThree<TypeA, TypeB, TypeC>>(jsonString)

            // Should prefer First when all match
            decoded.shouldBeInstanceOf<OneOfThree.First<TypeA>>()
            decoded.value shouldBe TypeA(42)
        }

        test("should disambiguate based on exact primitive match") {
            val stringJson = "\"text\""
            val intJson = "42"
            val boolJson = "true"

            val str = json.decodeFromString<OneOfThree<String, Int, Boolean>>(stringJson)
            val num = json.decodeFromString<OneOfThree<String, Int, Boolean>>(intJson)
            val bool = json.decodeFromString<OneOfThree<String, Int, Boolean>>(boolJson)

            str.shouldBeInstanceOf<OneOfThree.First<String>>()
            num.shouldBeInstanceOf<OneOfThree.Second<Int>>()
            bool.shouldBeInstanceOf<OneOfThree.Third<Boolean>>()
        }
    }

    context("OneOfThree type guards") {
        test("isFirst() contract should work") {
            val value: OneOfThree<String, Int, Boolean> = "hello".asFirst()

            if (value.isFirst()) {
                // Smart cast should work here
                value.value shouldBe "hello"
            }
        }

        test("isSecond() contract should work") {
            val value: OneOfThree<String, Int, Boolean> = 100.asSecond()

            if (value.isSecond()) {
                // Smart cast should work here
                value.value shouldBe 100
            }
        }

        test("isThird() contract should work") {
            val value: OneOfThree<String, Int, Boolean> = true.asThird()

            if (value.isThird()) {
                // Smart cast should work here
                value.value shouldBe true
            }
        }
    }

    context("OneOfThree with different structure types") {
        test("should correctly deserialize objects with different fields") {
            val personJson = """{"name":"Alice","age":30}"""
            val companyJson = """{"title":"Corp","employees":100}"""
            val locationJson = """{"city":"Paris","country":"France"}"""

            val person = json.decodeFromString<OneOfThree<Person, Company, Location>>(personJson)
            val company = json.decodeFromString<OneOfThree<Person, Company, Location>>(companyJson)
            val location = json.decodeFromString<OneOfThree<Person, Company, Location>>(locationJson)

            person.shouldBeInstanceOf<OneOfThree.First<Person>>()
            company.shouldBeInstanceOf<OneOfThree.Second<Company>>()
            location.shouldBeInstanceOf<OneOfThree.Third<Location>>()
        }
    }
})
