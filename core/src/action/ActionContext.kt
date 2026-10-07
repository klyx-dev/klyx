package com.klyx.core.action

import org.koin.core.Koin
import kotlin.reflect.KClass

/**
 * What a handler is given: the action being run, and read access to the app's dependency graph.
 *
 * [service] resolves through Koin, so a handler asks for the type it needs rather than for a key it has to
 * agree on with whoever registered it. A missing dependency throws as Koin's own
 * [org.koin.core.error.NoDefinitionFoundException].
 */
interface ActionContext {
    val action: Action

    fun <T : Any> get(clazz: KClass<T>): T

    fun <T : Any> getOrNull(clazz: KClass<T>): T?
}

/** Resolves [T] from the app's dependency graph. A missing one throws [org.koin.core.error.NoDefinitionFoundException]. */
inline fun <reified T : Any> ActionContext.service(): T = get(T::class)

/** Like [service], but null when nothing of that type is registered. */
inline fun <reified T : Any> ActionContext.serviceOrNull(): T? = getOrNull(T::class)

class DefaultActionContext(
    override val action: Action,
    private val koin: Koin,
) : ActionContext {
    override fun <T : Any> get(clazz: KClass<T>): T = koin.get(clazz)

    override fun <T : Any> getOrNull(clazz: KClass<T>): T? = koin.getOrNull(clazz)
}
