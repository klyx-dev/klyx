package com.klyx.api.plugin

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

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

@Serializable
@Immutable
data class PluginAuthor(
    val name: String,
    val email: String? = null,
    val url: String? = null,
    val github: String? = null
)

@Serializable
@Immutable
data class PluginLinks(
    val source: String? = null,
    val issues: String? = null,
    val website: String? = null,
    val donate: String? = null
)
