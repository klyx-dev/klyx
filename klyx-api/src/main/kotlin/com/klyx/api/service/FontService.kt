package com.klyx.api.service

import androidx.compose.ui.text.font.FontFamily
import com.klyx.core.Global

interface FontService : Global {
    suspend fun getFontFamily(uri: String?): FontFamily
    fun clearCache()
}
