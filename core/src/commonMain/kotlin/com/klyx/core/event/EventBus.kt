package com.klyx.core.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.klyx.core.atomic.atomicMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class EventBus private constructor() {
    companion object {
        val INSTANCE by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { EventBus() }
    }

    private val eventChannels = atomicMapOf<KClass<*>, Channel<Any>>()
    private val eventFlows = atomicMapOf<KClass<*>, SharedFlow<Any>>()

    @PublishedApi
    internal val subscribers = hashMapOf<KClass<*>, Job>()

    @PublishedApi
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @PublishedApi
    internal inline infix fun <reified T> addSubscriber(job: Job) {
        subscribers += T::class to job
    }

    /**
     * Post an event to all subscribers
     */
    suspend fun post(event: Any) {
        val eventClass = event::class
        getOrCreateChannel(eventClass).send(event)
    }

    /**
     * Post an event synchronously (non-blocking)
     */
    fun postSync(event: Any) {
        scope.launch { post(event) }
    }

    /**
     * Subscribe to events of a specific type
     * Returns a [Flow] that emits events of the specified type
     */
    inline fun <reified T : Any> subscribe(): Flow<T> {
        return getOrCreateFlow(T::class).filterIsInstance<T>()
    }

    /**
     * Subscribe to events with lifecycle awareness
     * Automatically manages subscription lifecycle based on the provided [LifecycleOwner]
     */
    inline fun <reified T : Any> subscribe(
        lifecycleOwner: LifecycleOwner,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline onEvent: suspend (T) -> Unit
    ): Job {
        return lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(minActiveState) {
                subscribe<T>().collect {
                    onEvent(it)
                }
            }
        }.also { addSubscriber<T>(it) }
    }

    /**
     * Subscribe to events with a specific coroutine dispatcher
     * Returns a [Job] that can be used to cancel the subscription
     */
    inline fun <reified T : Any> subscribe(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline onEvent: suspend (T) -> Unit
    ): Job {
        return scope.launch(dispatcher) {
            subscribe<T>().collect { onEvent(it) }
        }.also { addSubscriber<T>(it) }
    }

    /**
     * Subscribe with error handling
     */
    inline fun <reified T : Any> subscribeWithErrorHandling(
        lifecycleOwner: LifecycleOwner,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline onEvent: suspend (T) -> Unit,
        crossinline onError: suspend (Throwable) -> Unit = {}
    ): Job {
        return lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(minActiveState) {
                subscribe<T>().catch { onError(it) }.collect { onEvent(it) }
            }
        }.also { addSubscriber<T>(it) }
    }

    inline fun <reified T : Any> unsubscribe(job: Job) {
        unsubscribeFlow(T::class)
        val cause = CancellationException("This event subscription was cancelled.")
        unsubscribeChannel(T::class)?.also { channel -> channel.cancel(cause) }
        job.cancel(cause)
        subscribers.remove(T::class)
    }

    inline fun <reified T : Any> unsubscribe() {
        subscribers[T::class]
            ?.let { job -> unsubscribe<T>(job) }
            ?.also { subscribers.remove(T::class) }
    }

    @PublishedApi
    internal fun unsubscribeChannel(eventClass: KClass<*>) = eventChannels.remove(eventClass)

    @PublishedApi
    internal fun unsubscribeFlow(eventClass: KClass<*>) = eventFlows.remove(eventClass)

    private fun getOrCreateChannel(eventClass: KClass<*>): Channel<Any> {
        return eventChannels.getOrPut(eventClass) {
            Channel<Any>(Channel.UNLIMITED).also { channel ->
                val sharedFlow = channel.receiveAsFlow()
                    .shareIn(
                        scope = scope,
                        started = SharingStarted.Lazily,
                        replay = 0
                    )
                eventFlows[eventClass] = sharedFlow
            }
        }
    }

    @PublishedApi
    internal fun getOrCreateFlow(eventClass: KClass<*>): SharedFlow<Any> {
        return eventFlows.getOrPut(eventClass) {
            getOrCreateChannel(eventClass)
            eventFlows[eventClass]!!
        }
    }

    fun clear() {
        subscribers.values.forEach { job -> job.cancel(CancellationException("EventBus was cleared.")) }
        eventChannels.values.forEach { it.cancel(CancellationException("EventBus was cleared.")) }
        eventChannels.clear()
        eventFlows.clear()
    }

    fun unsubscribeAll() = clear()
}

inline fun <reified T : Any> LifecycleOwner.subscribeToEvent(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline onEvent: suspend (event: T) -> Unit
): Job {
    return EventBus.INSTANCE.subscribe<T>(this, minActiveState, onEvent)
}
