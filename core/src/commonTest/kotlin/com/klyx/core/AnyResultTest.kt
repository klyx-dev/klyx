package com.klyx.core

import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.github.michaelbull.result.annotation.UnsafeResultErrorAccess
import com.github.michaelbull.result.annotation.UnsafeResultValueAccess
import com.github.michaelbull.result.fold
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

@OptIn(UnsafeResultErrorAccess::class, UnsafeResultValueAccess::class)
class AnyResultTest : FunSpec({

    test("AnyErr.msg creates string error") {
        val err = AnyErr.msg("boom")
        err.causeError shouldBe err.causeError
        err.render() shouldContain "boom"
    }

    test("AnyErr.fromAny wraps string, throwable, AnyErr") {
        val e1 = AnyErr.fromAny("x")
        e1.render() shouldContain "x"

        val throwable = IllegalStateException("bad")
        val e2 = AnyErr.fromAny(throwable)
        e2.causeError shouldBe throwable

        val orig = AnyErr.msg("orig")
        AnyErr.fromAny(orig) shouldBe orig
    }

    test("AnyErr.context adds context frames") {
        val e = AnyErr.msg("fail")
            .context("layer1")
            .context("layer2")

        e.render() shouldContain "layer1"
        e.render() shouldContain "layer2"
        e.render() shouldContain "fail"
    }

    test("anyResult success") {
        val r = anyResult { 42 }
        r shouldBe Ok(42)
    }

    test("anyResult failure using raise(error)") {
        val r = anyResult { raise("nope") }
        r.isErr shouldBe true
    }

    test("anyhow.ok/value and anyhow.err") {
        anyhow.ok(10) shouldBe Ok(10)
        anyhow.err("boom").isErr shouldBe true
        Err("x").isErr shouldBe true
        Ok(99) shouldBe Ok(99)
    }

    test("bind extracts success") {
        val result = anyResult {
            val a = Ok(5).bind()
            val b = anyhow.ok(7).bind()
            a + b
        }
        result shouldBe Ok(12)
    }

    test("bind short-circuits failure") {
        val result = anyResult {
            Ok(1).bind()
            Err("boom").bind<Int>() // Should short-circuit
            99 // never reached
        }
        result.isErr shouldBe true
    }

    test("bindAll for List") {
        val r = anyResult {
            listOf(Ok(1), Ok(2), Ok(3)).bindAll()
        }
        r shouldBe Ok(listOf(1, 2, 3))
    }

    test("bindAll failure inside list short-circuits") {
        val r = anyResult {
            listOf(Ok(1), Err("nope"), Ok(3)).bindAll()
        }
        r.isErr shouldBe true
    }

    test("ensure passes when true") {
        val r = anyResult {
            ensure(true) { "fail" }
            5
        }
        r shouldBe Ok(5)
    }

    test("ensure fails when false") {
        val r = anyResult {
            ensure(false) { "nope" }
            99
        }
        r.isErr shouldBe true
    }

    test("ensureNotNull returns value") {
        val r = anyResult {
            ensureNotNull("x") { "missing" }
        }
        r shouldBe Ok("x")
    }

    test("ensureNotNull fails for null") {
        val r = anyResult<String> {
            ensureNotNull(null) { "null!" }
        }
        r.isErr shouldBe true
    }

    test("recover returns success if no failure") {
        val r = anyResult {
            recover({ 10 }) { -1 }
        }
        r shouldBe Ok(10)
    }

    test("recover catches failure and maps") {
        val r = anyResult {
            recover(
                block = { raise("boom") },
                recover = { 999 }
            )
        }
        r shouldBe Ok(999)
    }

    test("withContext adds contextual messages on AnyErr") {
        val result = anyhow {
            withContext("outer") {
                withContext("inner") {
                    raise("bad")
                }
            }
        }

        val err = result.error
        err.render() shouldContain "outer"
        err.render() shouldContain "inner"
        err.render() shouldContain "bad"
    }

    test("catching converts thrown exception into AnyErr") {
        val result = anyResult {
            catching { throw IllegalArgumentException("oops") }
        }
        result.isErr shouldBe true
        result.fold(
            success = { error("Expected error") },
            failure = { it.causeError.message shouldBe "oops" }
        )
    }

    test("AnyResult.ok returns Some on success") {
        Ok(123).ok() shouldBe Some(123)
    }

    test("AnyResult.ok returns None on error") {
        Err("x").ok() shouldBe None
    }

    test("Kotlin Result -> AnyResult") {
        kotlin.runCatching { 5 }.toAnyResult() shouldBe Ok(5)
        kotlin.runCatching { error("boom") }.toAnyResult().isErr shouldBe true
    }

    test("Either -> AnyResult") {
        (5.right()).toAnyResult() shouldBe Ok(5)
        (AnyErr.msg("fail").left()).toAnyResult().isErr shouldBe true
    }

    test("AnyResult -> Either") {
        Ok(7).toEither() shouldBe 7.right()
        Err("x").toEither().isLeft() shouldBe true
    }

    test("AnyResult -> Kotlin Result") {
        Ok(1).toKotlinResult().getOrNull() shouldBe 1
        Err("x").toKotlinResult().exceptionOrNull()!!.message shouldContain "x"
    }

    test("context() adds context to AnyResult error") {
        val r = Err("fail").context("ctx")
        val err = r.error
        err.render() shouldContain "ctx"
        err.render() shouldContain "fail"
    }

    test("bindAll for Map") {
        val r = anyResult {
            mapOf(
                "a" to Ok(1),
                "b" to Ok(2),
                "c" to Ok(3)
            ).bindAll()
        }
        r shouldBe Ok(mapOf("a" to 1, "b" to 2, "c" to 3))
    }

    test("bindAll for Map with failure") {
        val r = anyResult {
            mapOf(
                "a" to Ok(1),
                "b" to Err("failed"),
                "c" to Ok(3)
            ).bindAll()
        }
        r.isErr shouldBe true
    }

    test("bindAll for NonEmptyList") {
        val r = anyResult {
            NonEmptyList(Ok(1), listOf(Ok(2), Ok(3))).bindAll()
        }
        r.value shouldBe NonEmptyList(1, listOf(2, 3))
    }

    test("bindAll for NonEmptyList with failure") {
        val r = anyResult {
            NonEmptyList(Ok(1), listOf(Err("boom"), Ok(3))).bindAll()
        }
        r.isErr shouldBe true
    }

    test("nested anyResult blocks") {
        val r = anyResult {
            val inner = anyhow {
                Ok(5).bind() + Ok(3).bind()
            }
            inner.bind() * 2
        }
        r shouldBe Ok(16)
    }

    test("nested anyResult with inner failure") {
        val r = anyResult {
            val inner = anyhow {
                raise("inner fail")
            }
            inner.bind()
        }
        r.isErr shouldBe true
        r.error.render() shouldContain "inner fail"
    }

    test("bail with String") {
        val r = anyResult {
            bail("emergency exit")
        }
        r.isErr shouldBe true
        r.error.render() shouldContain "emergency exit"
    }

    test("bail with Throwable") {
        val r = anyResult {
            bail(IllegalStateException("bad state"))
        }
        r.isErr shouldBe true
        r.error.causeError.shouldBeInstanceOf<IllegalStateException>()
    }

    test("bail with AnyErr") {
        val err = AnyErr.msg("custom").context("layer")
        val r = anyResult {
            bail(err)
        }
        r.isErr shouldBe true
        r.error.render() shouldContain "layer"
        r.error.render() shouldContain "custom"
    }

    test("anyhow top-level function for error") {
        val r = anyhow("direct error")
        r.isErr shouldBe true
        r.error.render() shouldContain "direct error"
    }

    test("anyhow DSL function raises error") {
        val r = anyResult {
            anyhow("raised in DSL")
        }
        r.isErr shouldBe true
    }

    test("catching success case") {
        val r = anyResult {
            catching { 42 }
        }
        r shouldBe Ok(42)
    }

    test("catching with different exception types") {
        val r1 = anyResult {
            catching { throw NullPointerException("null") }
        }
        r1.isErr shouldBe true
        r1.error.causeError.shouldBeInstanceOf<NullPointerException>()

        val r2 = anyResult {
            catching { throw IllegalArgumentException("invalid") }
        }
        r2.isErr shouldBe true
        r2.error.causeError.shouldBeInstanceOf<IllegalArgumentException>()
    }

    test("context on success preserves value") {
        val r = Ok(42).context("ignored context")
        r shouldBe Ok(42)
    }

    test("multiple context calls stack properly") {
        val r = Err("base")
            .context("c1")
            .context("c2")
            .context("c3")

        val rendered = r.error.render()
        rendered shouldContain "c3"
        rendered shouldContain "c2"
        rendered shouldContain "c1"
        rendered shouldContain "base"

        // Verify order (first context added appears first in render)
        (rendered.indexOf("c1") < rendered.indexOf("c2")) shouldBe true
        (rendered.indexOf("c2") < rendered.indexOf("c3")) shouldBe true
        (rendered.indexOf("c3") < rendered.indexOf("base")) shouldBe true
    }

    test("AnyErr.from with existing AnyErr returns same instance") {
        val original = AnyErr.msg("test")
        AnyErr.from(original) shouldBe original
    }

    test("AnyErr.from with Throwable wraps it") {
        val throwable = RuntimeException("error")
        val err = AnyErr.from(throwable)
        err.causeError shouldBe throwable
    }

    test("AnyErr.fromAny with arbitrary object") {
        val err = AnyErr.fromAny(12345)
        err.render() shouldContain "12345"
    }

    test("withContext preserves success") {
        val r = anyResult {
            withContext("ctx") {
                42
            }
        }
        r shouldBe Ok(42)
    }

    test("withContext with nested failures") {
        val r = anyResult {
            withContext("outer") {
                withContext("middle") {
                    withContext("inner") {
                        raise("core error")
                    }
                }
            }
        }

        val rendered = r.error.render()
        rendered shouldContain "outer"
        rendered shouldContain "middle"
        rendered shouldContain "inner"
        rendered shouldContain "core error"
    }

    test("recover with nested anyResult") {
        val r = anyResult {
            recover(
                block = {
                    val inner = anyhow { raise("inner") }
                    inner.bind()
                },
                recover = { 100 }
            )
        }
        r shouldBe Ok(100)
    }

    test("combining ensure and ensureNotNull") {
        val r = anyResult {
            val value: String? = "test"
            val checked = ensureNotNull(value) { "was null" }
            ensure(checked.length > 2) { "too short" }
            checked.uppercase()
        }
        r shouldBe Ok("TEST")
    }

    test("ensure with false condition stops execution") {
        var sideEffect = 0
        val r = anyResult {
            ensure(false) { "stopped" }
            sideEffect = 1
        }
        r.isErr shouldBe true
        sideEffect shouldBe 0
    }

    test("bind with Result type (non-AnyResult)") {
        val r = anyResult {
            val result: com.github.michaelbull.result.Result<Int, String> =
                com.github.michaelbull.result.Ok(42)
            result.bind()
        }
        r shouldBe Ok(42)
    }

    test("bind with Result failure") {
        val r = anyResult {
            val result: com.github.michaelbull.result.Result<Int, String> =
                com.github.michaelbull.result.Err("failed")
            result.bind()
        }
        r.isErr shouldBe true
    }

    test("complex real-world scenario") {
        data class User(val id: Int, val name: String)

        fun validateId(id: Int): AnyResult<Int> = anyResult {
            ensure(id > 0) { "ID must be positive" }
            id
        }

        fun fetchUser(id: Int): AnyResult<User> = anyResult {
            val validId = validateId(id).bind()
            withContext("fetching user $validId") {
                ensure(validId != 999) { "User not found" }
                User(validId, "User$validId")
            }
        }

        val success = fetchUser(42)
        success shouldBe Ok(User(42, "User42"))

        val invalidId = fetchUser(-1)
        invalidId.isErr shouldBe true
        invalidId.error.render() shouldContain "positive"

        val notFound = fetchUser(999)
        notFound.isErr shouldBe true
        notFound.error.render() shouldContain "not found"
        notFound.error.render() shouldContain "fetching user"
    }
})
