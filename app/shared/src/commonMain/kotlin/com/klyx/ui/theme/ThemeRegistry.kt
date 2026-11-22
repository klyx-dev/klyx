package com.klyx.ui.theme

import com.klyx.app.resources.Resources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object ThemeRegistry {
    private var themeNames: List<String> = emptyList()
    private val cache = mutableMapOf<String, KxTheme>()

    val availableThemeNames: List<String> get() = themeNames

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            if (themeNames.isNotEmpty()) return@withContext
            themeNames = Resources.readBytes("files/themes/index.txt")
                .decodeToString()
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()
        }
    }

    suspend fun getTheme(name: String): KxTheme? = withContext(Dispatchers.IO) {
        if (name !in themeNames) return@withContext null
        cache[name]?.let { return@withContext it }
        val content = try {
            Resources.readBytes("files/themes/$name").decodeToString()
        } catch (_: Exception) {
            ensureActive()
            return@withContext null
        }
        val theme = KxTheme.parse(name, content)
        cache[name] = theme
        theme
    }
}
