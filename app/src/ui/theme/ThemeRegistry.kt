package com.klyx.ui.theme

import klyx.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object ThemeRegistry {
    private var themeNames: List<String> = emptyList()
    private val cache = mutableMapOf<String, KxTheme>()

    suspend fun init() = withContext(Dispatchers.IO) {
        if (themeNames.isNotEmpty()) return@withContext
        themeNames = Res.readBytes("files/themes/index.txt")
            .decodeToString()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
    }

    val availableThemeNames: List<String> get() = themeNames

    suspend fun getTheme(name: String): KxTheme? = withContext(Dispatchers.IO) {
        if (name !in themeNames) return@withContext null
        cache[name]?.let { return@withContext it }
        val content = runCatching {
            Res.readBytes("files/themes/$name").decodeToString()
        }.getOrNull() ?: return@withContext null
        val theme = KxTheme.parse(name, content)
        cache[name] = theme
        theme
    }
}
