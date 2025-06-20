package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.source.decodeFromStream
import kotlinx.serialization.encodeToString
import java.io.InputStream

fun createToml(toml: ExtensionToml) = Toml.encodeToString(toml)

fun parseToml(inputStream: InputStream) = Toml.decodeFromStream<ExtensionToml>(inputStream)
