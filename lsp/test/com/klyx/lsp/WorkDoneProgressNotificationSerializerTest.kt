package com.klyx.lsp

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class WorkDoneProgressNotificationSerializerTest : FunSpec({

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        explicitNulls = false
    }

    test("serialize begin progress") {
        val value: WorkDoneProgressNotification =
            WorkDoneProgressBegin(
                title = "Indexing",
                message = "Starting",
                percentage = 0u
            )

        val encoded = json.encodeToString(value)

        encoded shouldBe """{"title":"Indexing","message":"Starting","percentage":0,"kind":"begin"}"""
    }

    test("serialize report progress") {
        val value: WorkDoneProgressNotification =
            WorkDoneProgressReport(
                message = "Scanning",
                percentage = 50u
            )

        val encoded = json.encodeToString(value)

        encoded shouldBe
                """{"message":"Scanning","percentage":50,"kind":"report"}"""
    }

    test("serialize end progress") {
        val value: WorkDoneProgressNotification =
            WorkDoneProgressEnd(
                message = "Done"
            )

        val encoded = json.encodeToString(value)

        encoded shouldBe
                """{"message":"Done","kind":"end"}"""
    }

    test("deserialize begin progress") {
        val jsonInput =
            """{"kind":"begin","title":"Indexing","message":"Starting","percentage":0}"""

        val decoded = json.decodeFromString<WorkDoneProgressNotification>(jsonInput)

        decoded shouldBe
                WorkDoneProgressBegin(
                    title = "Indexing",
                    message = "Starting",
                    percentage = 0u
                )
    }

    test("deserialize report progress") {
        val jsonInput =
            """{"kind":"report","message":"Scanning","percentage":42}"""

        val decoded = json.decodeFromString<WorkDoneProgressNotification>(jsonInput)

        decoded shouldBe
                WorkDoneProgressReport(
                    message = "Scanning",
                    percentage = 42u
                )
    }

    test("deserialize end progress") {
        val jsonInput =
            """{"kind":"end","message":"Done"}"""

        val decoded = json.decodeFromString<WorkDoneProgressNotification>(jsonInput)

        decoded shouldBe
                WorkDoneProgressEnd(
                    message = "Done"
                )
    }

    test("unknown progress kind throws error") {
        val jsonInput =
            """{"kind":"unknown","message":"???"}"""

        shouldThrow<IllegalStateException> {
            json.decodeFromString<WorkDoneProgressNotification>(jsonInput)
        }
    }
})
