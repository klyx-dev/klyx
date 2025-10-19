package com.klyx.editor.compose.event

import com.klyx.core.atomic.atomicMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class EditorEventManager internal constructor(
    @PublishedApi
    internal val scope: CoroutineScope
) {
    private val eventChannels = atomicMapOf<KClass<out EditorEvent>, Channel<EditorEvent>>()
    private val eventFlows = atomicMapOf<KClass<out EditorEvent>, SharedFlow<EditorEvent>>()

    suspend fun <E : EditorEvent> post(event: E) {
        val clazz = event::class
        getOrCreateChannel(clazz).send(event)
    }

    fun <E : EditorEvent> postSync(event: E) {
        scope.launch { post(event) }
    }

    inline fun <reified E : EditorEvent> subscribe(): Flow<E> {
        return getOrCreateFlow(E::class).filterIsInstance()
    }

    inline fun <reified E : EditorEvent> subscribe(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline onEvent: suspend (E) -> Unit
    ): Job {
        return scope.launch(dispatcher) {
            subscribe<E>().collect { onEvent(it) }
        }
    }

    fun unsubscribe(job: Job) {
        job.cancel()
    }

    fun unsubscribeAll() {
        eventChannels.values.forEach { it.cancel(CancellationException("Editor event manager unsubscribed")) }
    }

    private fun getOrCreateChannel(eventClass: KClass<out EditorEvent>): Channel<EditorEvent> {
        return eventChannels.getOrPut(eventClass) {
            Channel<EditorEvent>(Channel.UNLIMITED).also { channel ->
                val flow = channel.receiveAsFlow()
                    .shareIn(
                        scope = scope,
                        started = SharingStarted.Lazily,
                        replay = 0
                    )
                eventFlows[eventClass] = flow
            }
        }
    }

    @PublishedApi
    internal fun getOrCreateFlow(eventClass: KClass<out EditorEvent>): SharedFlow<EditorEvent> {
        return eventFlows.getOrPut(eventClass) {
            getOrCreateChannel(eventClass)
            eventFlows[eventClass]!!
        }
    }
}

//@Composable
//fun rememberEditorEventManager(): EditorEventManager {
//    val scope = rememberCoroutineScope()
//    return remember { EditorEventManager(scope) }
//}
