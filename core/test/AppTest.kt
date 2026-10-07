@file:OptIn(ExperimentalCoroutinesApi::class)

package com.klyx.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.error.NoDefinitionFoundException
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppTest : FunSpec({

    beforeTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    afterTest {
        Dispatchers.resetMain()
        stopKoin()
    }

    test("App can get instances from its own koin") {
        val testModule = module {
            single { "Hello World" }
            single { 42 }
        }
        val app = App(koinApplication { modules(testModule) })

        app.has<String>() shouldBe true
        app.has<Int>() shouldBe true
        app.has<Double>() shouldBe false

        app.has(String::class) shouldBe true
        app.has(Double::class) shouldBe false

        app.get<String>() shouldBe "Hello World"
        app.get(Int::class) shouldBe 42

        app.getOrNull<String>() shouldBe "Hello World"
        app.getOrNull<Double>().shouldBeNull()
        app.getOrNull(Double::class).shouldBeNull()

        app.getOrDefault { "fallback" } shouldBe "Hello World"
        app.getOrDefault { 3.14 } shouldBe 3.14

        val lazyString = app.inject<String>()
        lazyString.value shouldBe "Hello World"

        val lazyInt = app.inject(Int::class)
        lazyInt.value shouldBe 42

        shouldThrow<NoDefinitionFoundException> {
            app.get<Double>()
        }
    }

    test("App can get instances from default koin (KoinPlatform)") {
        startKoin {
            modules(
                module {
                    single { "Platform String" }
                    single { 100L }
                }
            )
        }

        val emptyApp = App(koinApplication { })

        emptyApp.has<String>() shouldBe true
        emptyApp.has<Long>() shouldBe true
        emptyApp.has<Double>() shouldBe false

        emptyApp.get<String>() shouldBe "Platform String"
        emptyApp.get(Long::class) shouldBe 100L

        emptyApp.getOrNull<String>() shouldBe "Platform String"
        emptyApp.getOrNull<Double>().shouldBeNull()

        emptyApp.inject<String>().value shouldBe "Platform String"
    }

    test("App prefers its own koin over default koin") {
        startKoin {
            modules(
                module {
                    single { "From Platform" }
                }
            )
        }

        val app = App(koinApplication {
            modules(
                module {
                    single { "From App" }
                }
            )
        })

        app.get<String>() shouldBe "From App"
        app.getOrNull<String>() shouldBe "From App"
    }

    test("App works with Global marker interface") {
        class MyGlobal(val value: String) : Global

        val app = App(koinApplication { })

        app.hasGlobal<MyGlobal>() shouldBe false
        app.globalOrNull<MyGlobal>().shouldBeNull()

        app.setGlobal(MyGlobal("test-global"))

        app.hasGlobal<MyGlobal>() shouldBe true
        app.global<MyGlobal>().value shouldBe "test-global"
        app.global(MyGlobal::class).value shouldBe "test-global"
        app.globalOrNull<MyGlobal>()?.value shouldBe "test-global"

        app.withGlobal<MyGlobal, String> {
            "result-${value}"
        } shouldBe "result-test-global"

        val defaultGlobal = app.globalOrDefault { MyGlobal("default") }
        defaultGlobal.value shouldBe "test-global"
    }
})
