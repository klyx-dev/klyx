package com.klyx.core.app

import com.klyx.core.event.EventBus
import com.klyx.core.event.SettingsChangeEvent
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Singleton object for managing a thread-safe collection of breadcrumbs.
 * Breadcrumbs can log significant or recent events.
 *
 * Must never throw, even during OOM or fatal crashes.
 */
object Breadcrumbs : SynchronizedObject() {
    private const val MAX = 20
    private val breadcrumbs = ArrayDeque<String>(MAX)

    init {
        EventBus.INSTANCE.subscribeAll { event ->
            when (event) {
                is SettingsChangeEvent -> add("SettingsChanged")
                else -> add(event)
            }
        }
    }

    fun add(event: Any) = add(runCatching { event.toString() }.getOrElse { "<breadcrumb-error>" })

    fun add(event: String) {
        if (event.isBlank()) return
        synchronized(this) {
            if (breadcrumbs.size == MAX) breadcrumbs.removeFirst()
            breadcrumbs.addLast(event)
        }
    }

    fun snapshot() = synchronized(this) { breadcrumbs.toList() }

    override fun toString() = snapshot().joinToString("\n")
}

@Suppress("unused")
private val initBreadcrumbs_ = Breadcrumbs

/**
 * Adds a trace message to the breadcrumb trail for debugging or diagnostic purposes.
 *
 * @param message The message to be recorded in the breadcrumb trail. Blank messages are ignored.
 */
fun trace(message: String) = Breadcrumbs.add(message)
