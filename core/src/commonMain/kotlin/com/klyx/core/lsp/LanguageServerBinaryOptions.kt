package com.klyx.core.lsp

/**
 * Configures the search (and installation) of language servers.
 *
 * @property allowPathLookup Whether the adapter should look at the users system
 * @property allowBinaryDownload  Whether the adapter should download its own version
 * @property preRelease Whether the adapter should download a pre-release version
 */
data class LanguageServerBinaryOptions(
    val allowPathLookup: Boolean = false,
    val allowBinaryDownload: Boolean = false,
    val preRelease: Boolean = false,
)
