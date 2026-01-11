package com.klyx.settings

import com.klyx.core.app.App
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okio.Path

typealias TypeId = io.ktor.util.reflect.TypeInfo

@OptIn(ExperimentalSerializationApi::class)
internal val json = Json {
    explicitNulls = false
    encodeDefaults = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
    isLenient = true
}

fun initSettings(cx: App) {

}

data class SettingsLocation(
    val worktreeId: WorktreeId,
    val path: Path
)

interface Settings
