@file:MustUseReturnValues

package com.klyx.util

import android.content.Context
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("NOTHING_TO_INLINE")
inline fun defaultKoinContext() = GlobalContext

inline val koin: Koin get() = defaultKoinContext().get()

fun applicationContext(): Context = defaultKoinContext().get().get()

@IgnorableReturnValue
inline fun <R> withApplicationContext(block: context(Context) () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(GlobalContext.get().get())
}

context(context: Context)
inline val context get() = context
