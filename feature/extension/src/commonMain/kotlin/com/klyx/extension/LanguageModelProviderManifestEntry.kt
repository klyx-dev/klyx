package com.klyx.extension

import kotlinx.serialization.Serializable

/**
 * Manifest entry for a language model provider.
 *
 * @property name Display name for the provider.
 * @property icon Path to an SVG icon file relative to the extension root (e.g., "icons/provider.svg").
 */
@Serializable
data class LanguageModelProviderManifestEntry(
    val name: String,
    val icon: String? = null
)
