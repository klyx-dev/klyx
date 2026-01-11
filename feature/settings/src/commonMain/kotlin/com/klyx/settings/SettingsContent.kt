package com.klyx.settings

import com.klyx.settings.content.ExtensionSettingsContent
import kotlinx.serialization.Serializable

@Serializable
data class SettingsContent(
    val extension: ExtensionSettingsContent = ExtensionSettingsContent()
)
