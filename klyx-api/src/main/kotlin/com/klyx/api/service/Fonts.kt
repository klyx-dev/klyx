package com.klyx.api.service

import androidx.compose.ui.text.font.FontFamily
import com.klyx.api.plugin.PluginService

/**
 * Service for managing and retrieving font families within the Klyx application.
 *
 * This service handles font loading, often from URIs, and manages an internal cache
 * to ensure efficient resource usage across different parts of the UI (like the editor).
 */
interface Fonts : PluginService {

    /**
     * Resolves and returns a [FontFamily] for the given [uri].
     *
     * If [uri] is null or cannot be resolved, a default font family is returned.
     * The result is typically cached for future requests.
     */
    suspend fun getFontFamily(uri: String?): FontFamily

    /**
     * Clears all cached font resources.
     *
     * Call this when font settings change or to free up memory.
     */
    fun clearCache()
}
