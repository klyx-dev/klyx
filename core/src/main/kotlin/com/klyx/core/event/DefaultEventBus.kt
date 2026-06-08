package com.klyx.core.event

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * Internal EventBus implementation.
 *
 * Event publication is backed by a SharedFlow and delivered according to the
 * configured [EventBusConfig]. Subscriptions are stored by type and matched
 * at dispatch time, optionally supporting subtype delivery.
 */
internal class DefaultEventBus(
    parentScope: CoroutineScope,
    private val config: EventBusConfig,
) : EventBus {

    /**
     * Scope used internally by the bus.
     *
     * The parent's dispatcher and context elements are retained, but its [Job] is
     * intentionally excluded. This prevents the dispatch loop from becoming a
     * child of the parent scope while still allowing work to run on the same
     * dispatcher.
     *
     * Bus shutdown is handled explicitly through [close].
     */
    private val busScope: CoroutineScope = CoroutineScope(
        parentScope.coroutineContext.minusKey(Job) +
                SupervisorJob() +
                CoroutineName("EventBus"),
    )

    private val _events = MutableSharedFlow<Any>(
        replay = 0,
        extraBufferCapacity = config.bufferCapacity,
        onBufferOverflow = config.bufferOverflow,
    )

    private val _deadLetters = MutableSharedFlow<Any>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val deadLetters: Flow<Any> = _deadLetters.asSharedFlow()

    /** Sticky events keyed by their published type. */
    private val stickyCache = ConcurrentHashMap<KClass<*>, Any>()

    private val registryLock = Any()
    private val registry = HashMap<KClass<*>, MutableList<SubscriberRecord<*>>>()

    private val closed = AtomicBoolean(false)

    override val isActive: Boolean
        get() = !closed.get() && busScope.isActive

    init {
        busScope.launch(
            context = CoroutineName("EventBus.dispatch"),
            start = CoroutineStart.UNDISPATCHED
        ) {
            _events.collect { event ->
                runInterceptorChain(event)
            }
        }
    }

    /**
     * Passes [event] through the configured interceptor chain before dispatch.
     */
    private suspend fun runInterceptorChain(event: Any) {
        if (config.interceptors.isEmpty()) {
            dispatchToSubscribers(event)
            return
        }

        // Build the chain tail -> head so the first interceptor is the outermost call.
        var chain: suspend (Any) -> Unit = ::dispatchToSubscribers

        for (interceptor in config.interceptors.asReversed()) {
            val next = chain
            chain = { e -> interceptor.intercept(e, next) }
        }

        chain(event)
    }

    /**
     * Finds matching subscribers and delivers [event] according to the configured
     * [DeliveryMode].
     */
    private suspend fun dispatchToSubscribers(event: Any) {
        val eventJavaClass = event::class.java

        val snapshot: List<SubscriberRecord<*>> = synchronized(registryLock) {
            buildList {
                for ((subType, records) in registry) {
                    val matches = when {
                        config.enableSubtypeMatching -> subType.java.isAssignableFrom(eventJavaClass)
                        else -> subType == event::class
                    }
                    if (matches) addAll(records)
                }
            }
        }.sortedByDescending { it.priority }

        @Suppress("UNCHECKED_CAST")
        val active = snapshot.filter { record ->
            record.isActive && (record as SubscriberRecord<Any>).filter?.invoke(event) != false
        }

        if (active.isEmpty()) {
            if (config.enableDeadLetters) _deadLetters.tryEmit(event)
            return
        }

        when (config.deliveryMode) {
            DeliveryMode.CONCURRENT -> dispatchConcurrent(event, active)
            DeliveryMode.SEQUENTIAL -> dispatchSequential(event, active)
        }
    }

    /**
     * Delivers [event] to all matching subscribers concurrently.
     *
     * When [EventBusConfig.waitForHandlers] is enabled, this function waits for
     * all handlers to complete before returning.
     */
    private suspend fun dispatchConcurrent(
        event: Any,
        records: List<SubscriberRecord<*>>,
    ) {
        if (config.waitForHandlers) {
            // Wait for all handlers before processing the next event (backpressure mode).
            supervisorScope {
                records.forEach { record ->
                    launch(record.dispatcher ?: config.defaultDispatcher ?: EmptyCoroutineContext) {
                        invokeHandler(record, event)
                    }
                }
            }
        } else {
            // Fire-and-forget: launch each handler as an independent child of busScope.
            records.forEach { record ->
                busScope.launch(
                    record.dispatcher ?: config.defaultDispatcher ?: EmptyCoroutineContext
                ) {
                    invokeHandler(record, event)
                }
            }
        }
    }

    /**
     * Delivers [event] to matching subscribers sequentially in priority order.
     */
    private suspend fun dispatchSequential(
        event: Any,
        records: List<SubscriberRecord<*>>,
    ) {
        records.forEach { record ->
            withContext(record.dispatcher ?: config.defaultDispatcher ?: EmptyCoroutineContext) {
                invokeHandler(record, event)
            }
        }
    }

    /**
     * Invokes a subscriber handler and routes failures to the appropriate
     * error handler.
     *
     * Cancellation exceptions are always rethrown.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun invokeHandler(
        record: SubscriberRecord<*>,
        event: Any,
    ) {
        if (!record.isActive) return
        record as SubscriberRecord<Any>
        try {
            record.handler(event)
        } catch (e: CancellationException) {
            throw e // never swallow cancellation
        } catch (e: Throwable) {
            when {
                record.onError != null -> record.onError(e, event)

                config.globalExceptionHandler != null -> {
                    config.globalExceptionHandler(e, event)
                }

                else -> throw e
            }
        }
    }

    override suspend fun <T : Any> publish(event: T) {
        checkActive()
        cacheIfSticky(event)
        _events.emit(event)
    }

    override fun <T : Any> tryPublish(event: T): Boolean {
        if (!isActive) return false
        cacheIfSticky(event)
        return _events.tryEmit(event)
    }

    /**
     * Stores [event] for future replay when sticky events are enabled.
     */
    private fun <T : Any> cacheIfSticky(event: T) {
        if (config.enableStickyEvents) stickyCache[event::class] = event
    }

    override fun <T : Any> subscribe(
        type: KClass<T>,
        priority: Priority,
        dispatcher: CoroutineDispatcher?,
        filter: ((T) -> Boolean)?,
        onError: (suspend (Throwable, T) -> Unit)?,
        handler: suspend (T) -> Unit,
    ): EventSubscription {
        checkActive()

        val record = SubscriberRecord(
            type = type,
            priority = priority,
            dispatcher = dispatcher,
            filter = filter,
            onError = onError,
            handler = handler,
        )

        synchronized(registryLock) {
            registry
                .getOrPut(type) { mutableListOf() }
                .apply {
                    add(record)
                    sortByDescending { it.priority }
                }
        }

        // Sticky replay is async because it must call the suspending handler.
        if (config.enableStickyEvents) {
            busScope.launch {
                @Suppress("UNCHECKED_CAST")
                val sticky = stickyCache[type] as? T ?: return@launch
                if (filter?.invoke(sticky) != false) {
                    invokeHandler(record, sticky)
                }
            }
        }

        return object : EventSubscription {
            override val isActive: Boolean get() = record.isActive

            override fun cancel() {
                if (!record.deactivate()) return // already cancelled
                synchronized(registryLock) {
                    registry[type]?.also { list ->
                        list.remove(record)
                        if (list.isEmpty()) registry.remove(type)
                    }
                }
            }
        }
    }

    override fun <T : Any> asFlow(type: KClass<T>): Flow<T> {
        val javaType = type.java
        @Suppress("UNCHECKED_CAST")
        return _events
            .filter { event ->
                if (config.enableSubtypeMatching) javaType.isAssignableFrom(event::class.java)
                else event::class == type
            }
            .map { it as T }
    }

    override suspend fun <T : Any> awaitFirst(
        type: KClass<T>,
        filter: ((T) -> Boolean)?,
    ): T = asFlow(type).first { event -> filter?.invoke(event) != false }

    override fun <T : Any> subscriberCount(type: KClass<T>): Int =
        synchronized(registryLock) { registry[type]?.size ?: 0 }

    override fun clearStickyEvent(type: KClass<*>) {
        stickyCache.remove(type)
    }

    override fun clearAllStickyEvents() {
        stickyCache.clear()
    }

    override fun reset() {
        synchronized(registryLock) {
            registry.values.forEach { list -> list.forEach { it.deactivate() } }
            registry.clear()
        }
        stickyCache.clear()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            busScope.cancel()
            synchronized(registryLock) { registry.clear() }
            stickyCache.clear()
        }
    }

    private fun checkActive() {
        check(isActive) { "EventBus is closed and cannot accept new operations." }
    }

    override fun toString(): String =
        "DefaultEventBus(active=$isActive, config=$config)"
}

/**
 * Internal representation of a subscription.
 *
 * Stores subscription metadata, lifecycle state, and the handler used to
 * process matching events.
 */
private class SubscriberRecord<T : Any>(
    val type: KClass<T>,
    val priority: Priority,
    val dispatcher: CoroutineDispatcher?,
    val filter: ((T) -> Boolean)?,
    val onError: (suspend (Throwable, T) -> Unit)?,
    val handler: suspend (T) -> Unit,
) {
    private val _active = AtomicBoolean(true)

    /** Returns `true` while this subscription is active. */
    val isActive: Boolean get() = _active.get()

    /**
     * Marks this subscription as inactive.
     *
     * @return `true` if the subscription was active and is now deactivated.
     */
    @IgnorableReturnValue
    fun deactivate(): Boolean = _active.compareAndSet(true, false)

    override fun toString(): String =
        "SubscriberRecord(type=${type.simpleName}, priority=$priority, active=$isActive)"
}

