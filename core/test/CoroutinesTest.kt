@file:OptIn(ExperimentalCoroutinesApi::class)

package com.klyx.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.ContinuationInterceptor

class CoroutinesTest : FunSpec({

    beforeTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    test("BackgroundScope doesn't allow main dispatchers") {
        shouldThrow<IllegalArgumentException> {
            val _ = BackgroundScope(Dispatchers.Main)
        }.message shouldBe "BackgroundScope must use a background dispatcher."

        shouldThrow<IllegalArgumentException> {
            val _ = BackgroundScope(Dispatchers.Main.immediate)
        }.message shouldBe "BackgroundScope must use a background dispatcher."
    }

    test("BackgroundScope() uses Dispatchers.Default") {
        val scope = BackgroundScope()
        scope.coroutineContext[ContinuationInterceptor] shouldBe Dispatchers.Default
    }
})
