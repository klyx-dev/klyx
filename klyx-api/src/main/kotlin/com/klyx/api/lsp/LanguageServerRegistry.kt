package com.klyx.api.lsp

import com.klyx.api.InternalKlyxApi
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService

/**
 * Registry for Language Servers.
 *
 * Plugins use this registry to contribute LSP support for specific file extensions.
 *
 * ### Example
 * ```kotlin
 * val lspRegistry: LanguageServerRegistry by plugin()
 *
 * fun registerLsp() {
 *     lspRegistry.register("kt", myLspProvider)
 * }
 * ```
 */
interface LanguageServerRegistry : PluginService {

    /**
     * Registers a [LanguageServerProvider] for the given file extension.
     *
     * @param extension The file extension (e.g., "kt", "java").
     * @param provider The provider that creates the Language Server instance.
     */
    context(plugin: KlyxPlugin)
    fun register(extension: String, provider: LanguageServerProvider): LanguageServerRegistration

    /**
     * Internal method to register a [LanguageServerProvider] without a [KlyxPlugin] context.
     * Use this for built-in providers.
     */
    @InternalKlyxApi
    fun registerInternal(extension: String, provider: LanguageServerProvider): LanguageServerRegistration

    /**
     * Unregisters the Language Server for the given ID.
     */
    fun unregister(id: String)

    /**
     * Returns the [LanguageServerProvider] for the given file extension, or null if none is registered.
     */
    fun getProvider(extension: String): LanguageServerProvider?
}
