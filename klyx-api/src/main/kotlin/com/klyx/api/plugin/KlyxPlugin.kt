package com.klyx.api.plugin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

interface KlyxPlugin : LifecycleOwner {

    val id: String
    val version: String
    val minHostVersion: String

    /**
     * The [PluginContext] provided by the framework. Set before [onLoad] is called and cleared
     * after [onUnload]. Plugin authors must not assign to this property.
     *
     * Use [currentPluginContext] to access context without storing it in a field.
     */
    var context: PluginContext

    fun onLoad(context: PluginContext)

    fun onUnload()

    fun onStart() {}

    fun onDestroy() {}
}

/**
 * Returns the [PluginContext] for the currently executing plugin code, or throws
 * [IllegalStateException] if [KlyxPlugin.onLoad] has not been called yet.
 */
fun currentPluginContext(): PluginContext =
    PluginContextHolder.get()
        ?: throw IllegalStateException(
            "PluginContext not available. onLoad() has not been called yet."
        )

fun currentPluginContextOrNull(): PluginContext? = PluginContextHolder.get()

object PluginContextHolder {
    private val threadLocal = ThreadLocal<PluginContext>()

    fun set(context: PluginContext) {
        threadLocal.set(context)
    }

    fun clear() {
        threadLocal.remove()
    }

    fun get(): PluginContext? = threadLocal.get()
}

/**
 * Convenience base class that wires up lifecycle and context automatically.
 */
abstract class BaseKlyxPlugin(
    override val id: String,
    override val version: String,
    override val minHostVersion: String
) : KlyxPlugin {

    final override val lifecycle: Lifecycle = LifecycleRegistry(this)

    @Volatile
    private var _context: PluginContext? = null

    final override var context: PluginContext
        get() = _context
            ?: throw IllegalStateException(
                "PluginContext not available. onLoad() has not been called yet."
            )
        set(value) {
            _context = value
        }

    final override fun onLoad(context: PluginContext) {
        PluginContextHolder.set(context)
        _context = context
        onPluginLoad(context)
    }

    final override fun onUnload() {
        onPluginUnload()
        _context = null
        PluginContextHolder.clear()
    }

    protected open fun onPluginLoad(context: PluginContext) {}

    protected open fun onPluginUnload() {}
}
