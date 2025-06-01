package com.klyx.extension

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.encodeToString

fun createToml(toml: ExtensionToml) = Toml.encodeToString(toml)
