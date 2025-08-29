package com.klyx.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object ThemeManager {
    val availableThemes = mutableStateListOf<Theme>()

    init {
        availableThemes.addAll(
            listOf(
                AutumnEmber.asTheme(),
                EmeraldWaves.asTheme(),
                OceanBreeze.asTheme(),
                GoldenGlow.asTheme()
            )
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    var showThemeSelector by mutableStateOf(false)
        private set

    fun showThemeSelector() {
        showThemeSelector = true
    }

    fun hideThemeSelector() {
        showThemeSelector = false
    }

    fun toggleThemeSelector() {
        showThemeSelector = !showThemeSelector
    }

    /**
     * Load and parse a theme family from extension
     */
    suspend fun loadThemeFamily(themeSource: RawSource?) = withContext(Dispatchers.IO) {
        val source = themeSource?.buffered()?.peek() ?: return@withContext Err("Theme source is null")

        val themeJson: ThemeJson = try {
            json.decodeFromString(source.readString())
        } catch (e: IllegalArgumentException) {
            return@withContext Err("Invalid theme JSON: ${e.message}")
        } catch (e: SerializationException) {
            return@withContext Err("Error parsing theme JSON: ${e.message}")
        }

        availableThemes += themeJson.asTheme()

        Ok(Unit)
    }

    /**
     * Get theme by name
     */
    fun getTheme(name: String?) = when (val trimmedName = name?.trim()?.replace(" ", "")?.lowercase()) {
        "autumnember" -> AutumnEmber.asTheme()
        "emeraldwaves" -> EmeraldWaves.asTheme()
        "oceanbreeze" -> OceanBreeze.asTheme()
        "goldenglow" -> GoldenGlow.asTheme()
        else -> availableThemes.find { it.name.trim().replace(" ", "").lowercase() == trimmedName }
    }
}
