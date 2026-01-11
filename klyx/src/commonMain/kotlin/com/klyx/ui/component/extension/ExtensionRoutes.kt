package com.klyx.ui.component.extension

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.klyx.extension.ExtensionManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface ExtensionRoutes : NavKey {
    @Serializable
    data object ExtensionList : ExtensionRoutes, NavKey

    @Serializable
    data class ExtensionDetail(val manifest: ExtensionManifest) : ExtensionRoutes, NavKey

    companion object {
        fun config() = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(ExtensionList::class, ExtensionList.serializer())
                    subclass(ExtensionDetail::class, ExtensionDetail.serializer())
                }
            }
        }
    }
}
