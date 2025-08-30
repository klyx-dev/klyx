package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val toml = Toml(
    inputConfig = TomlInputConfig(
        ignoreUnknownNames = true
    )
)

fun createToml(extensionInfo: ExtensionInfo) = toml.encodeToString(extensionInfo)

fun parseToml(source: RawSource): ExtensionInfo {
    val buffered = source.buffered().peek()
    return toml.decodeFromString(buffered.readString())
}
