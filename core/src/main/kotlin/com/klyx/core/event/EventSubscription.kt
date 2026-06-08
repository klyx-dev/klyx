package com.klyx.core.event

/**
 * Represents an active subscription to an [EventBus].
 *
 * A subscription can be cancelled to stop receiving events. Implementations
 * must treat repeated calls to [cancel] as a no-op.
 *
 * Subscriptions can be combined using [plus] and managed as a single unit.
 */
interface EventSubscription : AutoCloseable {

    /** Returns `true` while this subscription is active. */
    val isActive: Boolean

    /**
     * Stops event delivery for this subscription.
     *
     * Calling this method more than once is safe.
     */
    fun cancel()

    /**
     * Cancels this subscription.
     *
     * Equivalent to calling [cancel].
     */
    override fun close(): Unit = cancel()

    /**
     * Combines this subscription with [other].
     *
     * The returned [CompositeEventSubscription] can be used to cancel both
     * subscriptions together.
     */
    operator fun plus(other: EventSubscription): CompositeEventSubscription =
        CompositeEventSubscription(listOf(this, other))
}

/**
 * Groups multiple subscriptions and manages them as a single subscription.
 *
 * Cancelling the composite cancels all contained subscriptions.
 */
class CompositeEventSubscription(
    subscriptions: List<EventSubscription>,
) : EventSubscription {

    private val subs = subscriptions.toList()

    /**
     * Returns `true` if at least one contained subscription is still active.
     */
    override val isActive: Boolean
        get() = subs.any { it.isActive }

    /**
     * Cancels all contained subscriptions.
     */
    override fun cancel() {
        subs.forEach(EventSubscription::cancel)
    }

    /**
     * Returns a new composite containing all current subscriptions and [other].
     */
    override operator fun plus(other: EventSubscription): CompositeEventSubscription =
        CompositeEventSubscription(subs + other)

    override fun toString(): String =
        "CompositeEventSubscription(size=${subs.size}, active=$isActive)"
}

/**
 * Combines all subscriptions in this collection into a single
 * [CompositeEventSubscription].
 */
fun Iterable<EventSubscription>.composite(): CompositeEventSubscription =
    CompositeEventSubscription(toList())

/**
 * Combines all subscriptions in this array into a single
 * [CompositeEventSubscription].
 */
fun Array<out EventSubscription>.composite(): CompositeEventSubscription =
    CompositeEventSubscription(toList())
