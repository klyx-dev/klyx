package com.klyx.editor.language

import io.kotest.core.spec.style.FunSpec
import net.peanuuutz.tomlkt.Toml

class LanguageConfigTest : FunSpec({
    test("test serialize") {
        val config = LanguageConfig(LanguageName(""), "", "", LanguageMatcher())
        val toml = Toml {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
        val serialized = toml.encodeToString(LanguageConfig.serializer(), config)
        println(serialized)
    }
})
