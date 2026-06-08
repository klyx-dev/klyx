package com.klyx.core.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

sealed interface AppEvent
data class UserEvent(val name: String) : AppEvent
data class SystemEvent(val code: Int) : AppEvent
data class OrderEvent(val id: String, val amount: Double) : AppEvent
data object ShutdownEvent : AppEvent

private fun TestScope.advanceUntilIdle() = testCoroutineScheduler.advanceUntilIdle()

class EventBusTest : FunSpec({

    coroutineTestScope = true

    test("subscriber receives published event") {
        val bus = eventBus(this)
        val received = mutableListOf<UserEvent>()

        bus.subscribe<UserEvent> { received += it }
        bus.publish(UserEvent("Alice"))
        advanceUntilIdle()

        received shouldBe listOf(UserEvent("Alice"))
        bus.close()
    }

    test("subscriber does not receive unrelated event types") {
        val bus = eventBus(this)
        val received = mutableListOf<UserEvent>()

        bus.subscribe<UserEvent> { received += it }
        bus.publish(SystemEvent(42))
        advanceUntilIdle()

        received.shouldBeEmpty()
    }

    test("multiple subscribers all receive same event") {
        val bus = eventBus()

        val counter = AtomicInteger(0)
        repeat(5) { bus.subscribe<UserEvent> { counter.incrementAndGet() } }
        bus.publish(UserEvent("Bob"))
        advanceUntilIdle()

        counter.get() shouldBe 5
        bus.close()
    }

    test("cancelled subscription does not receive further events") {
        val bus = eventBus(this)
        val received = mutableListOf<UserEvent>()

        val sub = bus.subscribe<UserEvent> { received += it }
        bus.publish(UserEvent("first"))
        advanceUntilIdle()

        sub.cancel()
        bus.publish(UserEvent("second"))
        advanceUntilIdle()

        received shouldBe listOf(UserEvent("first"))
        sub.isActive.shouldBeFalse()
        bus.close()
    }

    test("higher-priority subscriber receives event first in SEQUENTIAL mode") {
        val bus = eventBus { deliveryMode = DeliveryMode.SEQUENTIAL }
        val order = mutableListOf<String>()

        bus.subscribe<UserEvent>(priority = Priority.Low) { order += "low" }
        bus.subscribe<UserEvent>(priority = Priority.High) { order += "high" }
        bus.subscribe<UserEvent>(priority = Priority.Normal) { order += "normal" }
        bus.subscribe<UserEvent>(priority = Priority.Highest) { order += "highest" }

        bus.publish(UserEvent("test"))
        advanceUntilIdle()

        order shouldBe listOf("highest", "high", "normal", "low")
        bus.close()
    }

    test("Priority arithmetic works correctly") {
        val base = Priority.Normal

        (base + 5) shouldBe Priority(55)
        (base - 5) shouldBe Priority(45)
        (Priority.High > Priority.Low).shouldBeTrue()
        (Priority.Lowest < Priority.Highest).shouldBeTrue()
    }

    test("subscribing to sealed base receives all subtypes") {
        val bus = eventBus(this)
        val received = mutableListOf<AppEvent>()

        bus.subscribe<AppEvent> { received += it }
        bus.publish(UserEvent("Alice"))
        bus.publish(SystemEvent(1))
        bus.publish(ShutdownEvent)
        advanceUntilIdle()

        received.size shouldBe 3
        received[0].shouldBeInstanceOf<UserEvent>()
        received[1].shouldBeInstanceOf<SystemEvent>()
        received[2].shouldBeInstanceOf<ShutdownEvent>()

        bus.close()
    }

    test("subtype matching disabled delivers only exact types") {
        eventBus { enableSubtypeMatching = false }.use { bus ->
            val appEvents = mutableListOf<AppEvent>()
            val userEvents = mutableListOf<UserEvent>()

            bus.subscribe<AppEvent> { appEvents += it }
            bus.subscribe<UserEvent> { userEvents += it }

            bus.publish(UserEvent("Alice"))
            advanceUntilIdle()

            // AppEvent subscriber should NOT receive UserEvent when exact-only
            withClue("Base subscriber should not receive subtype") {
                appEvents.shouldBeEmpty()
            }

            userEvents.size shouldBe 1
        }
    }

    test("filter restricts events delivered to subscriber") {
        eventBus().use { bus ->
            val bigOrders = mutableListOf<OrderEvent>()

            bus.subscribe<OrderEvent>(filter = { it.amount > 100.0 }) {
                bigOrders += it
            }

            bus.publish(OrderEvent("ord-1", 50.0))
            bus.publish(OrderEvent("ord-2", 200.0))
            bus.publish(OrderEvent("ord-3", 10.0))
            bus.publish(OrderEvent("ord-4", 150.0))
            advanceUntilIdle()

            bigOrders.map { it.id } shouldBe listOf("ord-2", "ord-4")
        }
    }

    test("events with no subscriber surface in deadLetters") {
        eventBus { enableDeadLetters = true }.use { bus ->
            val dead = mutableListOf<Any>()

            val deadJob = launch { bus.deadLetters.take(1).collect { dead += it } }

            bus.publish(SystemEvent(99))
            advanceUntilIdle()
            deadJob.cancelAndJoin()

            dead shouldBe listOf(SystemEvent(99))
        }
    }

    test("events with subscriber do not go to deadLetters") {
        eventBus { enableDeadLetters = true }.use { bus ->
            val dead = mutableListOf<Any>()

            val deadJob = launch { bus.deadLetters.collect { dead += it } }
            bus.subscribe<UserEvent> { /* consume */ }

            bus.publish(UserEvent("handled"))
            advanceUntilIdle()
            deadJob.cancelAndJoin()

            dead.shouldBeEmpty()
        }
    }

    test("sticky event is replayed to late subscriber") {
        eventBus { enableStickyEvents = true }.use { bus ->
            bus.publish(SystemEvent(404))
            advanceUntilIdle()

            val received = mutableListOf<SystemEvent>()
            bus.subscribe<SystemEvent> { received += it }
            advanceUntilIdle()

            received shouldBe listOf(SystemEvent(404))
        }
    }

    test("clearStickyEvent prevents replay") {
        eventBus { enableStickyEvents = true }.use { bus ->
            bus.publish(SystemEvent(200))
            advanceUntilIdle()
            bus.clearStickyEvent<SystemEvent>()

            val received = mutableListOf<SystemEvent>()
            bus.subscribe<SystemEvent> { received += it }
            advanceUntilIdle()

            received.shouldBeEmpty()
        }
    }

    test("only last sticky event is replayed") {
        eventBus { enableStickyEvents = true }.use { bus ->
            bus.publish(SystemEvent(1))
            bus.publish(SystemEvent(2))
            bus.publish(SystemEvent(3))
            advanceUntilIdle()

            val received = mutableListOf<SystemEvent>()
            bus.subscribe<SystemEvent> { received += it }
            advanceUntilIdle()

            received shouldHaveSingleElement SystemEvent(3)
        }
    }

    test("awaitFirst suspends until matching event arrives") {
        eventBus().use { bus ->
            val deferred = async {
                bus.awaitFirst<UserEvent> { it.name == "target" }
            }

            yield()

            bus.publish(UserEvent("noise"))
            bus.publish(UserEvent("target"))
            advanceUntilIdle()

            deferred.await() shouldBe UserEvent("target")
        }
    }

    test("asFlow emits events") {
        eventBus().use { bus ->
            val collected = mutableListOf<UserEvent>()

            val job = launch {
                bus.asFlow<UserEvent>().take(3).collect { collected += it }
            }

            yield()

            repeat(3) { i -> bus.publish(UserEvent("user-$i")) }
            advanceUntilIdle()
            job.join()

            collected.size shouldBe 3
        }
    }

    test("CompositeEventSubscription cancels all children") {
        eventBus().use { bus ->
            val aList = mutableListOf<UserEvent>()
            val bList = mutableListOf<SystemEvent>()

            val composite = bus.subscribe<UserEvent> { aList += it } +
                    bus.subscribe<SystemEvent> { bList += it }

            bus.publish(UserEvent("x"))
            bus.publish(SystemEvent(1))
            advanceUntilIdle()

            composite.cancel()
            composite.isActive.shouldBeFalse()

            bus.publish(UserEvent("y"))
            bus.publish(SystemEvent(2))
            advanceUntilIdle()

            aList.size shouldBe 1
            bList.size shouldBe 1
        }
    }

    test("subscribeOnce fires exactly once") {
        eventBus().use { bus ->
            val counter = AtomicInteger(0)

            bus.subscribeOnce<UserEvent> { counter.incrementAndGet() }

            repeat(5) { bus.publish(UserEvent("evt-$it")) }
            advanceUntilIdle()

            counter.get() shouldBe 1
        }
    }

    test("subscribeIn cancels when scope is cancelled") {
        eventBus().use { bus ->
            val received = mutableListOf<UserEvent>()
            val childScope = CoroutineScope(coroutineContext + Job())

            bus.subscribeIn<UserEvent>(childScope) { received += it }

            bus.publish(UserEvent("before"))
            advanceUntilIdle()
            received.size shouldBe 1

            childScope.cancel()
            bus.publish(UserEvent("after"))
            advanceUntilIdle()

            withClue("Should not receive after scope cancel") {
                received.size shouldBe 1
            }
        }
    }

    test("interceptors are called in order before subscribers") {
        val log = CopyOnWriteArrayList<String>()

        eventBus {
            addInterceptor { event, next ->
                log += "interceptor-1:before"
                next(event)
                log += "interceptor-1:after"
            }
            addInterceptor { event, next ->
                log += "interceptor-2:before"
                next(event)
                log += "interceptor-2:after"
            }
            deliveryMode = DeliveryMode.SEQUENTIAL
            waitForHandlers = true
        }.use { bus ->
            bus.subscribe<UserEvent> { log += "handler" }
            bus.publish(UserEvent("test"))
            advanceUntilIdle()

            log.toList() shouldBe listOf(
                "interceptor-1:before",
                "interceptor-2:before",
                "handler",
                "interceptor-2:after",
                "interceptor-1:after",
            )
        }
    }

    test("interceptor can transform events") {
        eventBus {
            addInterceptor { event, next ->
                if (event is UserEvent) next(event.copy(name = event.name.uppercase()))
                else next(event)
            }
        }.use { bus ->
            val received = mutableListOf<UserEvent>()
            bus.subscribe<UserEvent> { received += it }
            bus.publish(UserEvent("alice"))
            advanceUntilIdle()

            received.first().name shouldBe "ALICE"
        }
    }

    test("interceptor can drop events by not calling next") {
        eventBus {
            addInterceptor { event, next ->
                if (event is SystemEvent && event.code < 0) return@addInterceptor // drop
                next(event)
            }
        }.use { bus ->
            val received = mutableListOf<SystemEvent>()
            bus.subscribe<SystemEvent> { received += it }

            bus.publish(SystemEvent(1))
            bus.publish(SystemEvent(-1))
            bus.publish(SystemEvent(2))
            advanceUntilIdle()

            received.map { it.code } shouldBe listOf(1, 2)
        }
    }

    test("per-subscriber onError is called on handler exception") {
        eventBus().use { bus ->
            val errors = mutableListOf<Throwable>()

            bus.subscribe<UserEvent>(
                onError = { err, _ -> errors += err },
            ) { throw RuntimeException("boom") }

            bus.publish(UserEvent("test"))
            advanceUntilIdle()

            errors.size shouldBe 1
            errors.first().message shouldBe "boom"
        }
    }

    test("global exception handler is called when no onError is set") {
        val errors = mutableListOf<Pair<Throwable, Any>>()

        eventBus {
            globalExceptionHandler = { err, event -> errors += Pair(err, event) }
        }.use { bus ->
            bus.subscribe<UserEvent> { throw IllegalStateException("fail") }
            bus.publish(UserEvent("test"))
            advanceUntilIdle()

            errors.size shouldBe 1
            errors.first().first.message shouldBe "fail"
        }
    }

    test("subscriber exception does not affect other subscribers") {
        eventBus {
            globalExceptionHandler = { _, _ -> /* suppress */ }
        }.use { bus ->
            val received = mutableListOf<UserEvent>()

            bus.subscribe<UserEvent> { throw RuntimeException("bad subscriber") }
            bus.subscribe<UserEvent> { received += it }

            bus.publish(UserEvent("test"))
            advanceUntilIdle()

            withClue("Healthy subscriber must still receive event") {
                received.size shouldBe 1
            }
        }
    }

    test("SEQUENTIAL mode delivers exactly once per subscriber") {
        eventBus {
            deliveryMode = DeliveryMode.SEQUENTIAL
        }.use { bus ->
            val count = AtomicInteger(0)

            repeat(10) { bus.subscribe<UserEvent> { count.incrementAndGet() } }
            bus.publish(UserEvent("test"))
            advanceUntilIdle()

            count.get() shouldBe 10
        }
    }

    test("tryPublish returns false on closed bus") {
        val bus = eventBus()
        bus.close()
        bus.tryPublish(UserEvent("x")).shouldBeFalse()
    }

    test("reset removes all subscribers") {
        eventBus().use { bus ->
            val received = mutableListOf<UserEvent>()

            bus.subscribe<UserEvent> { received += it }
            bus.publish(UserEvent("before reset"))
            advanceUntilIdle()

            bus.reset()

            bus.publish(UserEvent("after reset"))
            advanceUntilIdle()

            withClue("Should only have received the pre-reset event") {
                received.size shouldBe 1
            }
        }
    }

    test("publishAll delivers all events in order") {
        eventBus {
            deliveryMode = DeliveryMode.SEQUENTIAL
            waitForHandlers = true
        }.use { bus ->
            val received = mutableListOf<SystemEvent>()

            bus.subscribe<SystemEvent> { received += it }
            bus.publishAll(SystemEvent(1), SystemEvent(2), SystemEvent(3))
            advanceUntilIdle()

            received.map { it.code } shouldBe listOf(1, 2, 3)
        }
    }

    test("subscriberCount reflects active subscriptions") {
        eventBus().use { bus ->
            bus.subscriberCount<UserEvent>() shouldBe 0

            val sub1 = bus.subscribe<UserEvent> {}
            val sub2 = bus.subscribe<UserEvent> {}
            bus.subscriberCount<UserEvent>() shouldBe 2

            sub1.cancel()
            bus.subscriberCount<UserEvent>() shouldBe 1

            sub2.cancel()
            bus.subscriberCount<UserEvent>() shouldBe 0
        }
    }

    test("plusAssign operator publishes via tryPublish") {
        eventBus().use { bus ->
            val received = mutableListOf<SystemEvent>()

            bus.subscribe<SystemEvent> { received += it }
            bus += SystemEvent(42)
            advanceUntilIdle()

            received shouldBe listOf(SystemEvent(42))
        }
    }

    test("publishTo forwards Flow emissions to bus") {
        eventBus().use { bus ->
            val received = mutableListOf<UserEvent>()

            bus.subscribe<UserEvent> { received += it }

            val source = flow {
                emit(UserEvent("a"))
                emit(UserEvent("b"))
            }
            source.publishTo(bus, this)
            advanceUntilIdle()

            received.map { it.name } shouldBe listOf("a", "b")
        }
    }

    test("on-DSL builder registers subscription correctly") {
        eventBus().use { bus ->
            val received = mutableListOf<OrderEvent>()

            val sub = bus.on<OrderEvent> {
                priority(Priority.High)
                filter { it.amount >= 50.0 }
                handle { received += it }
            }

            bus.publish(OrderEvent("ord-1", 10.0))
            bus.publish(OrderEvent("ord-2", 100.0))
            advanceUntilIdle()

            received.map { it.id } shouldBe listOf("ord-2")
            sub.isActive.shouldBeTrue()
        }
    }

    test("publish on closed bus throws IllegalStateException") {
        val bus = eventBus()
        bus.close()

        shouldThrow<IllegalStateException> {
            bus.publish(UserEvent("x"))
        }
    }

    test("subscribe on closed bus throws IllegalStateException") {
        val bus = eventBus()
        bus.close()

        shouldThrow<IllegalStateException> {
            bus.subscribe<UserEvent> {}
        }
    }
})
