package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerializationException

class SettingsJsonTest : FunSpec({

    fun storage(initial: Settings = Settings()) = object : DataStore<Settings> {
        override val data = MutableStateFlow(initial)

        override suspend fun updateData(transform: suspend (Settings) -> Settings): Settings =
            transform(data.value).also { data.value = it }
    }

    test("settings JSON uses the disk format and can be edited") {
        val store = SettingsStore(SettingsDataStore(storage()))
        val text = store.readJson()
        text.contains("\"auto_save\": false") shouldBe true
        store.writeJson(text.replace("\"auto_save\": false", "\"auto_save\": true"))
        store.read().autoSave shouldBe true
    }

    test("onboarding completion survives settings serialization") {
        val store = SettingsStore(SettingsDataStore(storage()))
        store.read().onboardingCompleted shouldBe false
        store.update { it.copy(onboardingCompleted = true) }
        val restored = SettingsStore(SettingsDataStore(storage()))
        restored.writeJson(store.readJson())
        restored.read().onboardingCompleted shouldBe true
    }

    test("invalid JSON leaves the previous settings intact") {
        val initial = Settings(autoSave = true)
        val store = SettingsStore(SettingsDataStore(storage(initial)))
        shouldThrow<SerializationException> { store.writeJson("{\"auto_save\":") }
        store.read() shouldBe initial
    }

    test("settings edits support comments and defaults for omitted fields") {
        val store = SettingsStore(SettingsDataStore(storage()))
        store.writeJson(
            """
            {
              // Enable saving from the editor.
              "auto_save": true
            }
        """.trimIndent()
        )
        store.read() shouldBe Settings(autoSave = true)
    }
})
