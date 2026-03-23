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
    data object Test : Route, NavKey

    @Serializable
    data object Extension : Route, NavKey

    @Serializable
    data class EditOrViewExtension(val filePath: String, val edit: Boolean = true) : Route, NavKey

    @Serializable
    data object Terminal : Route, NavKey

    @Serializable
    data object TerminalSettings : Route, NavKey

    @Serializable
    data object Settings : Route, NavKey

    companion object {
        val TopLevelRoutes: Set<NavKey> = setOf(Main, Test, Terminal, Settings)

        fun config() = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Main::class, Main.serializer())
                    subclass(Test::class, Test.serializer())
                    subclass(Extension::class, Extension.serializer())
                    subclass(EditOrViewExtension::class, EditOrViewExtension.serializer())
                    subclass(Terminal::class, Terminal.serializer())
                    subclass(TerminalSettings::class, TerminalSettings.serializer())
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

}
