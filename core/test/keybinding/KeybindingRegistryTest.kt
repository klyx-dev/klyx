package com.klyx.core.keybinding

import com.klyx.core.FakeOwner
import com.klyx.core.action.Action
import com.klyx.core.action.ActionDispatcher
import com.klyx.core.action.ActionId
import com.klyx.core.action.ActionInvocation
import com.klyx.core.action.ActionMetadata
import com.klyx.core.action.ActionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.koin.dsl.koinApplication

private object Save : Action {
    override val id = ActionId("test.save")
}

class KeybindingRegistryTest : FunSpec({
    test("resolves chords ignoring order and case") {
        val kb = KeybindingRegistry()
        kb.bind("Ctrl+Shift+Z", ActionId("workspace.openProject"))

        kb.resolve("shift+ctrl+z") shouldBe ActionId("workspace.openProject")
        kb.resolve("Shift+Ctrl+Z") shouldBe ActionId("workspace.openProject")
    }

    test("rebinding replaces previous mapping") {
        val kb = KeybindingRegistry()
        kb.bind("Ctrl+P", ActionId("a"))
        kb.bind("Ctrl+P", ActionId("b"))
        kb.resolve("Ctrl+P") shouldBe ActionId("b")
    }

    test("handle dispatches through the registered factory") {
        val registry = ActionRegistry()
        val dispatcher = ActionDispatcher(registry, koinApplication { }.koin)
        val kb = KeybindingRegistry()
        var ran = false
        registry.register(
            type = Save::class,
            metadata = ActionMetadata(Save.id, "Save"),
            factory = { ActionInvocation.Ready(Save) },
        ) {
            ran = true
        }
        kb.bind("Ctrl+S", Save.id)

        kb.handle("Ctrl+S", dispatcher)
        ran.shouldBeTrue()
        kb.handle("Ctrl+Q", dispatcher).shouldBeFalse()
    }

    test("bindDefault only binds when metadata declares a chord") {
        val kb = KeybindingRegistry()
        kb.bindDefault(ActionMetadata(Save.id, "Save", defaultKeybinding = "Ctrl+S"))
        kb.resolve("Ctrl+S") shouldBe Save.id
        kb.bindDefault(ActionMetadata(ActionId("test.bare"), "Bare")).shouldBeNull()
    }

    test("unbind removes one chord") {
        val kb = KeybindingRegistry()
        kb.bind("Ctrl+P", ActionId("a"))
        kb.unbind("Ctrl+P") shouldBe ActionId("a")
        kb.resolve("Ctrl+P").shouldBeNull()
    }

    test("the plus key survives normalisation") {
        val kb = KeybindingRegistry()
        kb.bind("Ctrl++", ActionId("a"))
        kb.resolve("Ctrl++") shouldBe ActionId("a")
        kb.resolve("ctrl++") shouldBe ActionId("a")
    }

    test("unbind all removes only that owners bindings") {
        val kb = KeybindingRegistry()
        val plugin = FakeOwner("p1")
        kb.bind("Ctrl+1", ActionId("a"), owner = plugin)
        kb.bind("Ctrl+2", ActionId("b"))
        kb.unbindAll(plugin)

        kb.resolve("Ctrl+1").shouldBeNull()
        kb.resolve("Ctrl+2") shouldBe ActionId("b")
    }
})
