package com.klyx.core.serializers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RegexSerializerTest : FunSpec({
    test("RegexSerializer") {
        val regex = Regex("foo")
        val serialized = Json.encodeToString(RegexSerializer, regex)
        val deserialized = Json.decodeFromString(RegexSerializer, serialized)
        deserialized shouldBe regex
        serialized shouldBe "\"foo\""
    }

    test("RegexSerializer with list") {
        val regex = List(3) { Regex("foo") }
        val serialized = Json.encodeToString(ListSerializer(RegexSerializer), regex)
        val deserialized = Json.decodeFromString(ListSerializer(RegexSerializer), serialized)
        deserialized shouldBe regex
        serialized shouldBe """["foo","foo","foo"]"""
    }
})
