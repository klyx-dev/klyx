package com.klyx.core.settings

import arrow.core.left
import arrow.core.right
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.partitionMap
import kotlinx.serialization.Serializable

/**
 * The settings for a particular language.
 *
 * @property tabSize How many columns a tab should occupy.
 */
@Serializable
data class LanguageSettings(
    val tabSize: UInt = 4u,
    /**
     * The list of language servers to use (or disable) for this language.
     *     
     * This array should consist of language server IDs, as well as the following special tokens:
     * - `"!<language_server_id>"` - A language server ID prefixed with a `!` will be disabled.
     * - `"..."` - A placeholder to refer to the **rest** of the registered language servers for this language.
     */
    val languageServers: List<String> = emptyList()
) {

    fun customizedLanguageServers(availableLanguageServers: List<LanguageServerName>): List<LanguageServerName> {
        return resolveLanguageServers(languageServers, availableLanguageServers)
    }

    companion object {
        /**
         * A token representing the rest of the available language servers.
         */
        const val REST_OF_LANGUAGE_SERVERS = "..."

        internal fun resolveLanguageServers(
            configuredLanguageServers: List<String>,
            availableLanguageServers: List<LanguageServerName>
        ): List<LanguageServerName> {
            val (disabledLanguageServers, enabledLanguageServers) = configuredLanguageServers
                .partitionMap { languageServer ->
                    when {
                        languageServer.startsWith('!') -> languageServer.removePrefix("!").left()
                        else -> languageServer.right()
                    }
                }

            val rest = availableLanguageServers
                .filter { it !in disabledLanguageServers && it !in enabledLanguageServers }

            return enabledLanguageServers.flatMap { languageServer ->
                if (languageServer == REST_OF_LANGUAGE_SERVERS) rest else listOf(languageServer)
            }
        }

        operator fun get(languageName: String) = SettingsManager.settings.value.languages[languageName]
            ?: SettingsManager.defaultSettings.languages[languageName]

    }
}
