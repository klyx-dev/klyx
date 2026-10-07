package com.klyx.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline

/** A [CoroutineScope] that refuses to run on the main dispatcher, so work here can never block the UI. */
@JvmInline
value class BackgroundScope(override val coroutineContext: CoroutineContext) : CoroutineScope {
    init {
        val dispatcher = coroutineContext[ContinuationInterceptor]
        require(
            dispatcher != null &&
                    (dispatcher !== Dispatchers.Main) &&
                    (dispatcher !== Dispatchers.Main.immediate)
        ) {
            "BackgroundScope must use a background dispatcher."
        }
    }

    companion object
}

/** A [BackgroundScope] backed by a [SupervisorJob] and [Dispatchers.Default]. */
fun BackgroundScope() = BackgroundScope(SupervisorJob() + Dispatchers.Default)

/** Platform default IO dispatcher. [Dispatchers.Default] on web. */
expect val IoDispatcher: CoroutineDispatcher
