package com.klyx.core.app

import com.klyx.core.platform.currentPlatform
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication

@OptIn(ExperimentalCoroutinesApi::class)
class AppTest : FunSpec({
    coroutineTestScope = true

    val app = App(currentPlatform(), koinApplication())

    test("spawn") {
        Dispatchers.setMain(coroutineContext[CoroutineDispatcher]!!)
        var result = ""
        app.spawn { result = "hello" }
        testCoroutineScheduler.advanceUntilIdle()
        result shouldBe "hello"
    }
})
