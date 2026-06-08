package com.klyx.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.ContinuationInterceptor

/**
 * A [CoroutineScope] designated for background operations.
 */
@JvmInline
value class BackgroundScope private constructor(
    private val scope: CoroutineScope
) : CoroutineScope by scope {

    companion object {
        fun io() = BackgroundScope(CoroutineScope(SupervisorJob() + Dispatchers.IO))

        fun default() = BackgroundScope(CoroutineScope(SupervisorJob() + Dispatchers.Default))

        fun from(scope: CoroutineScope) = BackgroundScope(scope)
    }

    init {
        val dispatcher = scope.coroutineContext[ContinuationInterceptor]
        require(
            dispatcher != null &&
                    dispatcher != Dispatchers.Main &&
                    dispatcher != Dispatchers.Main.immediate
        ) {
            "BackgroundScope must use a background dispatcher"
        }
    }
}
