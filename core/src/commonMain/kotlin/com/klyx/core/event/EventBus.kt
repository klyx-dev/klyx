package com.klyx.core.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class EventBus private constructor() {
    companion object {
        val instance by lazy { EventBus() }
    }

    private val eventChannels = ConcurrentHashMap<KClass<*>, Channel<Any>>()
    private val eventFlows = ConcurrentHashMap<KClass<*>, SharedFlow<Any>>()

    @PublishedApi
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(minActiveState) {
                subscribe<T>().collect {
                    onEvent(it)
                }
            }
        }
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
        }
    }

    /**
     * Subscribe with error handling
     */
    inline fun <reified T : Any> subscribeWithErrorHandling(
        lifecycleOwner: LifecycleOwner,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline onEvent: suspend (T) -> Unit,
        crossinline onError: suspend (Throwable) -> Unit = {}
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(minActiveState) {
                subscribe<T>().catch { onError(it) }.collect { onEvent(it) }
            }
        }
    }

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
        eventChannels.values.forEach { it.close() }
        eventChannels.clear()
        eventFlows.clear()
    }
}

inline fun <reified T : Any> LifecycleOwner.subscribeToEvent(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline onEvent: suspend (T) -> Unit
) {
    EventBus.instance.subscribe<T>(this, minActiveState, onEvent)
}
