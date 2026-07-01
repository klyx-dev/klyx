@file:MustUseReturnValues

package com.klyx.api.util

import android.app.Application
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("NOTHING_TO_INLINE")
inline fun defaultKoinContext() = GlobalContext

inline val koin: Koin get() = defaultKoinContext().get()

fun applicationContext(): Application = defaultKoinContext().get().get()

@IgnorableReturnValue
inline fun <R> withApplicationContext(block: Application.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(GlobalContext.get().get())
}
