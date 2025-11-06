package com.klyx

import androidx.compose.runtime.Composable
import com.klyx.res.about
import com.klyx.res.dark_theme
import com.klyx.res.editor_settings
import com.klyx.res.general_settings
import com.klyx.res.look_and_feel
import com.klyx.res.settings
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

sealed interface AppRoute {

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Terminal : AppRoute

    @Serializable
    data object Settings : AppRoute {

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

val AppRoute.title
    @Composable
    get() = when (this) {
        AppRoute.Settings.About -> stringResource(strings.about)
        AppRoute.Settings.Appearance -> stringResource(strings.look_and_feel)
        AppRoute.Settings.DarkTheme -> stringResource(strings.dark_theme)
        AppRoute.Settings.EditorPreferences -> stringResource(strings.editor_settings)
        AppRoute.Settings.GeneralPreferences -> stringResource(strings.general_settings)
        AppRoute.Settings -> stringResource(strings.settings)
        else -> this::class.simpleName ?: this::class.toString()
    }
