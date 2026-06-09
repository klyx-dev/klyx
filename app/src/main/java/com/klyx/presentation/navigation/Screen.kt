package com.klyx.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface Screen : NavKey {

    @Serializable
    data object Home : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Terminal : Screen

    companion object {
        fun config() = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(Screen::class) {
                    subclass(Home::class)
                    subclass(Settings::class)
                    subclass(Terminal::class)
                    subclass(SettingsScreen.Editor::class)
                    subclass(SettingsScreen.Appearance::class)
                    subclass(SettingsScreen.Terminal::class)
                    subclass(SettingsScreen.DeveloperOptions::class)
                    subclass(SettingsScreen.SystemDiagnostics::class)
                    subclass(SettingsScreen.About::class)
                }
            }
        }
    }
}

sealed interface SettingsScreen : Screen {

    @Serializable
    data object Editor : SettingsScreen

    @Serializable
    data object Appearance : SettingsScreen

    @Serializable
    data object Terminal : SettingsScreen

    @Serializable
    data object DeveloperOptions : SettingsScreen

    @Serializable
    data object SystemDiagnostics : SettingsScreen

    @Serializable
    data object About : SettingsScreen
}
