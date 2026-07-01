package com.klyx.api.plugin

import androidx.annotation.RestrictTo
import com.klyx.core.App
import com.klyx.core.Global
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface PluginService : Global

interface PluginRuntimeService

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PluginRuntimeRegistry : Global {
    fun <T : PluginRuntimeService> service(
        plugin: KlyxPlugin,
        type: KClass<T>
    ): T
}

inline fun <reified T : PluginService> App.pluginService(type: KClass<T>): T = global(type)

inline fun <reified T : PluginService> plugin() = PluginServiceDelegate(T::class)
inline fun <reified T : PluginRuntimeService> runtime() = PluginRuntimeDelegate(T::class)

class PluginRuntimeDelegate<T : PluginRuntimeService>(
    private val clazz: KClass<T>
) : ReadOnlyProperty<KlyxPlugin, T> {

    @OptIn(UnsafeGlobalAccess::class)
    override fun getValue(thisRef: KlyxPlugin, property: KProperty<*>): T {
        val registry = GlobalApp.global<PluginRuntimeRegistry>()
        return registry.service(thisRef, clazz)
    }
}

class PluginServiceDelegate<T : PluginService>(
    private val clazz: KClass<T>
) : ReadOnlyProperty<Any?, T> {

    @OptIn(UnsafeGlobalAccess::class)
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return GlobalApp.global(clazz)
    }
}
