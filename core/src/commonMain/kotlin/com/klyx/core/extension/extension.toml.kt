package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

fun createToml(toml: ExtensionToml) = Toml.encodeToString(toml)

fun parseToml(source: RawSource): ExtensionToml {
    val buffered = source.buffered().peek()
    return Toml.decodeFromString(buffered.readString())
}
