package com.klyx.settings

import com.klyx.runtime.settings.Settings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class TermuxPermissionSettingsTest : FunSpec({
    test("existing settings allow the explanatory consent prompt") {
        Json.decodeFromString<Settings>("{}").hideTermuxPermissionPrompt shouldBe false
    }

    test("don't ask again survives saving and reloading settings") {
        val settings = Settings(hideTermuxPermissionPrompt = true)
        val saved = Json.encodeToString(Settings.serializer(), settings)
        Json.decodeFromString<Settings>(saved).hideTermuxPermissionPrompt shouldBe true
    }
})
