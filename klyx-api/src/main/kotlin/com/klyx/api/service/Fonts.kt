package com.klyx.api.service

import androidx.compose.ui.text.font.FontFamily
import com.klyx.api.plugin.PluginService

interface Fonts : PluginService {
    suspend fun getFontFamily(uri: String?): FontFamily
    fun clearCache()
}
