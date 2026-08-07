@file:OptIn(InternalKlyxApi::class)

package com.klyx.plugin

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.klyx.api.InternalKlyxApi
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.createDirIfMissing
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.data.runner.FileRunnerRegistry
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginContext
import com.klyx.api.plugin.PluginContextElement
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.plugin.PluginLifecycleOwner
import com.klyx.api.plugin.PluginRuntimeService
import com.klyx.api.plugin.PluginScope
import com.klyx.api.plugin.PluginSettings
import com.klyx.api.plugin.PluginSettingsRegistry
import com.klyx.api.ui.ScreenRegistry
import com.klyx.core.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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

enum class PluginState { LOADED, STARTED, CRASHED, DISABLED }

internal class PluginRuntime(
    val plugin: KlyxPlugin,
    val context: PluginContext,
    val owner: PluginLifecycleOwnerImpl,
    val scope: PluginScope,
    val info: PluginInfo,
    val settings: PluginSettings
) {
    @Volatile
    var state: PluginState = PluginState.LOADED
        private set

    private val lifecycle = owner.lifecycle

    suspend fun load(progress: PluginManager.PluginLoadProgressListener? = null) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return

        runInPluginScope {
            lifecycle(Lifecycle.Event.ON_CREATE)

            tryOrDestroy {
                progress?.step("Calling onLoad()")
                plugin.onLoad()
            }
        }
    }

    suspend fun start(progress: PluginManager.PluginLoadProgressListener? = null) {
        // onStart() is only valid once the plugin is loaded (CREATED) and not yet started.
        if (lifecycle.currentState != Lifecycle.State.CREATED) return

        runInPluginScope {
            tryOrDestroy {
                progress?.step("Calling onStart()")
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

    fun crash(t: Throwable) {
        if (state == PluginState.CRASHED) return
        state = PluginState.CRASHED
        Log.e("PluginRuntime", "Plugin '${info.id}' crashed", t)

        scope.cancel(CancellationException("Plugin '${info.id}' crashed", t))
        Handler(Looper.getMainLooper()).post {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        context.app.global<ScreenRegistry>().unregisterAll(info.id)
        context.app.global<PluginSettingsRegistry>().unregisterAll(info.id)
        context.app.global<FileRunnerRegistry>().unregisterAll(info.id)
    }

    private suspend inline fun tryOrDestroy(block: suspend () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            crash(t)
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
            PluginSettings::class -> settings as T
            else -> error("Unknown runtime service: ${type.qualifiedName}")
        }
}

internal fun PluginRuntime(app: App, plugin: KlyxPlugin, info: PluginInfo): PluginRuntime {
    val context = PluginContextImpl(app, info.id, info.apkPath)
    val owner = PluginLifecycleOwnerImpl(context)
    val runtimeRef = object {
        var value: PluginRuntime? = null
    }
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PluginRuntime", "Plugin '${info.id}' threw an unhandled exception", throwable)
        runtimeRef.value?.crash(throwable)
    }
    val scope = PluginScopeImpl(
        SupervisorJob() +
                Dispatchers.Default +
                PluginContextElement(context, owner) +
                exceptionHandler
    )
    val settings = PluginSettingsImpl(app, info.id, scope)
    return PluginRuntime(plugin, context, owner, scope, info, settings).also { runtimeRef.value = it }
}

internal class PluginLifecycleOwnerImpl(
    private val context: PluginContext
) : PluginLifecycleOwner {

    override val lifecycle = LifecycleRegistry(this)

    override val lifecycleScope: CoroutineScope by lazy {
        (this as LifecycleOwner).lifecycleScope + PluginContextElement(context, this)
    }
}

internal class PluginContextImpl(
    override val app: App,
    override val pluginId: String,
    private val apkPath: String
) : PluginContext {
    override val dataDir by lazy {
        Paths.pluginsDir.resolve(pluginId).also { it.createDirIfMissing() }
    }

    private val resourceManager by lazy { PluginResourceManager(app, apkPath) }

    override val resources: Resources
        get() = resourceManager.resources

    override val context: Context
        get() = resourceManager.context
}

internal class PluginScopeImpl(override val coroutineContext: CoroutineContext) : PluginScope
