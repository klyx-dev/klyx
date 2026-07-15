package com.klyx.api.lsp

import com.klyx.api.InternalKlyxApi
import com.klyx.api.data.file.KxFile
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService

/**
 * Registry for Language Servers.
 *
 * Plugins use this registry to contribute LSP support for specific file types.
 * Multiple providers can be registered for the same pattern — e.g. a type-checker
 * and a linter both serving Python — and all of them will be started and queried
 * together as independent, parallel language servers.
 *
 * ### Example
 * ```kotlin
 * val lspRegistry: LanguageServerRegistry by plugin()
 *
 * fun registerLsp() {
 *     lspRegistry.register("py", pyrightProvider)
 *     lspRegistry.register("py", ruffProvider)
 * }
 * ```
 */
interface LanguageServerRegistry : PluginService {

    /** A provider registered under a stable [id], used to key its dedicated server instance. */
    data class RegisteredProvider(val id: String, val provider: LanguageServerProvider)

    /**
     * Registers a [LanguageServerProvider] for the given file pattern.
     *
     * @param pattern The file extension (e.g., "kt", "java") or, for extensionless
     * files, the lowercased file name (e.g., "dockerfile", "makefile").
     * @param provider The provider that creates the Language Server instance.
     */
    context(plugin: KlyxPlugin)
    fun register(pattern: String, provider: LanguageServerProvider): LanguageServerRegistration

    /**
     * Internal method to register a [LanguageServerProvider] without a [KlyxPlugin] context.
     * Use this for built-in providers.
     */
    @InternalKlyxApi
    fun registerInternal(pattern: String, provider: LanguageServerProvider): LanguageServerRegistration

    /**
     * Unregisters the Language Server for the given ID.
     */
    fun unregister(id: String)

    /**
     * Returns every [RegisteredProvider] whose pattern matches [file]'s [KxFile.providerKey][com.klyx.api.data.file.providerKey],
     * ordered by registration order (earliest first). Empty if none match.
     *
     * Callers should treat the first entry as the "primary" server (used for navigation
     * features that only make sense against a single source of truth, like go-to-definition)
     * and query the full list for features that benefit from aggregation, like diagnostics
     * or inlay hints.
     */
    fun getProviders(file: KxFile): List<RegisteredProvider>
}
