package com.klyx.api.plugin

import com.klyx.core.App

interface PluginContext : PluginRuntimeService {

    val app: App
}

inline fun <reified T : PluginService> PluginContext.service(): T = app.pluginService(T::class)
