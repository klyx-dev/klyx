package com.klyx.core.init

import com.klyx.core.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InitializerManager(
    private val initializers: List<Initializer>,
    private val scope: CoroutineScope
) {
    private val locks = initializers.associateWith { Mutex() }
    private val initialized = mutableSetOf<Initializer>()

    val ready: StateFlow<Boolean>
        field = MutableStateFlow(false)

    suspend fun start() {
        val critical = initializers.filter { it.priority == InitPriority.CRITICAL }

        coroutineScope {
            critical.map { initializer ->
                async {
                    initializeOnce(initializer)
                }
            }.awaitAll()
        }

        ready.value = true

        initializers
            .filter { it.priority == InitPriority.EAGER }
            .forEach { initializer ->
                scope.launch {
                    try {
                        initializeOnce(initializer)
                    } catch (e: Exception) {
                        if (e is CancellationException) ensureActive()
                        Log.e("Initializer", "Failed to initialize ${initializer::class.simpleName}", e)
                    }
                }
            }
    }

    private suspend fun initializeOnce(initializer: Initializer) {
        locks.getValue(initializer).withLock {
            if (initializer in initialized) return

            initializer.initialize()
            initialized.add(initializer)
        }
    }
}
