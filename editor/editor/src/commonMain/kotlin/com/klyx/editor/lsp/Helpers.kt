package com.klyx.editor.lsp

import com.klyx.core.app.App
import com.klyx.core.language.languageIdentifiers
import com.klyx.core.lsp.LanguageServerName
import com.klyx.core.settings.LanguageSettings
import com.klyx.editor.language.LanguageName
import com.klyx.editor.language.LanguageRegistry
import com.klyx.extension.host.ExtensionStore

internal fun getLanguageIdForLanguage(languageName: LanguageName, cx: App): String? {
    val store = ExtensionStore.global(cx)
    return store.installedExtensions().firstNotNullOfOrNull { (_, entry) ->
        entry.manifest.languageServers.values.firstNotNullOfOrNull { serverManifestEntry ->
            if (serverManifestEntry.languageIds.isNotEmpty()) {
                serverManifestEntry.languageIds[languageName]
            } else if (
                languageName in serverManifestEntry.languages ||
                languageName.contentEquals(serverManifestEntry.language, ignoreCase = true)
            ) {
                languageIdentifiers[languageName.value] ?: languageName.value.lowercase()
            } else {
                null
            }
        }
    } ?: languageIdentifiers[languageName.value]
}

internal fun getLanguageServerNameForLanguage(languageName: LanguageName, cx: App): LanguageServerName? {
    LanguageSettings[languageName.value]?.let { languageSettings ->
        val languages = LanguageRegistry.INSTANCE
        val availableLspAdapters = languages.lspAdapters(languageName)
        val availableLanguageServers = availableLspAdapters.map { it.name }
        val desiredLanguageServers = languageSettings.customizedLanguageServers(availableLanguageServers)
        return desiredLanguageServers.firstOrNull() ?: return@let
    }

    val store = ExtensionStore.global(cx)
    return store.installedExtensions().values.firstNotNullOfOrNull { extension ->
        extension.manifest.languageServers.entries.firstOrNull { (_, serverConfig) ->
            languageName in serverConfig.languages ||
                    serverConfig.languageIds.keys.contains(languageName) ||
                    languageName.contentEquals(serverConfig.language, ignoreCase = true)
        }?.key
    }
}
