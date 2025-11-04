package com.klyx.core.event

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

inline fun <reified E : Any> EventBus.registerSubscriber(
    subscriber: Subscriber<E>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): Job {
    return subscribe<E>(dispatcher = dispatcher, onEvent = subscriber::onEvent)
}

inline fun <reified E : Any> Subscriber<E>.subscribe(dispatcher: CoroutineDispatcher = Dispatchers.Default): Job {
    return EventBus.INSTANCE.registerSubscriber(this, dispatcher)
}

inline fun <reified E : Any> LifecycleOwner.registerSubscriber(
    subscriber: Subscriber<E>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
): Job {
    return EventBus.INSTANCE.subscribe<E>(
        lifecycleOwner = this,
        minActiveState = minActiveState,
        onEvent = subscriber::onEvent
    )
}

context(lifecycleOwner: LifecycleOwner)
inline fun <reified E : Any> Subscriber<E>.subscribe(minActiveState: Lifecycle.State = Lifecycle.State.STARTED): Job {
    return lifecycleOwner.registerSubscriber(this, minActiveState)
}
