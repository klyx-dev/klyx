package com.klyx.core.action

import com.klyx.core.FakeOwner
import com.klyx.core.event.ActionEvent
import com.klyx.core.event.ActionFailed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.core.error.NoDefinitionFoundException
import org.koin.dsl.koinApplication
import org.koin.dsl.module

private object Ping : Action {
    override val id = ActionId("test.ping")
}

private object Boom : Action {
    override val id = ActionId("test.boom")
}

private object Cancels : Action {
    override val id = ActionId("test.cancels")
}

private data class WithParams(val n: Int) : Action {
    override val id = WithParams.id

    companion object : ActionDescriptor<WithParams> {
        override val id = ActionId("test.withParams")
    }
}

private fun setup(): Pair<ActionRegistry, ActionDispatcher> {
    val registry = ActionRegistry()
    return registry to ActionDispatcher(registry, koinApplication { }.koin)
}

class ActionRegistryDispatcherTest : FunSpec({

    test("dispatch invokes handler") {
        val [registry, dispatcher] = setup()
        var ran = false
        registry.register(Ping::class, ActionMetadata(Ping.id, "Ping")) { ran = true }
        dispatcher.dispatch(Ping)
        ran.shouldBeTrue()
    }

    test("duplicate registration throws") {
        val registry = setup().first
        registry.register(Ping::class, ActionMetadata(Ping.id, "Ping")) {}
        shouldThrow<DuplicateActionException> {
            registry.register(Ping::class, ActionMetadata(Ping.id, "Ping again")) {}
        }
    }

    test("unknown action throws") {
        val dispatcher = setup().second
        shouldThrow<UnknownActionException> { dispatcher.dispatch(Ping) }
    }

    test("registration time availability is honoured and can be flipped") {
        val [registry, dispatcher] = setup()
        registry.register(
            Ping::class,
            ActionMetadata(Ping.id, "Ping"),
            availability = ActionAvailability(enabled = false),
        ) {}
        shouldThrow<ActionDisabledException> { dispatcher.dispatch(Ping) }
        registry.setEnabled(Ping.id, true)
        dispatcher.dispatch(Ping)
    }

    test("each registration gets its own availability instance") {
        val [registry, dispatcher] = setup()
        ActionRegistrar(registry).action(instance = Ping) { }
        ActionRegistrar(registry).action(instance = Boom) { }
        registry.setEnabled(Ping.id, false)
        registry.isEnabled(Boom.id).shouldBeTrue()
        shouldThrow<ActionDisabledException> { dispatcher.dispatch(Ping) }
    }

    test("listing exposes enabled and visible") {
        val registry = setup().first
        registry.register(Ping::class, ActionMetadata(Ping.id, "Ping")) {}
        registry.setVisible(Ping.id, false)
        registry.list().single() shouldBe ActionListing(ActionMetadata(Ping.id, "Ping"), enabled = true, visible = false)
        registry.all.value.single().visible.shouldBeFalse()
    }

    test("exceptions propagate and emit failure event") {
        val [registry, dispatcher] = setup()
        registry.register(Boom::class, ActionMetadata(Boom.id, "Boom")) { error("kaboom") }
        val events = mutableListOf<ActionEvent>()
        val collector = launch { dispatcher.events.collect { events += it } }
        yield()
        shouldThrow<IllegalStateException> { dispatcher.dispatch(Boom) }
        yield()
        collector.cancel()
        events.any { it is ActionFailed }.shouldBeTrue()
    }

    test("cancellation propagates unwrapped") {
        val [registry, dispatcher] = setup()
        registry.register(Cancels::class, ActionMetadata(Cancels.id, "Cancels")) {
            throw CancellationException("cancel")
        }
        shouldThrow<CancellationException> { dispatcher.dispatch(Cancels) }
    }

    test("dispatchCatching never turns cancellation into a Result") {
        val [registry, dispatcher] = setup()
        registry.register(Cancels::class, ActionMetadata(Cancels.id, "Cancels")) {
            throw CancellationException("cancel")
        }
        shouldThrow<CancellationException> { dispatcher.dispatchCatching(Cancels) }
    }

    test("dispatchCatching reports ordinary failures") {
        val [registry, dispatcher] = setup()
        registry.register(Boom::class, ActionMetadata(Boom.id, "Boom")) { error("kaboom") }
        dispatcher.dispatchCatching(Boom).exceptionOrNull()!!.message shouldBe "kaboom"
    }

    test("a handler registered for one type rejects a foreign action") {
        val registry = ActionRegistry()
        ActionRegistrar(registry).action(instance = Ping) { }
        val dispatcher = ActionDispatcher(registry, koinApplication { }.koin)
        val foreign = object : Action {
            override val id = Ping.id
        }
        shouldThrow<ActionTypeMismatchException> { dispatcher.dispatch(foreign) }
    }

    test("dispatching by id routes through the registered factory") {
        val [registry, dispatcher] = setup()
        var seen = -1
        ActionRegistrar(registry).action(
            descriptor = WithParams,
            factory = { WithParams(7) },
        ) { seen = it.n }
        dispatcher.dispatch(WithParams.id)
        seen shouldBe 7
    }

    test("dispatching by id without a factory reports it") {
        val [registry, dispatcher] = setup()
        ActionRegistrar(registry).action<WithParams>(WithParams.id.value) { }
        shouldThrow<ActionNeedsInputException> { dispatcher.dispatch(WithParams.id) }
    }

    @Suppress("UNCHECKED_CAST")
    test("dispatching by id surfaces needed input instead of guessing") {
        val [registry, dispatcher] = setup()
        ActionRegistrar(registry).action(
            descriptor = WithParams,
            invocation = { singleInput("n") { n -> WithParams(n.trim().toInt()) } },
        ) { }
        shouldThrow<ActionNeedsInputException> { dispatcher.dispatch(WithParams.id) }
        val pending = dispatcher.dispatchOrNull(WithParams.id) as ActionInvocation.NeedsInput<WithParams>
        pending.build(mapOf("value" to " 12 ")).n shouldBe 12
    }

    test("a factory that produces a different action is rejected") {
        val [registry, dispatcher] = setup()
        registry.register(
            type = WithParams::class,
            metadata = ActionMetadata(WithParams.id, "With params"),
            factory = { ActionInvocation.Ready(object : Action { override val id = Boom.id }) },
        ) {}
        shouldThrow<ActionFactoryMismatchException> { dispatcher.dispatch(WithParams.id) }
    }

    test("unload of an owner removes its actions") {
        val [registry, dispatcher] = setup()
        val plugin = FakeOwner("myPlugin")
        registry.register(Ping::class, ActionMetadata(Ping.id, "Ping"), owner = plugin) {}
        registry.isRegistered(Ping.id).shouldBeTrue()
        registry.unregisterAll(plugin)
        registry.isRegistered(Ping.id).shouldBeFalse()
        shouldThrow<UnknownActionException> { dispatcher.dispatch(Ping) }
    }

    test("a stale disposable leaves a newer registration alone") {
        val registry = setup().first
        val first = registry.register(Ping::class, ActionMetadata(Ping.id, "Ping")) {}
        registry.unregister(Ping.id)
        registry.register(Ping::class, ActionMetadata(Ping.id, "Ping v2")) {}
        first.dispose()
        registry.isRegistered(Ping.id).shouldBeTrue()
    }

    test("disposable unregisters just its own action") {
        val registry = setup().first
        val first = registry.register(Ping::class, ActionMetadata(Ping.id, "Ping")) {}
        registry.register(Boom::class, ActionMetadata(Boom.id, "Boom")) {}
        first.dispose()
        registry.isRegistered(Ping.id).shouldBeFalse()
        registry.isRegistered(Boom.id).shouldBeTrue()
    }

    test("dispatch refuses actions whose owner went away") {
        val [registry, dispatcher] = setup()
        val plugin = FakeOwner("myPlugin", isActive = false)
        registry.register(Ping::class, ActionMetadata(Ping.id, "Ping"), owner = plugin) {}
        shouldThrow<ActionOwnerInactiveException> { dispatcher.dispatch(Ping) }
    }

    test("serialized actions never overlap") {
        val registry = ActionRegistry()
        val dispatcher = ActionDispatcher(registry, koinApplication { }.koin)
        var active = 0
        var maxActive = 0
        ActionRegistrar(registry).action(instance = Ping, serialize = true) {
            active++
            maxActive = maxOf(maxActive, active)
            yield()
            active--
        }
        coroutineScope {
            repeat(16) { launch { dispatcher.dispatch(Ping) } }
        }
        maxActive shouldBe 1
    }

    test("a hidden action is still dispatchable, it is only kept out of listings") {
        val [registry, dispatcher] = setup()
        var ran = 0
        ActionRegistrar(registry).action(instance = Ping) { ran++ }
        registry.setVisible(Ping.id, false)

        dispatcher.dispatch(Ping)
        ran shouldBe 1

        registry.list().map { it.metadata.id } shouldBe listOf(Ping.id)
        registry.listVisible().size shouldBe 0
        registry.isVisible(Ping.id).shouldBeFalse()
    }

    test("action ids reject blank input") {
        shouldThrow<IllegalArgumentException> { ActionId("  ") }
        shouldThrow<IllegalArgumentException> { ActionId(" x ") }
    }

    test("dependencies are reachable from a handler through koin") {
        val registry = ActionRegistry()
        var seen: Greeting? = null
        ActionRegistrar(registry).action(instance = Ping) { seen = service<Greeting>() }
        val koin = koinApplication { modules(module { single { Greeting() } }) }.koin
        ActionDispatcher(registry, koin).dispatch(Ping)
        (seen != null).shouldBeTrue()
    }

    test("a missing dependency surfaces koin's own error") {
        val registry = ActionRegistry()
        ActionRegistrar(registry).action(instance = Ping) { service<Greeting>() }
        shouldThrow<NoDefinitionFoundException> {
            ActionDispatcher(registry, koinApplication { }.koin).dispatch(Ping)
        }
    }
})

private class Greeting
