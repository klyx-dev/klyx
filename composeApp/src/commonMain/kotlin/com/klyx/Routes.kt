package com.klyx

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    /**
     * Main screen route
     */
    @Serializable
    data object Main : Route, NavKey

    @Serializable
    data object Terminal : Route, NavKey

    @Serializable
    data object Settings : Route, NavKey

    companion object {
        val TopLevelRoutes = setOf(Main, Terminal, Settings)

        fun config() = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Main::class, Main.serializer())
                    subclass(Terminal::class, Terminal.serializer())
                    subclass(Settings::class, Settings.serializer())

                    subclass(SettingsRoute.General::class, SettingsRoute.General.serializer())
                    subclass(SettingsRoute.Appearance::class, SettingsRoute.Appearance.serializer())
                    subclass(SettingsRoute.DarkTheme::class, SettingsRoute.DarkTheme.serializer())
                    subclass(SettingsRoute.Editor::class, SettingsRoute.Editor.serializer())
                    subclass(SettingsRoute.About::class, SettingsRoute.About.serializer())
                }
            }
        }
    }
}

@Serializable
sealed interface SettingsRoute : NavKey {

    @Serializable
    data object General : NavKey, SettingsRoute

    @Serializable
    data object Appearance : NavKey, SettingsRoute

    @Serializable
    data object DarkTheme : NavKey, SettingsRoute

    @Serializable
    data object Editor : NavKey, SettingsRoute

    @Serializable
    data object About : NavKey, SettingsRoute

    companion object {
        fun config() = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(General::class, General.serializer())
                    subclass(Appearance::class, Appearance.serializer())
                    subclass(DarkTheme::class, DarkTheme.serializer())
                    subclass(Editor::class, Editor.serializer())
                    subclass(About::class, About.serializer())
                }
            }
        }
    }
}
