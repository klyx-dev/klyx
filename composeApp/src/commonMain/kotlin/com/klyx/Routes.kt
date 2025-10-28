package com.klyx

import kotlinx.serialization.Serializable

sealed interface AppRoute {

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Terminal : AppRoute

    @Serializable
    data object Settings : AppRoute {

        @Serializable
        data object SettingsPage : AppRoute

        @Serializable
        data object GeneralPreferences : AppRoute

        @Serializable
        data object Appearance : AppRoute

        @Serializable
        data object DarkTheme : AppRoute

        @Serializable
        data object EditorPreferences : AppRoute

        @Serializable
        data object About : AppRoute
    }
}
