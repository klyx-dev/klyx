package com.klyx.ui.component.extension

import com.klyx.core.extension.ExtensionInfo
import kotlinx.serialization.Serializable

@Serializable
sealed interface ExtensionRoutes {
    @Serializable
    data object ExtensionList : ExtensionRoutes

    @Serializable
    data class ExtensionDetail(val extensionInfo: ExtensionInfo) : ExtensionRoutes
}
