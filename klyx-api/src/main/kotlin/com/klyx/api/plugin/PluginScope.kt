package com.klyx.api.plugin

import kotlinx.coroutines.CoroutineScope

interface PluginScope : CoroutineScope, PluginRuntimeService
