package com.klyx.core.event

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * Registers a subscriber for events of type [T].
 *
 * This is the reified equivalent of:
 *
 * ```kotlin
 * subscribe(T::class, ...)
 * ```
 */
@IgnorableReturnValue
inline fun <reified T : Any> EventBus.subscribe(
    priority: Priority = Priority.Normal,
    dispatcher: CoroutineDispatcher? = null,
    noinline filter: ((T) -> Boolean)? = null,
    noinline onError: (suspend (Throwable, T) -> Unit)? = null,
    noinline handler: suspend (T) -> Unit,
): EventSubscription = subscribe(T::class, priority, dispatcher, filter, onError, handler)

/** Returns a Flow that emits events of type [T]. */
inline fun <reified T : Any> EventBus.asFlow(): Flow<T> = asFlow(T::class)

/**
 * Waits for the next event of type [T] that matches [filter].
 *
 * Returns as soon as a matching event is received.
 */
suspend inline fun <reified T : Any> EventBus.awaitFirst(
    noinline filter: ((T) -> Boolean)? = null,
): T = awaitFirst(T::class, filter)

/** Returns the number of active subscribers for events of type [T]. */
inline fun <reified T : Any> EventBus.subscriberCount(): Int = subscriberCount(T::class)

/** Removes the sticky event associated with type [T], if present. */
inline fun <reified T : Any> EventBus.clearStickyEvent() = clearStickyEvent(T::class)

/**
 * Registers a subscriber that is automatically cancelled when [scope] completes.
 *
 * Useful when a subscription should follow the lifecycle of a ViewModel,
 * screen, service, or other scoped component.
 *
 * @param scope Scope that controls the lifetime of the subscription.
 */
@IgnorableReturnValue
inline fun <reified T : Any> EventBus.subscribeIn(
    scope: CoroutineScope,
    priority: Priority = Priority.Normal,
    dispatcher: CoroutineDispatcher? = null,
    noinline filter: ((T) -> Boolean)? = null,
    noinline onError: (suspend (Throwable, T) -> Unit)? = null,
    noinline handler: suspend (T) -> Unit,
): EventSubscription {
    val subscription = subscribe<T>(priority, dispatcher, filter, onError, handler)
    scope.coroutineContext[Job]?.invokeOnCompletion { subscription.cancel() }
    return subscription
}

/**
 * Registers a subscriber that receives at most one event.
 *
 * The subscription is cancelled immediately before the handler is invoked,
 * ensuring that only the first matching event is delivered.
 */
@IgnorableReturnValue
fun <T : Any> EventBus.subscribeOnce(
    type: KClass<T>,
    priority: Priority = Priority.Normal,
    dispatcher: CoroutineDispatcher? = null,
    filter: ((T) -> Boolean)? = null,
    handler: suspend (T) -> Unit,
): EventSubscription {
    // Holds the subscription reference so the handler can self-cancel.
    // We use AtomicReference because the handler (a non-inline lambda) cannot
    // capture a plain 'var' in Kotlin.
    val ref = AtomicReference<EventSubscription?>()

    val sub = subscribe(type, priority, dispatcher, filter) { event ->
        // Cancel before invoking handler so further events are dropped,
        // even if the handler suspends for a long time.
        ref.get()?.cancel()
        handler(event)
    }
    ref.set(sub)
    return sub
}

/**
 * Registers a subscriber that receives at most one event.
 *
 * The subscription is cancelled immediately before the handler is invoked,
 * ensuring that only the first matching event is delivered.
 */
@IgnorableReturnValue
inline fun <reified T : Any> EventBus.subscribeOnce(
    priority: Priority = Priority.Normal,
    dispatcher: CoroutineDispatcher? = null,
    noinline filter: ((T) -> Boolean)? = null,
    noinline handler: suspend (T) -> Unit,
): EventSubscription = subscribeOnce(T::class, priority, dispatcher, filter, handler)

/**
 * Collects events of type [T] as a Flow in the provided [scope].
 *
 * This is a convenience wrapper around:
 *
 * ```kotlin
 * asFlow<T>()
 *     .onEach(block)
 *     .launchIn(scope)
 * ```
 */
@IgnorableReturnValue
inline fun <reified T : Any> EventBus.collectIn(
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
    noinline block: suspend (T) -> Unit,
): Job = asFlow<T>().onEach(block).launchIn(scope + context)

/**
 * Publishes every value emitted by this Flow to [bus].
 *
 * The forwarding coroutine runs in [scope] and is cancelled with it.
 */
@IgnorableReturnValue
fun <T : Any> Flow<T>.publishTo(
    bus: EventBus,
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
): Job = onEach { event -> bus.publish(event) }.launchIn(scope + context)

/** Alias for [asFlow]. */
fun <T : Any> EventBus.flowOf(type: KClass<T>): Flow<T> = asFlow(type)

/** Alias for [asFlow]. */
inline fun <reified T : Any> EventBus.flowOf(): Flow<T> = flowOf(T::class)

/**
 * Attempts to publish [event] without suspending.
 *
 * Equivalent to:
 *
 * ```kotlin
 * bus.tryPublish(event)
 * ```
 */
operator fun EventBus.plusAssign(event: Any) {
    tryPublish(event)
}

/**
 * Publishes all provided events in order.
 */
suspend fun EventBus.publishAll(vararg events: Any) {
    for (event in events) publish(event)
}

/**
 * Publishes all provided events in order.
 */
suspend fun EventBus.publishAll(sequence: Iterable<Any>) {
    for (event in sequence) publish(event)
}

/**
 * Builder used by [on] to configure a subscription before registering it.
 *
 * A subscription is not created until [handle] is called.
 */
class SubscriptionBuilder<T : Any> @PublishedApi internal constructor(
    private val bus: EventBus,
    private val type: KClass<T>,
) {
    private var priority: Priority = Priority.Normal
    private var dispatcher: CoroutineDispatcher? = null
    private var filter: ((T) -> Boolean)? = null
    private var onError: (suspend (Throwable, T) -> Unit)? = null

    /** Sets the subscription priority. */
    @IgnorableReturnValue
    fun priority(p: Priority): SubscriptionBuilder<T> = apply { priority = p }

    /** Sets the dispatcher used to invoke the handler. */
    @IgnorableReturnValue
    fun dispatcher(d: CoroutineDispatcher): SubscriptionBuilder<T> = apply { dispatcher = d }

    /** Filters incoming events before delivery. */
    @IgnorableReturnValue
    fun filter(f: (T) -> Boolean): SubscriptionBuilder<T> = apply { filter = f }

    /** Handles exceptions thrown by the subscription handler. */
    @IgnorableReturnValue
    fun onError(f: suspend (Throwable, T) -> Unit): SubscriptionBuilder<T> = apply { onError = f }

    /**
     * Registers the subscription using the configured options.
     */
    fun handle(handler: suspend (T) -> Unit): EventSubscription =
        bus.subscribe(type, priority, dispatcher, filter, onError, handler)
}

/**
 * Creates a subscription using a fluent DSL.
 *
 * ```kotlin
 * bus.on<OrderEvent> {
 *     priority(Priority.High)
 *     filter { it.amount > 100 }
 *     handle(::processOrder)
 * }
 * ```
 */
@IgnorableReturnValue
inline fun <reified T : Any> EventBus.on(
    block: SubscriptionBuilder<T>.() -> EventSubscription,
): EventSubscription = SubscriptionBuilder(this, T::class).block()
