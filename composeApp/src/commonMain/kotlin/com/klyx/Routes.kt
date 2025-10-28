package com.klyx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@Polymorphic
sealed interface AppRoute {

    @Serializable
    data object Home : AppRoute

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
