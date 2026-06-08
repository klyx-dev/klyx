package com.klyx.core.event

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Defines how events are delivered to matching subscribers.
 */
enum class DeliveryMode {

    /**
     * Delivers events to all matching subscribers concurrently.
     *
     * This mode maximizes throughput but does not guarantee completion order
     * between subscribers.
     *
     * If [EventBusConfig.waitForHandlers] is enabled, the bus waits for all
     * handlers to complete before processing the next event.
     */
    CONCURRENT,

    /**
     * Delivers events to subscribers one at a time in priority order.
     *
     * The next subscriber is not invoked until the previous handler completes.
     * This provides deterministic delivery ordering at the cost of throughput.
     */
    SEQUENTIAL,
}

/**
 * Intercepts events before they are delivered to subscribers.
 *
 * Interceptors form a pipeline and are invoked in the order they are registered.
 * Each interceptor must call `next` to continue event dispatch.
 */
fun interface EventInterceptor {

    /**
     * Processes [event] before it reaches subscribers.
     *
     * Implementations may observe, transform, replace, or suppress events.
     * Calling [next] continues dispatch through the remaining interceptors.
     */
    suspend fun intercept(event: Any, next: suspend (Any) -> Unit)
}

/**
 * Configuration for an [EventBus].
 *
 * Instances are typically created using [eventBusConfig].
 */
data class EventBusConfig(

    /**
     * Maximum number of events that may be queued before backpressure is applied.
     */
    val bufferCapacity: Int = 256,

    /**
     * Overflow strategy used when the internal event buffer is full.
     */
    val bufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,

    /**
     * Default dispatcher used for subscriber handlers.
     *
     * Individual subscriptions may override this value.
     */
    val defaultDispatcher: CoroutineDispatcher? = null,

    /**
     * Controls how events are delivered to matching subscribers.
     */
    val deliveryMode: DeliveryMode = DeliveryMode.CONCURRENT,

    /**
     * Whether the bus waits for all handlers of an event to complete before
     * processing the next event when using [DeliveryMode.CONCURRENT].
     */
    val waitForHandlers: Boolean = false,

    /**
     * Whether unmatched events are emitted to [EventBus.deadLetters].
     */
    val enableDeadLetters: Boolean = true,

    /**
     * Whether the most recent event of each type is cached and replayed to
     * new subscribers.
     */
    val enableStickyEvents: Boolean = false,

    /**
     * Whether subscribers receive events published as subtypes of the
     * subscribed type.
     */
    val enableSubtypeMatching: Boolean = true,

    /**
     * Interceptors applied to events before delivery.
     */
    val interceptors: List<EventInterceptor> = emptyList(),

    /**
     * Fallback error handler used when a subscriber throws and no
     * subscription-specific handler is provided.
     */
    val globalExceptionHandler: ((Throwable, Any) -> Unit)? = null,
)

/** Marks types that participate in the EventBus DSL. */
@DslMarker
annotation class EventBusDsl

/**
 * Creates an [EventBusConfig] using the configuration DSL.
 *
 * ```kotlin
 * val config = eventBusConfig {
 *     enableStickyEvents = true
 *     deliveryMode = DeliveryMode.SEQUENTIAL
 * }
 * ```
 */
fun eventBusConfig(
    block: EventBusConfigBuilder.() -> Unit = {},
): EventBusConfig = EventBusConfigBuilder().apply(block).build()

/**
 * Builder used by [eventBusConfig].
 */
@EventBusDsl
class EventBusConfigBuilder {

    /** Maximum number of queued events. */
    var bufferCapacity: Int = 256

    /** Overflow strategy used when the buffer is full. */
    var bufferOverflow: BufferOverflow = BufferOverflow.SUSPEND

    /** Default dispatcher used for subscriber handlers. */
    var defaultDispatcher: CoroutineDispatcher? = null

    /** Controls how events are delivered to subscribers. */
    var deliveryMode: DeliveryMode = DeliveryMode.CONCURRENT

    /** Whether event processing waits for all handlers to complete. */
    var waitForHandlers: Boolean = false

    /** Whether unmatched events are emitted to dead letters. */
    var enableDeadLetters: Boolean = true

    /** Whether events should be cached and replayed to new subscribers. */
    var enableStickyEvents: Boolean = false

    /** Whether subtype events should match parent-type subscriptions. */
    var enableSubtypeMatching: Boolean = true

    /** Fallback handler for uncaught subscriber exceptions. */
    var globalExceptionHandler: ((Throwable, Any) -> Unit)? = null

    /** Registered interceptors. */
    val interceptors: List<EventInterceptor>
        field: MutableList<EventInterceptor> = mutableListOf()

    /**
     * Adds [interceptor] to the end of the interceptor pipeline.
     */
    operator fun List<EventInterceptor>.plusAssign(
        interceptor: EventInterceptor,
    ) {
        interceptors.add(interceptor)
    }

    /**
     * Adds an interceptor to the pipeline.
     */
    fun addInterceptor(interceptor: EventInterceptor) {
        interceptors += interceptor
    }

    @PublishedApi
    internal fun build(): EventBusConfig = EventBusConfig(
        bufferCapacity = bufferCapacity,
        bufferOverflow = bufferOverflow,
        defaultDispatcher = defaultDispatcher,
        deliveryMode = deliveryMode,
        waitForHandlers = waitForHandlers,
        enableDeadLetters = enableDeadLetters,
        enableStickyEvents = enableStickyEvents,
        enableSubtypeMatching = enableSubtypeMatching,
        interceptors = interceptors.toList(),
        globalExceptionHandler = globalExceptionHandler,
    )
}
