package com.klyx.core

import com.klyx.core.app.Application
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
        val cx = application.app
        SettingsManager.init(cx)

        withContext(Dispatchers.Default) {
            extension.init(cx)
            val extensionHostProxy = ExtensionHostProxy.global(cx)
            // todo: expose node settings
            val options = MutableStateFlow(NodeBinaryOptions())
            val nodeRuntime = NodeRuntime(httpClient, CompletableDeferred(Unit), options.asStateFlow())
            val languageRegistry = LanguageRegistry.INSTANCE
            initLanguages(languageRegistry, nodeRuntime, application.app)
            initLanguageExtensions(extensionHostProxy, languageRegistry)
            initExtensionHost(extensionHostProxy, nodeRuntime, cx)
        }

        isInitialized = true
    }
}
