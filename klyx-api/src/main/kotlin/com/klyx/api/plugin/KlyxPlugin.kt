package com.klyx.api.plugin

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

@JvmDefaultWithoutCompatibility
interface KlyxPlugin {

    suspend fun onLoad()

    suspend fun onStart()

    suspend fun onStop()

    suspend fun onUnload()
}

val KlyxPlugin.context: PluginContext by runtime()
val KlyxPlugin.lifecycleOwner: PluginLifecycleOwner by runtime()

suspend fun currentLifecycleOwner(): LifecycleOwner {
    return currentCoroutineContext()[PluginContextElement]?.owner
        ?: error("Not executing inside a plugin.")
}

suspend fun currentPluginContext(): PluginContext {
    return currentCoroutineContext()[PluginContextElement]?.context
        ?: error("Not executing inside a plugin.")
}

suspend fun currentPluginContextOrNull(): PluginContext? =
    currentCoroutineContext()[PluginContextElement]?.context

val CoroutineScope.pluginContext: PluginContext
    get() = coroutineContext[PluginContextElement]?.context
        ?: error("'pluginContext' is only available in plugin scope")

class PluginContextElement(
    val context: PluginContext,
    val owner: PluginLifecycleOwner
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<PluginContextElement>

    override val key = Key
}
