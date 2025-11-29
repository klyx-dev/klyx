package com.klyx.core

import com.klyx.core.extension.ExtensionHost
import com.klyx.core.noderuntime.NodeBinaryOptions
import com.klyx.core.noderuntime.NodeRuntime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

typealias App = Application

object Application {
    suspend fun init() {
        val shellEnvLoaded = CompletableDeferred<Unit>()
        // TODO: expose node settings
        val options = MutableStateFlow(NodeBinaryOptions())

        backgroundScope.launch {
            shellEnvLoaded.complete(Unit)
        }

        val nodeRuntime = NodeRuntime(httpClient, shellEnvLoaded, options.asStateFlow())
        ExtensionHost.init(nodeRuntime)
    }
}

