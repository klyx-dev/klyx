package com.klyx.core.event

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * A coroutine-based event bus for publishing and subscribing to typed events.
 *
 * Subscribers can listen for specific event types, optionally receive subtype
 * events, control delivery priority, and consume events as Flows.
 *
 * Features include:
 * - Priority-based delivery
 * - Optional subtype matching
 * - Sticky events
 * - Dead-letter handling
 * - Flow integration
 * - Interceptors
 * - Structured concurrency
 * - Per-subscription error handling
 *
 * ## Example
 * ```kotlin
 * val bus = eventBus(viewModelScope) {
 *     enableStickyEvents = true
 * }
 *
 * val subscription = bus.subscribe<UserEvent> {
 *     println(it)
 * }
 *
 * bus.publish(UserEvent.LoggedIn("Alice"))
 *
 * subscription.cancel()
 * ```
 *
 * ## Consuming events as a Flow
 * ```kotlin
 * bus.asFlow<AnalyticsEvent>()
 *     .filter { it.isImportant }
 *     .collect(::track)
 * ```
 *
 * ## Waiting for a single event
 * ```kotlin
 * val result = bus.awaitFirst<SearchEvent> {
 *     it.query == "kotlin"
 * }
 * ```
 */
interface EventBus : AutoCloseable {

    /** Returns `true` while the bus is accepting events and subscriptions. */
    val isActive: Boolean

    /**
     * Emits events that were published but had no matching subscribers.
     *
     * Only available when dead-letter support is enabled.
     */
    val deadLetters: Flow<Any>

    /**
     * Publishes an event to the bus.
     *
     * The event passes through all configured interceptors before being delivered
     * to matching subscribers.
     *
     * This call may suspend if the bus is configured to suspend when its buffer
     * is full.
     *
     * @throws IllegalStateException if the bus has been closed.
     */
    suspend fun <T : Any> publish(event: T)

    /**
     * Attempts to publish an event without suspending.
     *
     * @return `true` if the event was accepted, otherwise `false`.
     */
    @IgnorableReturnValue
    fun <T : Any> tryPublish(event: T): Boolean

    /**
     * Registers a subscriber for events of the given type.
     *
     * Subscriptions become active immediately after this function returns.
     *
     * @param type Event type to listen for.
     * @param priority Determines delivery order when multiple subscribers match.
     * Higher priority subscribers receive events first.
     * @param dispatcher Dispatcher used to execute the handler.
     * @param filter Optional predicate used to ignore matching events.
     * @param onError Optional callback invoked when the handler throws.
     * @param handler Invoked for every accepted event.
     *
     * @return A subscription that can be cancelled to stop receiving events.
     *
     * @throws IllegalStateException if the bus has been closed.
     */
    @IgnorableReturnValue
    fun <T : Any> subscribe(
        type: KClass<T>,
        priority: Priority = Priority.Normal,
        dispatcher: CoroutineDispatcher? = null,
        filter: ((T) -> Boolean)? = null,
        onError: (suspend (Throwable, T) -> Unit)? = null,
        handler: suspend (T) -> Unit,
    ): EventSubscription

    /**
     * Returns a Flow of events matching the given type.
     *
     * The returned Flow is cold and starts collecting when observed.
     */
    fun <T : Any> asFlow(type: KClass<T>): Flow<T>

    /**
     * Waits for the next event that matches the given type and filter.
     *
     * Returns the first matching event and then stops listening.
     */
    suspend fun <T : Any> awaitFirst(
        type: KClass<T>,
        filter: ((T) -> Boolean)? = null,
    ): T

    /** Returns the number of active subscribers for the given event type. */
    fun <T : Any> subscriberCount(type: KClass<T>): Int

    /** Removes the sticky event associated with the given type, if present. */
    fun clearStickyEvent(type: KClass<*>)

    /** Removes all stored sticky events. */
    fun clearAllStickyEvents()

    /**
     * Clears all subscriptions and sticky events.
     *
     * The bus remains active and can be used again immediately.
     */
    fun reset()

    /**
     * Permanently shuts down the bus.
     *
     * All subscriptions are cancelled and no further events can be published
     * or subscribed to.
     */
    override fun close()

    companion object
}

/**
 * Creates an [EventBus] whose lifecycle is tied to the provided [scope].
 *
 * The bus is closed automatically when the [scope] is cancelled.
 */
fun eventBus(
    scope: CoroutineScope,
    block: EventBusConfigBuilder.() -> Unit = {},
): EventBus = DefaultEventBus(scope, EventBusConfigBuilder().apply(block).build())

/**
 * Creates an [EventBus] bound to this [CoroutineScope].
 *
 * Equivalent to:
 * ```kotlin
 * eventBus(this) { ... }
 * ```
 */
@JvmName("_eventBus")
fun CoroutineScope.eventBus(
    block: EventBusConfigBuilder.() -> Unit = {},
): EventBus = eventBus(this, block)
