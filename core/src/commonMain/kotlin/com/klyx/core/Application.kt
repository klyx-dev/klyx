package com.klyx.core

import com.klyx.core.extension.ExtensionHost
import com.klyx.core.io.Paths
import com.klyx.core.io.languagesDir
import com.klyx.core.language.LanguageRegistry
import com.klyx.core.languages.Languages
import com.klyx.core.noderuntime.NodeBinaryOptions
import com.klyx.core.noderuntime.NodeRuntime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias App = Application

object Application {
    suspend fun init() = withContext(Dispatchers.Default) {
        val shellEnvLoaded = CompletableDeferred<Unit>()
        // TODO: expose node settings
        val options = MutableStateFlow(NodeBinaryOptions())

        backgroundScope.launch {
            shellEnvLoaded.complete(Unit)
        }

        val nodeRuntime = NodeRuntime(httpClient, shellEnvLoaded, options.asStateFlow())
        val languageRegistry = LanguageRegistry.INSTANCE
        languageRegistry.setLanguageServerDownloadDir(Paths.languagesDir)
        Languages.init(languageRegistry, nodeRuntime)
        ExtensionHost.init(nodeRuntime)
    }
}

