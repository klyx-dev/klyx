package com.klyx.extension

import com.klyx.extension.native.WasmHost

sealed interface Event {
    data object ExtensionsUpdated : Event
    data object StartedReloading : Event

    @JvmInline
    value class ExtensionInstalled(val extensionId: String) : Event

    @JvmInline
    value class ExtensionUninstalled(val extensionId: String) : Event

    @JvmInline
    value class ExtensionFailedToLoad(val extensionId: String) : Event
}
