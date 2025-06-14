package com.klyx.core.extension

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionEntry(
    val submodule: String,
    val version: String
)

typealias ExtensionsIndex = Map<String, ExtensionEntry>
