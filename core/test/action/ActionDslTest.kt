package com.klyx.core.action

import com.klyx.core.App
import com.klyx.core.FakeOwner
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

private object Format : Action {
    override val id = ActionId("test.format")
}

private data class Goto(val line: Int) : Action {
    override val id = Goto.id

    companion object : ActionDescriptor<Goto> {
        override val id = ActionId("test.goto")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActionDslTest : FunSpec({

    beforeTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        App.init(startKoin { })
    }

    afterTest {
        App.global.close()
        stopKoin()
        Dispatchers.resetMain()
    }

    test("form 1: action(instance) invokes") {
        var ran = 0
        action(Format) { ran++ }
        action(Format).join()
        ran shouldBe 1
    }

    test("form 2: action(instance) { } defines and auto records a factory") {
        var ran = 0
        action(Format) { ran++ }
        action(Format.id).join()
        ran shouldBe 1
    }

    test("form 3: action(descriptor) { factory } { handle } defines a parameterised action") {
        var seen = -1
        action(Goto, factory = { Goto(99) }) { seen = it.line }
        action(Goto.id).join()
        seen shouldBe 99
    }

    test("form 4: action(build) builds then invokes") {
        var seen = -1
        action(Goto, factory = { Goto(4) }) { seen = it.line }
        action { Goto(4) }.join()
        seen shouldBe 4
    }

    test("a descriptor without a factory cannot be reached by id") {
        action(Goto) { }
        shouldThrow<ActionNeedsInputException> { invoke(Goto.id) }
    }

    test("title defaults to a humanised form of the id") {
        action(Goto) { }
        App.global.actionRegistry.metadataOf(Goto.id)!!.title shouldBe "Goto"
    }

    test("declared chords become bindings once the app syncs them") {
        action(Format, defaultKeybinding = "Alt+Shift+D") { }
        App.global.keybindings.resolve("Alt+Shift+D").shouldBeNull()
        App.global.bindDefaultKeybindings()
        App.global.keybindings.resolve("Alt+Shift+D") shouldBe Format.id
    }

    test("groups hand back a disposable that reverses every registration") {
        val owner = FakeOwner("group")
        val group = ActionGroup { this.action(Format) { } }
        val disposable = with(group) { ActionRegistrar(App.global.actionRegistry, owner).register() }
        App.global.actionRegistry.isRegistered(Format.id) shouldBe true
        disposable.dispose()
        App.global.actionRegistry.isRegistered(Format.id) shouldBe false
    }
})
