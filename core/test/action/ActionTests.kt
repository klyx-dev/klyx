package com.klyx.core.action

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import com.klyx.core.App
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.engine.coroutines.testScheduler
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.time.Duration.Companion.seconds

@Serializable
data object Ping : Action

@Serializable
data class Greet(val name: String = "world", val times: Int = 1) : Action

@Serializable
data class NeedsArg(val x: Int) : Action

class ActionDispatcherTest : FunSpec({
    coroutineTestScope = true

    test("execute runs handler with params") {
        val d = ActionDispatcher(backgroundScope)
        val seen = mutableListOf<Greet>()
        d.register<Greet> { seen += it }
        d.execute(Greet("vivek", 2))

        seen shouldBe listOf(Greet("vivek", 2))
    }

    test("type dispatch uses defaults") {
        val d = ActionDispatcher(backgroundScope)
        val seen = mutableListOf<Greet>()
        d.register<Greet> { seen += it }
        d.execute<Greet>()
        seen shouldBe listOf(Greet())
    }

    test("type dispatch with required param fails") {
        val d = ActionDispatcher(backgroundScope)
        d.register<NeedsArg> { }
        shouldThrow<SerializationException> { d.execute<NeedsArg>() }
    }

    test("handler failure is wrapped and propagated") {
        val d = ActionDispatcher(backgroundScope)
        d.register<Ping> { throw IllegalStateException("boom") }

        val e = shouldThrow<ActionException> { d.execute<Ping>() }
        e.action shouldBe Ping
        e.cause shouldBeInstanceOf IllegalStateException::class
        e.cause?.message shouldBe "boom"
    }

    test("chained failure keeps cause chain") {
        val d = ActionDispatcher(backgroundScope)
        d.register<Ping> { dispatcher.execute(Greet()) }
        d.register<Greet> { error("inner") }

        val e = shouldThrow<ActionException> { d.execute(Ping) }
        e.action shouldBe Ping

        val inner = e.cause shouldBeInstanceOf ActionException::class
        inner.action shouldBe Greet()
        inner.cause?.message shouldBe "inner"
    }

    test("cancellation is not wrapped") {
        val d = ActionDispatcher(backgroundScope)
        d.register<Ping> { throw CancellationException("cancelled") }
        shouldThrow<CancellationException> { d.execute(Ping) }
    }

    test("unregistered action throws") {
        val d = ActionDispatcher(backgroundScope)
        val e = shouldThrow<ActionNotRegisteredException> { d.execute(Ping) }
        e.type shouldBe Ping::class
    }

    test("duplicate registration is rejected") {
        val d = ActionDispatcher(backgroundScope)
        d.register<Ping> { }
        shouldThrow<IllegalStateException> { d.register<Ping> { } }
    }

    test("unregister removes handler") {
        val d = ActionDispatcher(backgroundScope)
        val reg = d.register<Ping> { }

        reg.unregister()

        d.isRegistered(Ping::class).shouldBeFalse()
        shouldThrow<ActionNotRegisteredException> { d.execute(Ping) }
    }

    test("failed action does not cancel the dispatcher") {
        val d = ActionDispatcher(backgroundScope)
        var pinged = false
        d.register<Greet> { error("boom") }
        d.register<Ping> { pinged = true }

        shouldThrow<ActionException> { d.dispatchAsync(Greet()).await() }
        d.dispatchAsync(Ping).await()

        pinged shouldBe true
    }

    test("dispatchAsync surfaces error on await") {
        val d = ActionDispatcher(backgroundScope)
        d.register<Ping> { error("boom") }

        val deferred = d.dispatchAsync(Ping)
        shouldThrow<ActionException> { deferred.await() }
    }

    test("fire-and-forget failure reaches scope ExceptionHandler") {
        val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler) +
                    CoroutineExceptionHandler { _, e -> errors += e }
        )

        val d = ActionDispatcher(scope)
        d.register<Ping> { error("boom") }

        d.dispatch(Ping)
        testCoroutineScheduler.advanceUntilIdle()

        errors.single().shouldBeInstanceOf<ActionException>()
    }

    test("encode decode round trip") {
        val d = ActionDispatcher(CoroutineScope(Job()))
        d.register<Greet> { }
        d.register<Ping> { }

        val raw = d.encode(Greet("a", 3))

        d.decode(raw) shouldBe Greet("a", 3)
        d.decode(d.encode(Ping)) shouldBe Ping
    }

    test("decode unknown type fails") {
        val d = ActionDispatcher(CoroutineScope(Job()))
        d.register<Ping> { }
        shouldThrow<SerializationException> { d.decode("""{"type":"nope"}""") }
    }
})

