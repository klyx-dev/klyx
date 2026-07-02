package com.klyx.plugin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginContext
import com.klyx.api.plugin.PluginContextElement
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.plugin.PluginLifecycleOwner
import com.klyx.api.plugin.PluginRuntimeService
import com.klyx.api.plugin.PluginScope
import com.klyx.core.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

internal class PluginRuntime(
    val plugin: KlyxPlugin,
    val context: PluginContext,
    val owner: PluginLifecycleOwnerImpl,
    val scope: PluginScope,
    val info: PluginInfo
) {

    private val lifecycle = owner.lifecycle

    suspend fun load(progress: PluginManager.PluginLoadProgressListener? = null) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return

        runInPluginScope {
            lifecycle(Lifecycle.Event.ON_CREATE)

            tryOrDestroy {
                progress?.step("plugin.onLoad()")
                plugin.onLoad()
            }
        }
    }

    suspend fun start(progress: PluginManager.PluginLoadProgressListener? = null) {
        // onStart() is only valid once the plugin is loaded (CREATED) and not yet started.
        if (lifecycle.currentState != Lifecycle.State.CREATED) return

        runInPluginScope {
            tryOrDestroy {
                progress?.step("plugin.onStart()")
                lifecycle(Lifecycle.Event.ON_START)
                plugin.onStart()
            }
        }
    }

    suspend fun stop() = runInPluginScope {
        tryOrDestroy {
            lifecycle(Lifecycle.Event.ON_STOP)
            plugin.onStop()
        }
    }

    suspend fun unload() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return

        runInPluginScope {
            try {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    lifecycle(Lifecycle.Event.ON_STOP)
                    plugin.onStop()
                }

                plugin.onUnload()
            } finally {
                lifecycle(Lifecycle.Event.ON_DESTROY)
            }
        }
        scope.cancel()
    }

    private suspend inline fun tryOrDestroy(block: suspend () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            lifecycle(Lifecycle.Event.ON_DESTROY)
            throw t
        }
    }

    private suspend inline fun runInPluginScope(
        crossinline block: suspend () -> Unit
    ) {
        val deferred = scope.async {
            block()
        }

        try {
            deferred.await()
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            throw e
        }
    }

    private suspend fun lifecycle(event: Lifecycle.Event) {
        withContext(Dispatchers.Main.immediate) {
            lifecycle.handleLifecycleEvent(event)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : PluginRuntimeService> service(type: KClass<T>): T =
        when (type) {
            PluginContext::class -> context as T
            PluginScope::class -> scope as T
            PluginInfo::class -> info as T
            PluginLifecycleOwner::class -> owner as T
            else -> error("Unknown runtime service: ${type.qualifiedName}")
        }
}

internal fun PluginRuntime(app: App, plugin: KlyxPlugin, info: PluginInfo): PluginRuntime {
    val context = PluginContextImpl(app)
    val owner = PluginLifecycleOwnerImpl(context)
    val scope = PluginScopeImpl(
        SupervisorJob() +
                Dispatchers.Default +
                PluginContextElement(context, owner)
    )
    return PluginRuntime(plugin, context, owner, scope, info)
}

internal class PluginLifecycleOwnerImpl(
    private val context: PluginContext
) : PluginLifecycleOwner {

    override val lifecycle = LifecycleRegistry(this)

    override val lifecycleScope: CoroutineScope by lazy {
        (this as LifecycleOwner).lifecycleScope + PluginContextElement(context, this)
    }
}

internal class PluginContextImpl(override val app: App) : PluginContext

internal class PluginScopeImpl(override val coroutineContext: CoroutineContext) : PluginScope
