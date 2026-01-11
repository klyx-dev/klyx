package com.klyx.extension

import com.klyx.core.app.App

@Suppress("ClassName")
object extension {
    fun init(cx: App) {
        val _ = ExtensionHostProxy.defaultGlobal(cx)
    }
}
