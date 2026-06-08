package com.klyx.event

import com.klyx.core.App
import com.klyx.core.Global
import com.klyx.core.event.EventBus
import com.klyx.core.event.eventBus
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

private class EventBusGlobal(val bus: EventBus) : Global

@OptIn(UnsafeGlobalAccess::class)
val GlobalEventBus by lazy { GlobalApp.eventBus() }

/**
 * Retrieves the global [EventBus] instance from the [App] registry.
 *
 * @return The registered [EventBus].
 */
fun App.eventBus() = global<EventBusGlobal>().bus

/**
 * Initializes and registers the global [EventBus] into the provided [App] instance.
 * This implementation uses [GlobalScope] for the bus's coroutine lifecycle.
 *
 * @param app The application instance where the global bus will be registered.
 */
@OptIn(DelicateCoroutinesApi::class)
fun initializeGlobalEventBus(app: App) {
    app.setGlobal(EventBusGlobal(eventBus(GlobalScope)))
}
