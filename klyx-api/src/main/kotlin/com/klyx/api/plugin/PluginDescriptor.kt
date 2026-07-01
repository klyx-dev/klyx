package com.klyx.api.plugin

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Describes a plugin's metadata and requirements.
 *
 * @property id Unique identifier for the plugin.
 * @property name Display name of the plugin. Defaults to [id].
 * @property version Version of the plugin.
 * @property minAppVersion Minimum application version required by the plugin.
 * @property maxAppVersion Maximum application version supported by the plugin, if any.
 * @property entryClass Fully qualified name of the plugin's entry point class.
 * @property description A short description of the plugin's functionality.
 * @property icon Path or URL to the plugin's icon.
 * @property author Information about the plugin's author.
 * @property license License under which the plugin is distributed.
 * @property links Useful links related to the plugin.
 * @property permissions List of permissions required by the plugin.
 */
@Serializable
@Immutable
data class PluginDescriptor(
    val id: String,
    val name: String = id,
    val version: String,
    val minAppVersion: String,
    val maxAppVersion: String? = null,
    val entryClass: String,
    val description: String = "",
    val icon: String? = null,
    val author: PluginAuthor? = null,
    val license: String = "",
    val links: PluginLinks? = null,
    val permissions: List<String> = emptyList()
)

/**
 * Information about a plugin's author.
 *
 * @property name Name of the author.
 * @property email Email address of the author.
 * @property url Personal website or profile URL.
 * @property github GitHub username or profile URL.
 */
@Serializable
@Immutable
data class PluginAuthor(
    val name: String,
    val email: String? = null,
    val url: String? = null,
    val github: String? = null
)

/**
 * Useful links related to a plugin.
 *
 * @property source URL to the plugin's source code.
 * @property issues URL to the plugin's issue tracker.
 * @property website URL to the plugin's official website.
 * @property donate URL for donations.
 */
@Serializable
@Immutable
data class PluginLinks(
    val source: String? = null,
    val issues: String? = null,
    val website: String? = null,
    val donate: String? = null
)
