package com.klyx.api.event

import com.klyx.api.InternalKlyxApi
import com.klyx.api.event.editor.FileOpenedEvent
import com.klyx.api.plugin.PluginContext
import com.klyx.core.Global
import com.klyx.core.event.EventBus

/**
 * A [Global] wrapper that exposes the application's [EventBus] to plugins.
 *
 * Registered by the host during startup; plugins reach the bus through [PluginContext.eventBus].
 */
@InternalKlyxApi
class EventBusHolder(val bus: EventBus) : Global

/**
 * The application-wide [EventBus].
 *
 * Plugins can publish their own events and subscribe to built-in ones (such as
 * [FileOpenedEvent]) through this bus.
 *
 * ### Example
 * ```kotlin
 * suspend fun watchOpens() {
 *     currentPluginContext().eventBus.subscribe<FileOpenedEvent> { event ->
 *         println("Opened ${event.fileName}")
 *     }
 * }
 * ```
 */
@OptIn(InternalKlyxApi::class)
val PluginContext.eventBus: EventBus
    get() = app.global<EventBusHolder>().bus
