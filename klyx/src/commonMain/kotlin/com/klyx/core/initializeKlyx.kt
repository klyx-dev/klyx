package com.klyx.core

import com.klyx.core.app.Application
import com.klyx.core.app.Initialization
import com.klyx.core.app.trace
import com.klyx.core.noderuntime.NodeBinaryOptions
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.language.LanguageRegistry
import com.klyx.editor.languages.initLanguages
import com.klyx.extension.ExtensionHostProxy
import com.klyx.extension.extension
import com.klyx.extension.host.initExtensionHost
import com.klyx.language.extension.initLanguageExtensions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val initMutex = Mutex()
private var isInitialized = false

private val STEPS = arrayOf(
    "Loading settings",
    "Starting extension system",
    "Building extension index",
    "Preparing Node runtime",
    "Registering languages",
    "Wiring language extensions",
    "Starting extension host",
)

/**
 * Initializes the application runtime environment including language server configuration,
 * extension host setup, and environment variables. This method ensures the initialization
 * logic is executed only once during the application lifecycle.
 *
 * Note: This method is thread-safe and should not cause side effects if called redundantly.
 */
suspend fun initializeKlyx(application: Application) {
    trace("Initialization")

    initMutex.withLock {
        if (isInitialized) return
        Initialization.defineSteps(*STEPS)

        val cx = application.app
        step(0) { SettingsManager.init(cx) }

        withContext(Dispatchers.Default) {
            step(1) { extension.init(cx) }

            val extensionHostProxy = ExtensionHostProxy.global(cx)
            val options = MutableStateFlow(NodeBinaryOptions())

            step(2) {
                NodeRuntime(httpClient, CompletableDeferred(Unit), options.asStateFlow())
            }?.let { nodeRuntime ->
                val languageRegistry = LanguageRegistry.INSTANCE
                step(3) { initLanguages(languageRegistry, nodeRuntime, application.app) }
                step(4) { initLanguageExtensions(extensionHostProxy, languageRegistry) }
                step(5) { initExtensionHost(extensionHostProxy, nodeRuntime, cx) }
            }
        }

        isInitialized = true
        Initialization.complete()
    }
}

private inline fun <R> step(index: Int, block: () -> R): R? {
    Initialization.beginStep(index)
    return runCatching(block).fold(
        onSuccess = { Initialization.completeStep(index); it },
        onFailure = { Initialization.failStep(index, it); null },
    )
}