@Serializable
data class Open(val path: String = "", val line: Int = 0) : Action

@Serializable
data class Query(val text: String = "") : Action

private suspend fun <T> Deferred<T>.awaitShort(): T = withTimeout(5.seconds) { await() }

class ActionsApiTest : FunSpec({
    coroutineTestScope = true

    lateinit var app: App

    beforeTest {
        @OptIn(ExperimentalCoroutinesApi::class)
        Dispatchers.setMain(StandardTestDispatcher())
        app = App.init(startKoin { })
    }

    afterTest {
        app.close()
        stopKoin()
        @OptIn(ExperimentalCoroutinesApi::class)
        Dispatchers.resetMain()
    }

    test("action<A> { } registers a handler and returns its Registration") {
        val got = CompletableDeferred<Open>()
        val reg = action<Open> { got.complete(it) }

        app.actions.isRegistered(Open::class).shouldBeTrue()
        Open("/a.kt", 3).dispatch()
        got.awaitShort() shouldBe Open("/a.kt", 3)

        reg.unregister()
        app.actions.isRegistered(Open::class).shouldBeFalse()
    }

    test("registering the same action twice throws") {
        action<Open> { }
        shouldThrow<IllegalStateException> { action<Open> { } }
    }

    test("action<A>() dispatches the default instance only when invoked") {
        val got = CompletableDeferred<Open>()
        action<Open> { got.complete(it) }

        val onClick = action<Open>()
        got.isCompleted.shouldBeFalse()

        onClick()
        got.awaitShort() shouldBe Open()
    }

    test("action(value) dispatches that value on every invoke") {
        val seen = Channel<Open>(Channel.UNLIMITED)
        action<Open> { seen.send(it) }

        val onClick = action(Open("/Main.kt", 42))
        onClick()
        onClick()

        repeat(2) {
            withTimeout(5.seconds) { seen.receive() } shouldBe Open("/Main.kt", 42)
        }
    }

    test("actionWith builds the action from the callback value") {
        val got = CompletableDeferred<Query>()
        action<Query> { got.complete(it) }

        val onValueChange = actionWith { text: String -> Query(text) }
        onValueChange("klyx")

        got.awaitShort() shouldBe Query("klyx")
    }

    test("action<A>() with a required parameter fails on creation, not on invoke") {
        shouldThrow<SerializationException> { action<NeedsArg>() }
    }

    test("Action.execute propagates handler failure as ActionException") {
        action<Ping> { error("boom") }

        val e = shouldThrow<ActionException> { Ping.execute() }

        e.action shouldBe Ping
        e.cause?.message shouldBe "boom"
    }

    test("Action.execute without a handler throws ActionNotRegisteredException") {
        val e = shouldThrow<ActionNotRegisteredException> { Ping.execute() }
        e.type shouldBe Ping::class
    }

    test("handlers can chain actions and the cause chain is kept") {
        action<Ping> { dispatcher.execute(NeedsArg(1)) }
        action<NeedsArg> { error("inner") }

        val e = shouldThrow<ActionException> { Ping.execute() }

        e.action shouldBe Ping
        val inner = e.cause.shouldBeInstanceOf<ActionException>()
        inner.action shouldBe NeedsArg(1)
        inner.cause?.message shouldBe "inner"
    }

    test("a failed action does not break later ones") {
        action<Ping> { error("boom") }
        val got = CompletableDeferred<Query>()
        action<Query> { got.complete(it) }

        shouldThrow<ActionException> { app.actions.dispatchAsync(Ping).awaitShort() }
        app.actions.dispatchAsync(Query("ok")).awaitShort()

        got.awaitShort() shouldBe Query("ok")
    }

    test("invoking without an App fails with a clear error") {
        stopKoin()

        val e = shouldThrow<IllegalStateException> { action(Ping)() }
        e.message shouldBe "initKoin first"
    }

    fun TestScope.settle() {
        Snapshot.sendApplyNotifications()
        testCoroutineScheduler.advanceUntilIdle()
    }

    test("OnAction registers while composed and unregisters when it leaves") {
        var show by mutableStateOf(true)
        val job = moleculeFlow(RecompositionMode.Immediate) {
            if (show) OnAction<Ping> { }
        }.launchIn(backgroundScope)
        settle()
        eventually(5.seconds) { app.actions.isRegistered(Ping::class).shouldBeTrue() }

        show = false
        settle()
        eventually(5.seconds) { app.actions.isRegistered(Ping::class).shouldBeFalse() }

        job.cancel()
    }
})
