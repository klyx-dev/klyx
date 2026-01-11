package com.klyx.core.app

import kotlin.reflect.KType

/**
 * A marker interface for types that can be stored in Koin's global state.
 *
 * This interface exists to provide type-safe access to globals by ensuring only
 * types that implement [Global] can be used with the accessor methods. For
 * example, trying to access a global with a type that does not implement
 * [Global] will result in a compile-time error.
 *
 * Implement this on types you want to store in the context as a global.
 *
 * ## Restricting Access to Globals
 *
 * In some situations you may need to store some global state, but want to
 * restrict access to reading it or writing to it.
 *
 * In these cases, Kotlin's visibility system can be used to restrict access to
 * a global value. For example, you can create a private class that implements
 * [Global] and holds the global state. Then create a newtype class that wraps
 * the global type and create custom accessor methods to expose the desired subset
 * of operations.
 */
interface Global {
    // This interface is intentionally left empty, by virtue of being a marker interface.
    //
    // Use additional interfaces with blanket implementations to attach functionality
    // to types that implement `Global`.
}

/**
 * Returns the global instance of the implementing type.
 *
 * @throws NoGlobalException if a global for that type has not been assigned.
 */
inline fun <reified T : Global> global(cx: App): T = cx.global()

/**
 * Sets this instance as the global instance for its type [G].
 *
 * This extension function allows an instance of a [Global] type to register itself
 * into the [App] context provided by [cx]. Once set, this instance can be retrieved
 * via [global].
 *
 * @receiver The instance of [Global] to store.
 * @param cx The context provider containing the [App] instance where the global will be stored.
 * @see global
 */
inline fun <reified G : Global> G.setGlobal(cx: HasApp) = cx.app.setGlobal(this)

/**
 * @property message The detail message explaining which global type was missing.
 * @property type The specific Kotlin type ([KType]) of the global that could not be found.
 * @property cause The underlying cause of the exception, if available.
 */
class NoGlobalException(
    message: String,
    val type: KType,
    override val cause: Throwable? = null
) : Exception(message)

