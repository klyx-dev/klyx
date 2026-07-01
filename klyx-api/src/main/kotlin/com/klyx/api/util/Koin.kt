@file:MustUseReturnValues

package com.klyx.api.util

import android.app.Application
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Returns the default Koin context (GlobalContext).
 */
@Suppress("NOTHING_TO_INLINE")
inline fun defaultKoinContext() = GlobalContext

/**
 * Access the global Koin instance.
 */
inline val koin: Koin get() = defaultKoinContext().get()

/**
 * Retrieves the [Application] context from the global Koin instance.
 */
fun applicationContext(): Application = defaultKoinContext().get().get()

/**
 * Executes a [block] of code with the [Application] context as the receiver.
 */
@IgnorableReturnValue
inline fun <R> withApplicationContext(block: Application.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(GlobalContext.get().get())
}
